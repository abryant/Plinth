; ModuleID = 'vft.ll'

%opaque = type opaque
%VFT = type [0 x %opaque*]

%RawString = type {%opaque*, %opaque*, i32, i8(%RawString*, i32)*, void (%RawString*, i32, i8)*, [0 x i8]}
%String    = type {%opaque*, %opaque*, i32, i8(%String*,    i32)*, void (%String*,    i32, i8)*}

%RTTI = type {i8, i32}
%TypeArgumentMapper = type {i32, [0 x %RTTI*]}

%ExcludeList = type [0 x i1]
%Descriptor = type { i32, [0 x {%RawString*, %RTTI*}] }
%FunctionSearchList = type {i32, [0 x {%RTTI*, %Descriptor*, %VFT*, %ExcludeList*}]}

@OutOfMemoryErrorMessage = private unnamed_addr constant
                           {%opaque*, %opaque*, i32, i8(%RawString*, i32)*, void (%RawString*, i32, i8)*, [73 x i8]}
                           {
                             %opaque* null,
                             %opaque* null,
                             i32 73,
                             i8(%RawString*, i32)* @OutOfMemoryError_string_array_getter,
                             void (%RawString*, i32, i8)* @OutOfMemoryError_string_array_setter,
                             [73 x i8] c"Failed to allocate memory to set up virtual function tables! Aborting...\0A"
                           }
define private protected i8 @OutOfMemoryError_string_array_getter(%RawString* %array, i32 %index) {
entry:
  %pointer = getelementptr %RawString* %array, i32 0, i32 5, i32 %index
  %value = load i8* %pointer
  ret i8 %value
}
define private protected void @OutOfMemoryError_string_array_setter(%RawString* %array, i32 %index, i8 %value) {
entry:
  %pointer = getelementptr %RawString* %array, i32 0, i32 5, i32 %index
  store i8 %value, i8* %pointer
  ret void
}

declare i8* @calloc(i32, i32)
declare void @free(i8*)
declare i32 @strncmp(i8* %str1, i8* %str2, i32 %len)
declare void @abort() noreturn

declare protected i1 @plinth_check_type_matches(%RTTI* %queryType, %RTTI* %specType, %TypeArgumentMapper* %queryMapper, %TypeArgumentMapper* %specMapper, i1 %ignoreTypeModifiers, i1 %looselyMatchWildcards)

declare void @plinth_stderr_write(%String* %array)

define protected %VFT* @plinth_core_generate_supertype_vft(%FunctionSearchList* %searchDescriptors, i32 %index) {
entry:
  %thisRTTIPtr = getelementptr %FunctionSearchList* %searchDescriptors, i32 0, i32 1, i32 %index, i32 0
  %thisRTTI = load %RTTI** %thisRTTIPtr
  %thisDescriptorPtr = getelementptr %FunctionSearchList* %searchDescriptors, i32 0, i32 1, i32 %index, i32 1
  %thisDescriptor = load %Descriptor** %thisDescriptorPtr
  %thisVFTPtr = getelementptr %FunctionSearchList* %searchDescriptors, i32 0, i32 1, i32 %index, i32 2
  %thisVFT = load %VFT** %thisVFTPtr
  %vftLengthPtr = getelementptr %Descriptor* %thisDescriptor, i32 0, i32 0
  %vftLength = load i32* %vftLengthPtr
  %vftAlloc = call i8* @calloc(i32 ptrtoint (%opaque** getelementptr (%opaque** null, i32 1) to i32), i32 %vftLength)
  %outOfMemory = icmp eq i8* %vftAlloc, null
  br i1 %outOfMemory, label %error, label %startLoop

error:
  %errorMessage = bitcast { %opaque*, %opaque*, i32, i8(%RawString*, i32)*, void (%RawString*, i32, i8)*, [73 x i8] }* @OutOfMemoryErrorMessage to %String*
  call void @plinth_stderr_write(%String* %errorMessage)
  call void @abort() noreturn
  unreachable

startLoop:
  %vft = bitcast i8* %vftAlloc to %VFT*
  %check = icmp ult i32 0, %vftLength
  br i1 %check, label %loop, label %exit

loop:
  %i = phi i32 [0, %startLoop], [%nexti, %loop]
  %disambiguatorPtr = getelementptr %Descriptor* %thisDescriptor, i32 0, i32 1, i32 %i, i32 0
  %disambiguator = load %RawString** %disambiguatorPtr
  %defaultPtr = getelementptr %VFT* %thisVFT, i32 0, i32 %i
  %default = load %opaque** %defaultPtr
  %func = call %opaque* @plinth_find_vft_function(%RTTI* %thisRTTI, %RawString* %disambiguator, %opaque* %default, %FunctionSearchList* %searchDescriptors)
  %element = getelementptr %VFT* %vft, i32 0, i32 %i
  store %opaque* %func, %opaque** %element
  %nexti = add i32 %i, 1
  %b = icmp ult i32 %nexti, %vftLength
  br i1 %b, label %loop, label %exit

exit:
  ret %VFT* %vft
}

define private hidden %opaque* @plinth_find_vft_function(%RTTI* %thisRTTI, %RawString* %disambiguator, %opaque* %default, %FunctionSearchList* %searchDescriptors) {
entry:
  %disambiguatorLengthPtr = getelementptr %RawString* %disambiguator, i32 0, i32 2
  %disambiguatorLength = load i32* %disambiguatorLengthPtr
  %numSearchPtr = getelementptr %FunctionSearchList* %searchDescriptors, i32 0, i32 0
  %numSearch = load i32* %numSearchPtr
  %endOfNullExcludeList = getelementptr %ExcludeList* null, i32 1, i32 %numSearch
  %excludeListSize = ptrtoint i1* %endOfNullExcludeList to i32
  %excludeListAlloc = call i8* @calloc(i32 %excludeListSize, i32 1)
  %outOfMemory = icmp eq i8* %excludeListAlloc, null
  br i1 %outOfMemory, label %error, label %beforeLoop

error:
  %errorMessage = bitcast { %opaque*, %opaque*, i32, i8(%RawString*, i32)*, void (%RawString*, i32, i8)*, [73 x i8] }* @OutOfMemoryErrorMessage to %String*
  call void @plinth_stderr_write(%String* %errorMessage)
  call void @abort() noreturn
  unreachable

beforeLoop:
  %excludeList = bitcast i8* %excludeListAlloc to %ExcludeList*
  %continueOuterLoop = icmp ult i32 0, %numSearch
  br i1 %continueOuterLoop, label %searchLoop, label %exit

searchLoop:
  %i = phi i32 [0, %beforeLoop], [%nexti, %endSearchLoop]
  %descriptorPtr = getelementptr %FunctionSearchList* %searchDescriptors, i32 0, i32 1, i32 %i, i32 1
  %descriptor = load %Descriptor** %descriptorPtr
  %vftPtr = getelementptr %FunctionSearchList* %searchDescriptors, i32 0, i32 1, i32 %i, i32 2
  %vft = load %VFT** %vftPtr
  %numFunctionsPtr = getelementptr %Descriptor* %descriptor, i32 0, i32 0
  %numFunctions = load i32* %numFunctionsPtr
  %containsFunctions = icmp ult i32 0, %numFunctions
  %excludedPtr = getelementptr %ExcludeList* %excludeList, i32 0, i32 %i
  %excluded = load i1* %excludedPtr
  %notExcluded = xor i1 %excluded, 1
  %runInnerLoop = and i1 %containsFunctions, %notExcluded
  br i1 %runInnerLoop, label %functionLoop, label %endSearchLoop

functionLoop:
  %j = phi i32 [0, %searchLoop], [%nextj, %endFunctionLoop]
  %currentDisambiguatorPtr = getelementptr %Descriptor* %descriptor, i32 0, i32 1, i32 %j, i32 0
  %currentDisambiguator = load %RawString** %currentDisambiguatorPtr
  %currentDisambiguatorLengthPtr = getelementptr %RawString* %currentDisambiguator, i32 0, i32 2
  %currentDisambiguatorLength = load i32* %currentDisambiguatorLengthPtr
  %check = icmp eq i32 %disambiguatorLength, %currentDisambiguatorLength
  br i1 %check, label %checkDisambiguator, label %endFunctionLoop

checkDisambiguator:
  %disambiguatorStr = getelementptr %RawString* %disambiguator, i32 0, i32 5, i32 0
  %currentDisambiguatorStr = getelementptr %RawString* %currentDisambiguator, i32 0, i32 5, i32 0
  %comparison = call i32 @strncmp(i8* %disambiguatorStr, i8* %currentDisambiguatorStr, i32 %disambiguatorLength)
  %match = icmp eq i32 %comparison, 0
  br i1 %match, label %checkOverrideRTTI, label %endFunctionLoop

checkOverrideRTTI:
  %overrideRTTIPtr = getelementptr %Descriptor* %descriptor, i32 0, i32 1, i32 %j, i32 1
  %overrideRTTI = load %RTTI** %overrideRTTIPtr
  %overrideRTTIIsNull = icmp eq %RTTI* %overrideRTTI, null
  br i1 %overrideRTTIIsNull, label %foundFunction, label %checkNotNullOverrideRTTI

checkNotNullOverrideRTTI:
  %overrideMapperPtr = getelementptr %FunctionSearchList* %searchDescriptors, i32 0, i32 1, i32 %i, i32 0
  %overrideMapper = load %RTTI** %overrideMapperPtr
  ; Note: the following bitcast and GEP will result in the wrong type if it is actually an object's RTTI, but in that case
  ; %overrideRTTI won't have any type parameters, so the %overrideMapper won't be accessed anyway, and we'll be fine
  %castedOverrideMapper = bitcast %RTTI* %overrideMapper to {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}*
  %overrideTypeArgumentMapper = getelementptr {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}* %castedOverrideMapper, i32 0, i32 5
  %overrideRTTIMatches = call i1 @plinth_check_type_matches(%RTTI* %overrideRTTI, %RTTI* %thisRTTI, %TypeArgumentMapper* %overrideTypeArgumentMapper, %TypeArgumentMapper* null, i1 true, i1 false)
  br i1 %overrideRTTIMatches, label %foundFunction, label %endFunctionLoop

foundFunction:
  %currentFunctionPtr = getelementptr %VFT* %vft, i32 0, i32 %j
  %currentFunction = load %opaque** %currentFunctionPtr
  %isAbstract = icmp eq %opaque* %currentFunction, null
  br i1 %isAbstract, label %updateExcludeList, label %returnFunction

returnFunction:
  call void @free(i8* %excludeListAlloc)
  ret %opaque* %currentFunction

updateExcludeList:
  %combiningExcludeListPtr = getelementptr %FunctionSearchList* %searchDescriptors, i32 0, i32 1, i32 %i, i32 3
  %combiningExcludeList = load %ExcludeList** %combiningExcludeListPtr
  %startExcludeListLoop = icmp ult i32 0, %numSearch
  br i1 %startExcludeListLoop, label %excludeListUpdateLoop, label %endSearchLoop

excludeListUpdateLoop:
  %excludeListIndex = phi i32 [0, %updateExcludeList], [%nextExcludeListIndex, %excludeListUpdateLoop]
  %excludeListElementPtr = getelementptr %ExcludeList* %excludeList, i32 0, i32 %excludeListIndex
  %excludeListElement = load i1* %excludeListElementPtr
  %combiningExcludeListElementPtr = getelementptr %ExcludeList* %combiningExcludeList, i32 0, i32 %excludeListIndex
  %combiningExcludeListElement = load i1* %combiningExcludeListElementPtr
  %combined = or i1 %excludeListElement, %combiningExcludeListElement
  store i1 %combined, i1* %excludeListElementPtr
  %nextExcludeListIndex = add i32 %excludeListIndex, 1
  %continueExcludeListLoop = icmp ult i32 %nextExcludeListIndex, %numSearch
  br i1 %continueExcludeListLoop, label %excludeListUpdateLoop, label %endSearchLoop

endFunctionLoop:
  %nextj = add i32 %j, 1
  %continue = icmp ult i32 %nextj, %numFunctions
  br i1 %continue, label %functionLoop, label %endSearchLoop

endSearchLoop:
  %nexti = add i32 %i, 1
  %b = icmp ult i32 %nexti, %numSearch
  br i1 %b, label %searchLoop, label %exit

exit:
  call void @free(i8* %excludeListAlloc)
  ret %opaque* %default
}
