
%opaque = type opaque
%RawString = type {%opaque*, %opaque*, i32, i8(%RawString*, i32)*, void (%RawString*, i32, i8)*, [0 x i8]}
%String    = type {%opaque*, %opaque*, i32, i8(%String*,    i32)*, void (%String*,    i32, i8)*}
%RTTI = type {i8, i32}
%TypeArgumentMapper = type {i32, [0 x %RTTI*]}

@TypeArgumentMappingErrorMessage = private unnamed_addr constant
                                   { %opaque*, %opaque*, i32, i8(%RawString*, i32)*, void (%RawString*, i32, i8)*, [71 x i8] }
                                   {
                                     %opaque* null,
                                     %opaque* null,
                                     i32 71,
                                     i8(%RawString*, i32)* @TypeArgumentMappingError_string_array_getter,
                                     void (%RawString*, i32, i8)* @TypeArgumentMappingError_string_array_setter,
                                     [71 x i8] c"plinth_is_type_equivalent: error mapping a type parameter! Aborting...\0A"
                                   }
define private protected i8 @TypeArgumentMappingError_string_array_getter(%RawString* %array, i32 %index) {
entry:
  %pointer = getelementptr %RawString* %array, i32 0, i32 5, i32 %index
  %value = load i8* %pointer
  ret i8 %value
}
define private protected void @TypeArgumentMappingError_string_array_setter(%RawString* %array, i32 %index, i8 %value) {
entry:
  %pointer = getelementptr %RawString* %array, i32 0, i32 5, i32 %index
  store i8 %value, i8* %pointer
  ret void
}

declare void @plinth_stderr_write(%String* %array)

declare i8* @calloc(i32, i32)
declare i8* @memcpy(i8* %dest, i8* %source, i32 %size)
declare i32 @strncmp(i8* %str1, i8* %str2, i32 %len)
declare void @abort()


; Checks whether typeA and typeB are equivalent, considering any type parameters inside each type to be replaced with their mapped versions from the corresponding TypeArgument mapper
; A TypeArgument mapper is a pure RTTI block for a NamedType, which contains values for the type arguments with given indices
define protected i1 @plinth_is_type_equivalent(%RTTI* %typeA, %RTTI* %typeB, %TypeArgumentMapper* %mapperA, %TypeArgumentMapper* %mapperB, i1 %ignoreTypeModifiers) {
entry:
  %sortAPtr = getelementptr %RTTI* %typeA, i32 0, i32 0
  %sortBPtr = getelementptr %RTTI* %typeB, i32 0, i32 0
  %sortA = load i8* %sortAPtr
  %sortB = load i8* %sortBPtr
  %aIsTypeParam = icmp eq i8 %sortA, 11
  %mapperAIsNotNull = icmp ne %TypeArgumentMapper* %mapperA, null
  %replaceTypeParameterA = and i1 %aIsTypeParam, %mapperAIsNotNull
  br i1 %replaceTypeParameterA, label %replaceTypeArgumentA, label %checkTypeArgumentB

replaceTypeArgumentA:
  %replacedTypeParameterA = bitcast %RTTI* %typeA to {i8, i32, i1, i1, i32}*
  %typeParameterANullabilityPtr = getelementptr {i8, i32, i1, i1, i32}* %replacedTypeParameterA, i32 0, i32 2
  %typeParameterAImmutabilityPtr = getelementptr {i8, i32, i1, i1, i32}* %replacedTypeParameterA, i32 0, i32 3
  %typeParameterANullability = load i1* %typeParameterANullabilityPtr
  %typeParameterAImmutability = load i1* %typeParameterAImmutabilityPtr
  %replacedAIndexPtr = getelementptr {i8, i32, i1, i1, i32}* %replacedTypeParameterA, i32 0, i32 4
  %replacedAIndex = load i32* %replacedAIndexPtr
  %mapperNumParamsAPtr = getelementptr %TypeArgumentMapper* %mapperA, i32 0, i32 0
  %mapperNumParamsA = load i32* %mapperNumParamsAPtr
  %aIndexInBounds = icmp ult i32 %replacedAIndex, %mapperNumParamsA
  br i1 %aIndexInBounds, label %mapTypeArgumentA, label %typeParamError

mapTypeArgumentA:
  %mappedTypeAPtr = getelementptr %TypeArgumentMapper* %mapperA, i32 0, i32 1, i32 %replacedAIndex
  %mappedTypeA = load %RTTI** %mappedTypeAPtr
  br label %checkTypeArgumentB

checkTypeArgumentB:
  %realTypeA = phi %RTTI* [%typeA, %entry], [%mappedTypeA, %mapTypeArgumentA]
  %forcedNullabilityA = phi i1 [false, %entry], [%typeParameterANullability, %mapTypeArgumentA]
  %forcedImmutabilityA = phi i1 [false, %entry], [%typeParameterAImmutability, %mapTypeArgumentA]
  %bIsTypeParam = icmp eq i8 %sortB, 11
  %mapperBIsNotNull = icmp ne %TypeArgumentMapper* %mapperB, null
  %replaceTypeParameterB = and i1 %bIsTypeParam, %mapperBIsNotNull
  br i1 %replaceTypeParameterB, label %replaceTypeArgumentB, label %checkSorts

replaceTypeArgumentB:
  %replacedTypeParameterB = bitcast %RTTI* %typeB to {i8, i32, i1, i1, i32}*
  %typeParameterBNullabilityPtr = getelementptr {i8, i32, i1, i1, i32}* %replacedTypeParameterB, i32 0, i32 2
  %typeParameterBImmutabilityPtr = getelementptr {i8, i32, i1, i1, i32}* %replacedTypeParameterB, i32 0, i32 3
  %typeParameterBNullability = load i1* %typeParameterBNullabilityPtr
  %typeParameterBImmutability = load i1* %typeParameterBImmutabilityPtr
  %replacedBIndexPtr = getelementptr {i8, i32, i1, i1, i32}* %replacedTypeParameterB, i32 0, i32 4
  %replacedBIndex = load i32* %replacedBIndexPtr
  %mapperNumParamsBPtr = getelementptr %TypeArgumentMapper* %mapperB, i32 0, i32 0
  %mapperNumParamsB = load i32* %mapperNumParamsBPtr
  %bIndexInBounds = icmp ult i32 %replacedBIndex, %mapperNumParamsB
  br i1 %bIndexInBounds, label %mapTypeArgumentB, label %typeParamError

mapTypeArgumentB:
  %mappedTypeBPtr = getelementptr %TypeArgumentMapper* %mapperB, i32 0, i32 1, i32 %replacedBIndex
  %mappedTypeB = load %RTTI** %mappedTypeBPtr
  br label %checkSorts

typeParamError:
  %errorMessage = bitcast { %opaque*, %opaque*, i32, i8(%RawString*, i32)*, void (%RawString*, i32, i8)*, [71 x i8] }* @TypeArgumentMappingErrorMessage to %String*
  call void @plinth_stderr_write(%String* %errorMessage)
  call void @abort()
  unreachable

checkSorts:
  %realTypeB = phi %RTTI* [%typeB, %checkTypeArgumentB], [%mappedTypeB, %mapTypeArgumentB]
  %forcedNullabilityB = phi i1 [false, %checkTypeArgumentB], [%typeParameterBNullability, %mapTypeArgumentB]
  %forcedImmutabilityB = phi i1 [false, %checkTypeArgumentB], [%typeParameterBImmutability, %mapTypeArgumentB]
  %realSortAPtr = getelementptr %RTTI* %realTypeA, i32 0, i32 0
  %realSortBPtr = getelementptr %RTTI* %realTypeB, i32 0, i32 0
  %realSortA = load i8* %realSortAPtr
  %realSortB = load i8* %realSortBPtr
  %sortsEqual = icmp eq i8 %realSortA, %realSortB
  br i1 %sortsEqual, label %chooseSort, label %failure

chooseSort:
  switch i8 %realSortA, label %failure [i8 1, label %object
                                        i8 2, label %primitive
                                        i8 3, label %array
                                        i8 4, label %tuple
                                        i8 5, label %function
                                        i8 6, label %named    ; class
                                        i8 7, label %named    ; compound
                                        i8 8, label %named    ; interface
                                        i8 9, label %success  ; void
                                        i8 10, label %success ; null
                                        i8 11, label %typeParameter]

object:
  %objectA = bitcast %RTTI* %realTypeA to {i8, i32, i1, i1}*
  %objectB = bitcast %RTTI* %realTypeB to {i8, i32, i1, i1}*
  %objectNullabilityAPtr = getelementptr {i8, i32, i1, i1}* %objectA, i32 0, i32 2
  %objectImmutabilityAPtr = getelementptr {i8, i32, i1, i1}* %objectA, i32 0, i32 3
  %objectNullabilityBPtr = getelementptr {i8, i32, i1, i1}* %objectB, i32 0, i32 2
  %objectImmutabilityBPtr = getelementptr {i8, i32, i1, i1}* %objectB, i32 0, i32 3
  %objectNullabilityA = load i1* %objectNullabilityAPtr
  %objectImmutabilityA = load i1* %objectImmutabilityAPtr
  %objectNullabilityB = load i1* %objectNullabilityBPtr
  %objectImmutabilityB = load i1* %objectImmutabilityBPtr
  %realObjectNullabilityA = or i1 %objectNullabilityA, %forcedNullabilityA
  %realObjectImmutabilityA = or i1 %objectImmutabilityA, %forcedImmutabilityA
  %realObjectNullabilityB = or i1 %objectNullabilityB, %forcedNullabilityB
  %realObjectImmutabilityB = or i1 %objectImmutabilityB, %forcedImmutabilityB
  %objectEqualNullability = icmp eq i1 %realObjectNullabilityA, %realObjectNullabilityB
  %objectEqualImmutability = icmp eq i1 %realObjectImmutabilityA, %realObjectImmutabilityB
  %objectEqualNullabilityImmutability = and i1 %objectEqualNullability, %objectEqualImmutability
  %objectResult = or i1 %objectEqualNullabilityImmutability, %ignoreTypeModifiers
  ret i1 %objectResult

primitive:
  %primitiveA = bitcast %RTTI* %realTypeA to {i8, i32, i1, i8}*
  %primitiveB = bitcast %RTTI* %realTypeB to {i8, i32, i1, i8}*
  %primitiveNullabilityAPtr = getelementptr {i8, i32, i1, i8}* %primitiveA, i32 0, i32 2
  %primitiveIdAPtr = getelementptr {i8, i32, i1, i8}* %primitiveA, i32 0, i32 3
  %primitiveNullabilityBPtr = getelementptr {i8, i32, i1, i8}* %primitiveB, i32 0, i32 2
  %primitiveIdBPtr = getelementptr {i8, i32, i1, i8}* %primitiveB, i32 0, i32 3
  %primitiveNullabilityA = load i1* %primitiveNullabilityAPtr
  %primitiveIdA = load i8* %primitiveIdAPtr
  %primitiveNullabilityB = load i1* %primitiveNullabilityBPtr
  %primitiveIdB = load i8* %primitiveIdBPtr
  %realPrimitiveNullabilityA = or i1 %primitiveNullabilityA, %forcedNullabilityA
  %realPrimitiveNullabilityB = or i1 %primitiveNullabilityB, %forcedNullabilityB
  %primitiveEqualNullability = icmp eq i1 %realPrimitiveNullabilityA, %realPrimitiveNullabilityB
  %primitiveNullabilityMatches = or i1 %primitiveEqualNullability, %ignoreTypeModifiers
  %primitiveEqualId = icmp eq i8 %primitiveIdA, %primitiveIdB
  %primitiveResult = and i1 %primitiveNullabilityMatches, %primitiveEqualId
  ret i1 %primitiveResult

array:
  %arrayA = bitcast %RTTI* %realTypeA to {i8, i32, i1, i1, %RTTI*}*
  %arrayB = bitcast %RTTI* %realTypeB to {i8, i32, i1, i1, %RTTI*}*
  %arrayNullabilityAPtr = getelementptr {i8, i32, i1, i1, %RTTI*}* %arrayA, i32 0, i32 2
  %arrayImmutabilityAPtr = getelementptr {i8, i32, i1, i1, %RTTI*}* %arrayA, i32 0, i32 3
  %arrayNullabilityBPtr = getelementptr {i8, i32, i1, i1, %RTTI*}* %arrayB, i32 0, i32 2
  %arrayImmutabilityBPtr = getelementptr {i8, i32, i1, i1, %RTTI*}* %arrayB, i32 0, i32 3
  %arrayNullabilityA = load i1* %arrayNullabilityAPtr
  %arrayImmutabilityA = load i1* %arrayImmutabilityAPtr
  %arrayNullabilityB = load i1* %arrayNullabilityBPtr
  %arrayImmutabilityB = load i1* %arrayImmutabilityBPtr
  %realArrayNullabilityA = or i1 %arrayNullabilityA, %forcedNullabilityA
  %realArrayImmutabilityA = or i1 %arrayImmutabilityA, %forcedImmutabilityA
  %realArrayNullabilityB = or i1 %arrayNullabilityB, %forcedNullabilityB
  %realArrayImmutabilityB = or i1 %arrayImmutabilityB, %forcedImmutabilityB
  %arrayEqualNullability = icmp eq i1 %realArrayNullabilityA, %realArrayNullabilityB
  %arrayEqualImmutability = icmp eq i1 %realArrayImmutabilityA, %realArrayImmutabilityB
  %arrayEqualNullabilityImmutability = and i1 %arrayEqualNullability, %arrayEqualImmutability
  %arrayPreliminaryResult = or i1 %arrayEqualNullabilityImmutability, %ignoreTypeModifiers
  br i1 %arrayPreliminaryResult, label %arrayContinue, label %failure
  
arrayContinue:
  %arrayBaseTypeAPtr = getelementptr {i8, i32, i1, i1, %RTTI*}* %arrayA, i32 0, i32 4
  %arrayBaseTypeBPtr = getelementptr {i8, i32, i1, i1, %RTTI*}* %arrayB, i32 0, i32 4
  %arrayBaseTypeA = load %RTTI** %arrayBaseTypeAPtr
  %arrayBaseTypeB = load %RTTI** %arrayBaseTypeBPtr
  %arrayResult = call i1 @plinth_is_type_equivalent(%RTTI* %arrayBaseTypeA, %RTTI* %arrayBaseTypeB, %TypeArgumentMapper* %mapperA, %TypeArgumentMapper* %mapperB, i1 0)
  ret i1 %arrayResult

tuple:
  %tupleA = bitcast %RTTI* %realTypeA to {i8, i32, i1, i32, [0 x %RTTI*]}*
  %tupleB = bitcast %RTTI* %realTypeB to {i8, i32, i1, i32, [0 x %RTTI*]}*
  %tupleNullabilityAPtr = getelementptr {i8, i32, i1, i32, [0 x %RTTI*]}* %tupleA, i32 0, i32 2
  %tupleNumSubTypesAPtr = getelementptr {i8, i32, i1, i32, [0 x %RTTI*]}* %tupleA, i32 0, i32 3
  %tupleNullabilityBPtr = getelementptr {i8, i32, i1, i32, [0 x %RTTI*]}* %tupleB, i32 0, i32 2
  %tupleNumSubTypesBPtr = getelementptr {i8, i32, i1, i32, [0 x %RTTI*]}* %tupleB, i32 0, i32 3
  %tupleNullabilityA = load i1* %tupleNullabilityAPtr
  %tupleNumSubTypesA = load i32* %tupleNumSubTypesAPtr
  %tupleNullabilityB = load i1* %tupleNullabilityBPtr
  %tupleNumSubTypesB = load i32* %tupleNumSubTypesBPtr
  %realTupleNullabilityA = or i1 %tupleNullabilityA, %forcedNullabilityA
  %realTupleNullabilityB = or i1 %tupleNullabilityB, %forcedNullabilityB
  %tupleEqualNullability = icmp eq i1 %realTupleNullabilityA, %realTupleNullabilityB
  %tupleNullabilityMatches = or i1 %tupleEqualNullability, %ignoreTypeModifiers
  %tupleEqualNumSubTypes = icmp eq i32 %tupleNumSubTypesA, %tupleNumSubTypesB
  %tuplePreliminaryResult = and i1 %tupleNullabilityMatches, %tupleEqualNumSubTypes
  br i1 %tuplePreliminaryResult, label %tupleLoopStart, label %failure

tupleLoopStart:
  %skipTupleLoop = icmp eq i32 %tupleNumSubTypesA, 0
  br i1 %skipTupleLoop, label %success, label %tupleLoop

tupleLoop:
  %tupleLoopCounter = phi i32 [0, %tupleLoopStart], [%nextTupleLoopCounter, %tupleLoopCheck]
  %tupleSubTypeAPtr = getelementptr {i8, i32, i1, i32, [0 x %RTTI*]}* %tupleA, i32 0, i32 4, i32 %tupleLoopCounter
  %tupleSubTypeBPtr = getelementptr {i8, i32, i1, i32, [0 x %RTTI*]}* %tupleB, i32 0, i32 4, i32 %tupleLoopCounter
  %tupleSubTypeA = load %RTTI** %tupleSubTypeAPtr
  %tupleSubTypeB = load %RTTI** %tupleSubTypeBPtr
  %tupleElementComparison = call i1 @plinth_is_type_equivalent(%RTTI* %tupleSubTypeA, %RTTI* %tupleSubTypeB, %TypeArgumentMapper* %mapperA, %TypeArgumentMapper* %mapperB, i1 0)
  br i1 %tupleElementComparison, label %tupleLoopCheck, label %failure

tupleLoopCheck:
  %nextTupleLoopCounter = add i32 %tupleLoopCounter, 1
  %tupleLoopContinue = icmp ult i32 %nextTupleLoopCounter, %tupleNumSubTypesA
  br i1 %tupleLoopContinue, label %tupleLoop, label %success

function:
  %functionA = bitcast %RTTI* %realTypeA to {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}*
  %functionB = bitcast %RTTI* %realTypeB to {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}*
  %functionNullabilityAPtr = getelementptr {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionA, i32 0, i32 2
  %functionImmutabilityAPtr = getelementptr {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionA, i32 0, i32 3
  %functionNumParametersAPtr = getelementptr {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionA, i32 0, i32 5
  %functionNullabilityBPtr = getelementptr {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionB, i32 0, i32 2
  %functionImmutabilityBPtr = getelementptr {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionB, i32 0, i32 3
  %functionNumParametersBPtr = getelementptr {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionB, i32 0, i32 5
  %functionNullabilityA = load i1* %functionNullabilityAPtr
  %functionImmutabilityA = load i1* %functionImmutabilityAPtr
  %functionNumParametersA = load i32* %functionNumParametersAPtr
  %functionNullabilityB = load i1* %functionNullabilityBPtr
  %functionImmutabilityB = load i1* %functionImmutabilityBPtr
  %functionNumParametersB = load i32* %functionNumParametersBPtr
  %realFunctionNullabilityA = or i1 %functionNullabilityA, %forcedNullabilityA
  %realFunctionNullabilityB = or i1 %functionNullabilityB, %forcedNullabilityB
  ; NOTE: function immutability cannot be forced, since it is not a form of data-immutability
  %functionEqualNullability = icmp eq i1 %realFunctionNullabilityA, %realFunctionNullabilityB
  %functionNullabilityMatches = or i1 %functionEqualNullability, %ignoreTypeModifiers
  %functionEqualImmutability = icmp eq i1 %functionImmutabilityA, %functionImmutabilityB
  %functionEqualNumParameters = icmp eq i32 %functionNumParametersA, %functionNumParametersB
  %functionNullabilityImmutability = and i1 %functionNullabilityMatches, %functionEqualImmutability
  %functionPreliminaryResult = and i1 %functionNullabilityImmutability, %functionEqualNumParameters
  br i1 %functionPreliminaryResult, label %functionReturnTypeCheck, label %failure

functionReturnTypeCheck:
  %functionReturnTypeAPtr = getelementptr {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionA, i32 0, i32 4
  %functionReturnTypeBPtr = getelementptr {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionB, i32 0, i32 4
  %functionReturnTypeA = load %RTTI** %functionReturnTypeAPtr
  %functionReturnTypeB = load %RTTI** %functionReturnTypeBPtr
  %functionEqualReturnTypes = call i1 @plinth_is_type_equivalent(%RTTI* %functionReturnTypeA, %RTTI* %functionReturnTypeB, %TypeArgumentMapper* %mapperA, %TypeArgumentMapper* %mapperB, i1 0)
  br i1 %functionEqualReturnTypes, label %functionLoopStart, label %failure

functionLoopStart:
  %skipFunctionLoop = icmp eq i32 %functionNumParametersA, 0
  br i1 %skipFunctionLoop, label %success, label %functionLoop

functionLoop:
  %functionLoopCounter = phi i32 [0, %functionLoopStart], [%nextFunctionLoopCounter, %functionLoopCheck]
  %functionParameterTypeAPtr = getelementptr {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionA, i32 0, i32 6, i32 %functionLoopCounter
  %functionParameterTypeBPtr = getelementptr {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionB, i32 0, i32 6, i32 %functionLoopCounter
  %functionParameterTypeA = load %RTTI** %functionParameterTypeAPtr
  %functionParameterTypeB = load %RTTI** %functionParameterTypeBPtr
  %functionElementComparison = call i1 @plinth_is_type_equivalent(%RTTI* %functionParameterTypeA, %RTTI* %functionParameterTypeB, %TypeArgumentMapper* %mapperA, %TypeArgumentMapper* %mapperB, i1 0)
  br i1 %functionElementComparison, label %functionLoopCheck, label %failure

functionLoopCheck:
  %nextFunctionLoopCounter = add i32 %functionLoopCounter, 1
  %functionLoopContinue = icmp ult i32 %nextFunctionLoopCounter, %functionNumParametersA
  br i1 %functionLoopContinue, label %functionLoop, label %success

named:
  %namedA = bitcast %RTTI* %realTypeA to {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}*
  %namedB = bitcast %RTTI* %realTypeB to {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}*
  %namedNullabilityAPtr = getelementptr {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}* %namedA, i32 0, i32 2
  %namedImmutabilityAPtr = getelementptr {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}* %namedA, i32 0, i32 3
  %namedNumArgumentsAPtr = getelementptr {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}* %namedA, i32 0, i32 5, i32 0
  %namedNullabilityBPtr = getelementptr {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}* %namedB, i32 0, i32 2
  %namedImmutabilityBPtr = getelementptr {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}* %namedB, i32 0, i32 3
  %namedNumArgumentsBPtr = getelementptr {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}* %namedB, i32 0, i32 5, i32 0
  %namedNullabilityA = load i1* %namedNullabilityAPtr
  %namedImmutabilityA = load i1* %namedImmutabilityAPtr
  %namedNumArgumentsA = load i32* %namedNumArgumentsAPtr
  %namedNullabilityB = load i1* %namedNullabilityBPtr
  %namedImmutabilityB = load i1* %namedImmutabilityBPtr
  %namedNumArgumentsB = load i32* %namedNumArgumentsBPtr
  %realNamedNullabilityA = or i1 %namedNullabilityA, %forcedNullabilityA
  %realNamedImmutabilityA = or i1 %namedImmutabilityA, %forcedImmutabilityA
  %realNamedNullabilityB = or i1 %namedNullabilityB, %forcedNullabilityB
  %realNamedImmutabilityB = or i1 %namedImmutabilityB, %forcedImmutabilityB
  %namedEqualNullability = icmp eq i1 %realNamedNullabilityA, %realNamedNullabilityB
  %namedEqualImmutability = icmp eq i1 %realNamedImmutabilityA, %realNamedImmutabilityB
  %namedNullabilityImmutability = and i1 %namedEqualNullability, %namedEqualImmutability
  %namedTypeModifiersMatch = or i1 %namedNullabilityImmutability, %ignoreTypeModifiers
  %namedEqualNumArguments = icmp eq i32 %namedNumArgumentsA, %namedNumArgumentsB
  %namedPreliminaryResult = and i1 %namedTypeModifiersMatch, %namedEqualNumArguments
  br i1 %namedPreliminaryResult, label %nameLengthCheck, label %failure

nameLengthCheck:
  %namedNameAPtr = getelementptr {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}* %namedA, i32 0, i32 4
  %namedNameBPtr = getelementptr {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}* %namedB, i32 0, i32 4
  %namedNameA = load %RawString** %namedNameAPtr
  %namedNameB = load %RawString** %namedNameBPtr
  %nameLengthAPtr = getelementptr %RawString* %namedNameA, i32 0, i32 2
  %nameLengthBPtr = getelementptr %RawString* %namedNameB, i32 0, i32 2
  %nameLengthA = load i32* %nameLengthAPtr
  %nameLengthB = load i32* %nameLengthBPtr
  %equalNameLengths = icmp eq i32 %nameLengthA, %nameLengthB
  br i1 %equalNameLengths, label %nameCheck, label %failure

nameCheck:
  %nameBytesA = getelementptr %RawString* %namedNameA, i32 0, i32 5, i32 0
  %nameBytesB = getelementptr %RawString* %namedNameB, i32 0, i32 5, i32 0
  %nameCompareResult = call i32 @strncmp(i8* %nameBytesA, i8* %nameBytesB, i32 %nameLengthA)
  %namesMatch = icmp eq i32 %nameCompareResult, 0
  br i1 %namesMatch, label %typeArgumentLoopStart, label %failure

typeArgumentLoopStart:
  %skipTypeArgumentLoop = icmp eq i32 %namedNumArgumentsA, 0
  br i1 %skipTypeArgumentLoop, label %success, label %typeArgumentLoop

typeArgumentLoop:
  %typeArgumentLoopCounter = phi i32 [0, %typeArgumentLoopStart], [%nextTypeArgumentLoopCounter, %typeArgumentLoopCheck]
  %typeArgumentAPtr = getelementptr {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}* %namedA, i32 0, i32 5, i32 1, i32 %typeArgumentLoopCounter
  %typeArgumentBPtr = getelementptr {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}* %namedB, i32 0, i32 5, i32 1, i32 %typeArgumentLoopCounter
  %typeArgumentA = load %RTTI** %typeArgumentAPtr
  %typeArgumentB = load %RTTI** %typeArgumentBPtr
  %typeArgumentsEquivalent = call i1 @plinth_is_type_equivalent(%RTTI* %typeArgumentA, %RTTI* %typeArgumentB, %TypeArgumentMapper* %mapperA, %TypeArgumentMapper* %mapperB, i1 0)
  br i1 %typeArgumentsEquivalent, label %typeArgumentLoopCheck, label %failure

typeArgumentLoopCheck:
  %nextTypeArgumentLoopCounter = add i32 %typeArgumentLoopCounter, 1
  %typeArgumentLoopContinue = icmp ult i32 %nextTypeArgumentLoopCounter, %namedNumArgumentsA
  br i1 %typeArgumentLoopContinue, label %typeArgumentLoop, label %success

typeParameter:
  %typeParameterA = bitcast %RTTI* %realTypeA to {i8, i32, i1, i1, i32}*
  %typeParameterB = bitcast %RTTI* %realTypeB to {i8, i32, i1, i1, i32}*
  %typeParameterNullabilityAPtr = getelementptr {i8, i32, i1, i1, i32}* %typeParameterA, i32 0, i32 2
  %typeParameterImmutabilityAPtr = getelementptr {i8, i32, i1, i1, i32}* %typeParameterA, i32 0, i32 3
  %typeParameterIndexAPtr = getelementptr {i8, i32, i1, i1, i32}* %typeParameterA, i32 0, i32 4
  %typeParameterNullabilityBPtr = getelementptr {i8, i32, i1, i1, i32}* %typeParameterB, i32 0, i32 2
  %typeParameterImmutabilityBPtr = getelementptr {i8, i32, i1, i1, i32}* %typeParameterB, i32 0, i32 3
  %typeParameterIndexBPtr = getelementptr {i8, i32, i1, i1, i32}* %typeParameterB, i32 0, i32 4
  %typeParameterNullabilityA = load i1* %typeParameterNullabilityAPtr
  %typeParameterImmutabilityA = load i1* %typeParameterImmutabilityAPtr
  %typeParameterIndexA = load i32* %typeParameterIndexAPtr
  %typeParameterNullabilityB = load i1* %typeParameterNullabilityBPtr
  %typeParameterImmutabilityB = load i1* %typeParameterImmutabilityBPtr
  %typeParameterIndexB = load i32* %typeParameterIndexBPtr
  %realTypeParameterNullabilityA = or i1 %typeParameterNullabilityA, %forcedNullabilityA
  %realTypeParameterImmutabilityA = or i1 %typeParameterImmutabilityA, %forcedImmutabilityA
  %realTypeParameterNullabilityB = or i1 %typeParameterNullabilityB, %forcedNullabilityB
  %realTypeParameterImmutabilityB = or i1 %typeParameterImmutabilityB, %forcedImmutabilityB
  %typeParameterEqualNullability = icmp eq i1 %realTypeParameterNullabilityA, %realTypeParameterNullabilityB
  %typeParameterEqualImmutability = icmp eq i1 %realTypeParameterImmutabilityA, %realTypeParameterImmutabilityB
  %typeParameterEqualIndex = icmp eq i32 %typeParameterIndexA, %typeParameterIndexB
  %typeParameterNullibilityImmutability = and i1 %typeParameterEqualNullability, %typeParameterEqualImmutability
  %typeParameterModifiersMatch = or i1 %typeParameterNullibilityImmutability, %ignoreTypeModifiers
  %typeParameterResult = and i1 %typeParameterModifiersMatch, %typeParameterEqualIndex
  ret i1 %typeParameterResult

failure:
  ret i1 false

success:
  ret i1 true

}

define %RTTI* @plinth_force_type_modifiers(%RTTI* %type, i1 %onlyAdd, i1 %forceNullable, i1 %forceImmutable) {
entry:
  %sortPtr = getelementptr %RTTI* %type, i32 0, i32 0
  %sort = load i8* %sortPtr
  switch i8 %sort, label %notApplicable [i8  1, label %bothNullableImmutable  ; object
                                         i8  2, label %onlyNullable           ; primitive
                                         i8  3, label %bothNullableImmutable  ; array
                                         i8  4, label %onlyNullable           ; tuple
                                         i8  5, label %onlyNullable           ; function (function immutability is not data-immutability)
                                         i8  6, label %bothNullableImmutable  ; class
                                         i8  7, label %bothNullableImmutable  ; compound
                                         i8  8, label %bothNullableImmutable  ; interface
                                         i8  9, label %notApplicable          ; void
                                         i8 10, label %notApplicable          ; null
                                         i8 11, label %bothNullableImmutable] ; type parameter

bothNullableImmutable:
  %nullableImmutableRTTI = bitcast %RTTI* %type to {i8, i32, i1, i1}*
  %immutablePtr = getelementptr {i8, i32, i1, i1}* %nullableImmutableRTTI, i32 0, i32 3
  %currentImmutable = load i1* %immutablePtr
  %currentForcedImmutable = and i1 %currentImmutable, %onlyAdd
  %alteredImmutable = or i1 %currentForcedImmutable, %forceImmutable
  %immutabilityNeedsAltering = xor i1 %currentImmutable, %alteredImmutable
  br i1 %immutabilityNeedsAltering, label %makeAlteration, label %onlyNullable

onlyNullable:
  %nullableRTTI = bitcast %RTTI* %type to {i8, i32, i1}*
  %nullablePtr = getelementptr {i8, i32, i1}* %nullableRTTI, i32 0, i32 2
  %currentNullable = load i1* %nullablePtr
  %currentForcedNullable = and i1 %currentNullable, %onlyAdd
  %alteredNullable = or i1 %currentForcedNullable, %forceNullable
  %nullabilityNeedsAltering = xor i1 %currentNullable, %alteredNullable
  br i1 %nullabilityNeedsAltering, label %makeAlteration, label %notApplicable

notApplicable:
  ret %RTTI* %type

makeAlteration:
  switch i8 %sort, label %notApplicable [i8  1, label %alterObject         ; object
                                         i8  2, label %alterPrimitive      ; primitive
                                         i8  3, label %alterArray          ; array
                                         i8  4, label %alterTuple          ; tuple
                                         i8  5, label %alterFunction       ; function (function immutability is not data-immutability)
                                         i8  6, label %alterNamed          ; class
                                         i8  7, label %alterNamed          ; compound
                                         i8  8, label %alterNamed          ; interface
                                         i8  9, label %notApplicable       ; void
                                         i8 10, label %notApplicable       ; null
                                         i8 11, label %alterTypeParameter] ; type parameter

alterObject:
  %objectSizePtr = getelementptr {i8, i32, i1, i1}* null, i32 1
  %objectSize = ptrtoint {i8, i32, i1, i1}* %objectSizePtr to i32
  br label %generalAlterBoth

alterPrimitive:
  %primitiveSizePtr = getelementptr {i8, i32, i1, i8}* null, i32 1
  %primitiveSize = ptrtoint {i8, i32, i1, i8}* %primitiveSizePtr to i32
  br label %generalAlterNullable

alterArray:
  %arraySizePtr = getelementptr {i8, i32, i1, i1, %RTTI*}* null, i32 1
  %arraySize = ptrtoint {i8, i32, i1, i1, %RTTI*}* %arraySizePtr to i32
  br label %generalAlterBoth

alterTuple:
  %tupleRTTI = bitcast %RTTI* %type to {i8, i32, i1, i32, [0 x %RTTI*]}*
  %tupleNumSubTypesPtr = getelementptr {i8, i32, i1, i32, [0 x %RTTI*]}* %tupleRTTI, i32 0, i32 3
  %tupleNumSubTypes = load i32* %tupleNumSubTypesPtr
  %tupleSizePtr = getelementptr {i8, i32, i1, i32, [0 x %RTTI*]}* null, i32 0, i32 4, i32 %tupleNumSubTypes
  %tupleSize = ptrtoint %RTTI** %tupleSizePtr to i32
  br label %generalAlterNullable

alterFunction:
  %functionRTTI = bitcast %RTTI* %type to {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}*
  %functionNumParametersPtr = getelementptr {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionRTTI, i32 0, i32 5
  %functionNumParameters = load i32* %functionNumParametersPtr
  %functionSizePtr = getelementptr {i8, i32, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* null, i32 0, i32 6, i32 %functionNumParameters
  %functionSize = ptrtoint %RTTI** %functionSizePtr to i32
  br label %generalAlterNullable

alterNamed:
  %namedRTTI = bitcast %RTTI* %type to {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}*
  %namedNumArgumentsPtr = getelementptr {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}* %namedRTTI, i32 0, i32 5, i32 0
  %namedNumArguments = load i32* %namedNumArgumentsPtr
  %namedSizePtr = getelementptr {i8, i32, i1, i1, %RawString*, %TypeArgumentMapper}* null, i32 0, i32 5, i32 1, i32 %namedNumArguments
  %namedSize = ptrtoint %RTTI** %namedSizePtr to i32
  br label %generalAlterBoth

alterTypeParameter:
  %typeParameterSizePtr = getelementptr {i8, i32, i1, i1, i32}* null, i32 1
  %typeParameterSize = ptrtoint {i8, i32, i1, i1, i32}* %typeParameterSizePtr to i32
  br label %generalAlterBoth

generalAlterNullable:
  %alterNullableSize = phi i32 [%primitiveSize, %alterPrimitive], [%tupleSize, %alterTuple], [%functionSize, %alterFunction]
  br label %reallocate

generalAlterBoth:
  %alterBothSize = phi i32 [%objectSize, %alterObject], [%arraySize, %alterArray], [%namedSize, %alterNamed], [%typeParameterSize, %alterTypeParameter]
  br label %reallocate

reallocate:
  %size = phi i32 [%alterNullableSize, %generalAlterNullable], [%alterBothSize, %generalAlterBoth]
  %modifyImmutability = phi i1 [false, %generalAlterNullable], [true, %generalAlterBoth]
  %alloc = call i8* @calloc(i32 %size, i32 1)
  %typePointer = bitcast %RTTI* %type to i8*
  call i8* @memcpy(i8* %alloc, i8* %typePointer, i32 %size)
  br i1 %modifyImmutability, label %alterImmutability, label %alterNullability

alterImmutability:
  %oldRTTIWithImmutability = bitcast %RTTI* %type to {i8, i32, i1, i1}*
  %newRTTIWithImmutability = bitcast i8* %alloc to {i8, i32, i1, i1}*
  %oldImmutabilityPtr = getelementptr {i8, i32, i1, i1}* %oldRTTIWithImmutability, i32 0, i32 3
  %newImmutabilityPtr = getelementptr {i8, i32, i1, i1}* %newRTTIWithImmutability, i32 0, i32 3
  %oldImmutability = load i1* %oldImmutabilityPtr
  %oldForcedImmutability = and i1 %oldImmutability, %onlyAdd
  %newImmutability = or i1 %oldForcedImmutability, %forceImmutable
  store i1 %newImmutability, i1* %newImmutabilityPtr
  br label %alterNullability

alterNullability:
  %oldRTTIWithNullability = bitcast %RTTI* %type to {i8, i32, i1}*
  %newRTTIWithNullability = bitcast i8* %alloc to {i8, i32, i1}*
  %oldNullabilityPtr = getelementptr {i8, i32, i1}* %oldRTTIWithNullability, i32 0, i32 2
  %newNullabilityPtr = getelementptr {i8, i32, i1}* %newRTTIWithNullability, i32 0, i32 2
  %oldNullability = load i1* %oldNullabilityPtr
  %oldForcedNullability = and i1 %oldNullability, %onlyAdd
  %newNullability = or i1 %oldForcedNullability, %forceNullable
  store i1 %newNullability, i1* %newNullabilityPtr
  %result = bitcast i8* %alloc to %RTTI*
  ret %RTTI* %result
}

