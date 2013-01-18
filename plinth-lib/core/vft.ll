; ModuleID = 'vft.ll'

%opaque = type opaque
%VFT = type [0 x %opaque*]

%RawString = type { %opaque*, %opaque*, i32, [0 x i8]}
%InterfaceSearchList = type {i32, [0 x {%RawString*, %VFT*}]}

%Descriptor = type { i32, [0 x %RawString*] }
%FunctionSearchList = type {i32, [0 x {%Descriptor*, %VFT*}]}

declare i8* @calloc(i32, i32)
declare i32 @strncmp(i8* %str1, i8* %str2, i32 %len)

define protected %VFT* @plinth_core_find_interface_vft(%InterfaceSearchList* %interfaceVFTList, %RawString* %searchName) {
entry:
  %searchNameLengthPtr = getelementptr %RawString* %searchName, i32 0, i32 2
  %searchNameLength = load i32* %searchNameLengthPtr
  %searchNameBytes = getelementptr %RawString* %searchName, i32 0, i32 3, i32 0
  %numSearchPtr = getelementptr %InterfaceSearchList* %interfaceVFTList, i32 0, i32 0
  %numSearch = load i32* %numSearchPtr
  %continueLoop = icmp ult i32 0, %numSearch
  br i1 %continueLoop, label %searchLoop, label %exit

searchLoop:
  %i = phi i32 [0, %entry], [%nexti, %endSearchLoop]
  %namePtr = getelementptr %InterfaceSearchList* %interfaceVFTList, i32 0, i32 1, i32 %i, i32 0
  %name = load %RawString** %namePtr
  %nameLengthPtr = getelementptr %RawString* %name, i32 0, i32 2
  %nameLength = load i32* %nameLengthPtr
  %check = icmp eq i32 %searchNameLength, %nameLength
  br i1 %check, label %compareNames, label %endSearchLoop

compareNames:
  %nameBytes = getelementptr %RawString* %name, i32 0, i32 3, i32 0
  %comparison = call i32 @strncmp(i8* %nameBytes, i8* %searchNameBytes, i32 %searchNameLength)
  %match = icmp eq i32 %comparison, 0
  br i1 %match, label %returnVFT, label %endSearchLoop

returnVFT:
  %vftPtr = getelementptr %InterfaceSearchList* %interfaceVFTList, i32 0, i32 1, i32 %i, i32 1
  %vft = load %VFT** %vftPtr
  ret %VFT* %vft

endSearchLoop:
  %nexti = add i32 %i, 1
  %continue = icmp ult i32 %nexti, %numSearch
  br i1 %continue, label %searchLoop, label %exit

exit:
  ret %VFT* null
}

define protected %VFT* @plinth_core_generate_supertype_vft(%Descriptor* %thisDescriptor, %VFT* %thisVFT, %FunctionSearchList* %searchDescriptors) {
entry:
  %vftLengthPtr = getelementptr %Descriptor* %thisDescriptor, i32 0, i32 0
  %vftLength = load i32* %vftLengthPtr
  %vftAlloc = call i8* @calloc(i32 ptrtoint (%opaque** getelementptr (%opaque** null, i32 1) to i32), i32 %vftLength)
  %vft = bitcast i8* %vftAlloc to %VFT*
  %check = icmp ult i32 0, %vftLength
  br i1 %check, label %loop, label %exit

loop:
  %i = phi i32 [0, %entry], [%nexti, %loop]
  %disambiguatorPtr = getelementptr %Descriptor* %thisDescriptor, i32 0, i32 1, i32 %i
  %disambiguator = load %RawString** %disambiguatorPtr
  %defaultPtr = getelementptr %VFT* %thisVFT, i32 0, i32 %i
  %default = load %opaque** %defaultPtr
  %func = call %opaque* @plinth_find_vft_function(%RawString* %disambiguator, %opaque* %default, %FunctionSearchList* %searchDescriptors)
  %element = getelementptr %VFT* %vft, i32 0, i32 %i
  store %opaque* %func, %opaque** %element
  %nexti = add i32 %i, 1
  %b = icmp ult i32 %nexti, %vftLength
  br i1 %b, label %loop, label %exit

exit:
  ret %VFT* %vft
}

define private hidden %opaque* @plinth_find_vft_function(%RawString* %disambiguator, %opaque* %default, %FunctionSearchList* %searchDescriptors) {
entry:
  %disambiguatorLengthPtr = getelementptr %RawString* %disambiguator, i32 0, i32 2
  %disambiguatorLength = load i32* %disambiguatorLengthPtr
  %numSearchPtr = getelementptr %FunctionSearchList* %searchDescriptors, i32 0, i32 0
  %numSearch = load i32* %numSearchPtr
  %continueOuterLoop = icmp ult i32 0, %numSearch
  br i1 %continueOuterLoop, label %searchloop, label %exit

searchloop:
  %i = phi i32 [0, %entry], [%nexti, %endsearchloop]
  %descriptorPtr = getelementptr %FunctionSearchList* %searchDescriptors, i32 0, i32 1, i32 %i, i32 0
  %descriptor = load %Descriptor** %descriptorPtr
  %vftPtr = getelementptr %FunctionSearchList* %searchDescriptors, i32 0, i32 1, i32 %i, i32 1
  %vft = load %VFT** %vftPtr
  %numFunctionsPtr = getelementptr %Descriptor* %descriptor, i32 0, i32 0
  %numFunctions = load i32* %numFunctionsPtr
  %runInnerLoop = icmp ult i32 0, %numFunctions
  br i1 %runInnerLoop, label %functionloop, label %endsearchloop

functionloop:
  %j = phi i32 [0, %searchloop], [%nextj, %endfunctionloop]
  %currentDisambiguatorPtr = getelementptr %Descriptor* %descriptor, i32 0, i32 1, i32 %j
  %currentDisambiguator = load %RawString** %currentDisambiguatorPtr
  %currentDisambiguatorLengthPtr = getelementptr %RawString* %currentDisambiguator, i32 0, i32 2
  %currentDisambiguatorLength = load i32* %currentDisambiguatorLengthPtr
  %check = icmp eq i32 %disambiguatorLength, %currentDisambiguatorLength
  br i1 %check, label %checkdisambiguator, label %endfunctionloop

checkdisambiguator:
  %disambiguatorStr = getelementptr %RawString* %disambiguator, i32 0, i32 3, i32 0
  %currentDisambiguatorStr = getelementptr %RawString* %currentDisambiguator, i32 0, i32 3, i32 0
  %comparison = call i32 @strncmp(i8* %disambiguatorStr, i8* %currentDisambiguatorStr, i32 %disambiguatorLength)
  %match = icmp eq i32 %comparison, 0
  br i1 %match, label %foundfunction, label %endfunctionloop

foundfunction:
  %currentFunctionPtr = getelementptr %VFT* %vft, i32 0, i32 %j
  %currentFunction = load %opaque** %currentFunctionPtr
  ret %opaque* %currentFunction

endfunctionloop:
  %nextj = add i32 %j, 1
  %continue = icmp ult i32 %nextj, %numFunctions
  br i1 %continue, label %functionloop, label %endsearchloop

endsearchloop:
  %nexti = add i32 %i, 1
  %b = icmp ult i32 %nexti, %numSearch
  br i1 %b, label %searchloop, label %exit

exit:
  ret %opaque* %default
}
