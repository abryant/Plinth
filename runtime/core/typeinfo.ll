
%opaque = type opaque
%VFT = type [0 x %opaque*]
%RawString = type {%opaque*, %opaque*, i32, i8(%RawString*, i32)*, void (%RawString*, i32, i8)*, [0 x i8]}
%String    = type {%opaque*, %opaque*, i32, i8(%String*,    i32)*, void (%String*,    i32, i8)*}
%RTTI = type {%TypeSearchList*, i8}
%TypeArgumentMapper = type {i32, [0 x %RTTI*]}

%TypeSearchList = type {i32, [0 x {%RTTI*, %VFT*}]}

@TypeArgumentMappingErrorMessage = private unnamed_addr constant
                                   { %opaque*, %opaque*, i32, i8(%RawString*, i32)*, void (%RawString*, i32, i8)*, [44 x i8] }
                                   {
                                     %opaque* null,
                                     %opaque* null,
                                     i32 44,
                                     i8(%RawString*, i32)* @TypeArgumentMappingError_string_array_getter,
                                     void (%RawString*, i32, i8)* @TypeArgumentMappingError_string_array_setter,
                                     [44 x i8] c"error mapping a type parameter! Aborting...\0A"
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

define protected {%RTTI*, %VFT*} @plinth_core_find_super_type(%RTTI* %thisRTTI, %RTTI* %searchRTTI, %TypeArgumentMapper* %searchTypeMapper) {
entry:
  %typeSearchListPtr = getelementptr %RTTI* %thisRTTI, i32 0, i32 0
  %typeSearchList = load %TypeSearchList** %typeSearchListPtr
  ; extract a pointer to the TypeArgumentMapper from this object's RTTI
  ; note: this object isn't necessarily a NamedType, so this bitcast and GEP might return something which cannot be used without causing undefined behaviour
  ;       however, it will only be used by 'plinth_check_type_matches' if the search list contains type parameters, and since type parameters only occur in named types,
  ;       we can be sure that no errors will occur
  %thisNamedRTTI = bitcast %RTTI* %thisRTTI to {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}*
  %thisTypeArgumentMapper = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %thisNamedRTTI, i32 0, i32 5
  %numSearchPtr = getelementptr %TypeSearchList* %typeSearchList, i32 0, i32 0
  %numSearch = load i32* %numSearchPtr
  %continueLoop = icmp ult i32 0, %numSearch
  br i1 %continueLoop, label %searchLoop, label %exit

searchLoop:
  %i = phi i32 [0, %entry], [%nexti, %endSearchLoop]
  %rttiPtr = getelementptr %TypeSearchList* %typeSearchList, i32 0, i32 1, i32 %i, i32 0
  %rtti = load %RTTI** %rttiPtr
  %match = call i1 @plinth_check_type_matches(%RTTI* %rtti, %RTTI* %searchRTTI, %TypeArgumentMapper* %thisTypeArgumentMapper, %TypeArgumentMapper* %searchTypeMapper, i1 true, i1 true, i1 true)
  br i1 %match, label %return, label %endSearchLoop

return:
  %vftPtr = getelementptr %TypeSearchList* %typeSearchList, i32 0, i32 1, i32 %i, i32 1
  %vft = load %VFT** %vftPtr
  %result = insertvalue {%RTTI*, %VFT*} undef, %RTTI* %rtti, 0
  %result2 = insertvalue {%RTTI*, %VFT*} %result, %VFT* %vft, 1
  ret {%RTTI*, %VFT*} %result2

endSearchLoop:
  %nexti = add i32 %i, 1
  %continue = icmp ult i32 %nexti, %numSearch
  br i1 %continue, label %searchLoop, label %exit

exit:
  ret {%RTTI*, %VFT*} zeroinitializer
}


; Checks whether queryType matches specType, considering any type parameters inside each type to be replaced with their mapped versions from the corresponding TypeArgumentMapper.
; If %ignoreNullability is true, the nullability of the top-level type being checked is ignored.
; If %ignoreImmutability is true, the data-immutability of the top-level type being checked is ignored.
; If %looselyMatchWildcards is false, this is an equivalence check. If it is true, wildcard type arguments in specType can match a range of different type arguments in queryType.
; A TypeArgumentMapper is a structure which stores the values of type parameters based on their indices. If a type parameter is encountered, its index is looked up and its mapped version is used instead.
define protected i1 @plinth_check_type_matches(%RTTI* %queryType, %RTTI* %specType, %TypeArgumentMapper* %queryMapper, %TypeArgumentMapper* %specMapper, i1 %ignoreNullability, i1 %ignoreImmutability, i1 %looselyMatchWildcards) {
entry:
  %sortQueryPtr = getelementptr %RTTI* %queryType, i32 0, i32 1
  %sortSpecPtr = getelementptr %RTTI* %specType, i32 0, i32 1
  %sortQuery = load i8* %sortQueryPtr
  %sortSpec = load i8* %sortSpecPtr
  %queryIsTypeParam = icmp eq i8 %sortQuery, 11
  %queryMapperIsNotNull = icmp ne %TypeArgumentMapper* %queryMapper, null
  %replaceQueryTypeParameter = and i1 %queryIsTypeParam, %queryMapperIsNotNull
  br i1 %replaceQueryTypeParameter, label %replaceQueryTypeArgument, label %checkSpecTypeArgument

replaceQueryTypeArgument:
  %replacedQueryTypeParameter = bitcast %RTTI* %queryType to {%TypeSearchList*, i8, i1, i1, i32}*
  %queryReplaceTypeParameterNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %replacedQueryTypeParameter, i32 0, i32 2
  %queryReplaceTypeParameterImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %replacedQueryTypeParameter, i32 0, i32 3
  %queryReplaceTypeParameterNullability = load i1* %queryReplaceTypeParameterNullabilityPtr
  %queryReplaceTypeParameterImmutability = load i1* %queryReplaceTypeParameterImmutabilityPtr
  %replacedQueryIndexPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %replacedQueryTypeParameter, i32 0, i32 4
  %replacedQueryIndex = load i32* %replacedQueryIndexPtr
  %queryMapperNumParamsPtr = getelementptr %TypeArgumentMapper* %queryMapper, i32 0, i32 0
  %queryMapperNumParams = load i32* %queryMapperNumParamsPtr
  %queryIndexInBounds = icmp ult i32 %replacedQueryIndex, %queryMapperNumParams
  br i1 %queryIndexInBounds, label %mapQueryTypeArgument, label %typeParamError

mapQueryTypeArgument:
  %mappedTypeAPtr = getelementptr %TypeArgumentMapper* %queryMapper, i32 0, i32 1, i32 %replacedQueryIndex
  %mappedTypeA = load %RTTI** %mappedTypeAPtr
  br label %checkSpecTypeArgument

checkSpecTypeArgument:
  %realQueryType = phi %RTTI* [%queryType, %entry], [%mappedTypeA, %mapQueryTypeArgument]
  %queryForcedNullability = phi i1 [false, %entry], [%queryReplaceTypeParameterNullability, %mapQueryTypeArgument]
  %queryForcedImmutability = phi i1 [false, %entry], [%queryReplaceTypeParameterImmutability, %mapQueryTypeArgument]
  %specIsTypeParam = icmp eq i8 %sortSpec, 11
  %specMapperIsNotNull = icmp ne %TypeArgumentMapper* %specMapper, null
  %replaceSpecTypeParameter = and i1 %specIsTypeParam, %specMapperIsNotNull
  br i1 %replaceSpecTypeParameter, label %replaceSpecTypeArgument, label %checkSorts

replaceSpecTypeArgument:
  %replacedSpecTypeParameter = bitcast %RTTI* %specType to {%TypeSearchList*, i8, i1, i1, i32}*
  %specReplaceTypeParameterNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %replacedSpecTypeParameter, i32 0, i32 2
  %specReplaceTypeParameterImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %replacedSpecTypeParameter, i32 0, i32 3
  %specReplaceTypeParameterNullability = load i1* %specReplaceTypeParameterNullabilityPtr
  %specReplaceTypeParameterImmutability = load i1* %specReplaceTypeParameterImmutabilityPtr
  %replacedSpecIndexPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %replacedSpecTypeParameter, i32 0, i32 4
  %replacedSpecIndex = load i32* %replacedSpecIndexPtr
  %specMapperNumParamsPtr = getelementptr %TypeArgumentMapper* %specMapper, i32 0, i32 0
  %specMapperNumParams = load i32* %specMapperNumParamsPtr
  %specIndexInBounds = icmp ult i32 %replacedSpecIndex, %specMapperNumParams
  br i1 %specIndexInBounds, label %mapSpecTypeArgument, label %typeParamError

mapSpecTypeArgument:
  %specMappedTypePtr = getelementptr %TypeArgumentMapper* %specMapper, i32 0, i32 1, i32 %replacedSpecIndex
  %specMappedType = load %RTTI** %specMappedTypePtr
  br label %checkSorts

typeParamError:
  %errorMessage = bitcast { %opaque*, %opaque*, i32, i8(%RawString*, i32)*, void (%RawString*, i32, i8)*, [44 x i8] }* @TypeArgumentMappingErrorMessage to %String*
  call void @plinth_stderr_write(%String* %errorMessage)
  call void @abort()
  unreachable

checkSorts:
  %realSpecType = phi %RTTI* [%specType, %checkSpecTypeArgument], [%specMappedType, %mapSpecTypeArgument]
  %forcedSpecNullability = phi i1 [false, %checkSpecTypeArgument], [%specReplaceTypeParameterNullability, %mapSpecTypeArgument]
  %forcedSpecImmutability = phi i1 [false, %checkSpecTypeArgument], [%specReplaceTypeParameterImmutability, %mapSpecTypeArgument]
  %realQuerySortPtr = getelementptr %RTTI* %realQueryType, i32 0, i32 1
  %realSpecSortPtr = getelementptr %RTTI* %realSpecType, i32 0, i32 1
  %realQuerySort = load i8* %realQuerySortPtr
  %realSpecSort = load i8* %realSpecSortPtr
  %specTypeIsWildcard = icmp eq i8 %realSpecSort, 12
  %isLooseWildcardCheck = and i1 %specTypeIsWildcard, %looselyMatchWildcards
  br i1 %isLooseWildcardCheck, label %checkWildcardSpecType, label %checkSortsEquality

checkWildcardSpecType:
  %wildcardSpecType = bitcast %RTTI* %realSpecType to {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}*
  %wildcardSpecNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardSpecType, i32 0, i32 2
  %wildcardSpecImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardSpecType, i32 0, i32 3
  %wildcardSpecNullability = load i1* %wildcardSpecNullabilityPtr
  %wildcardSpecImmutability = load i1* %wildcardSpecImmutabilityPtr
  %wildcardForcedSpecNullability = or i1 %wildcardSpecNullability, %forcedSpecNullability
  %wildcardForcedSpecImmutability = or i1 %wildcardSpecImmutability, %forcedSpecImmutability
  %queryIsNullable = call i1 @plinth_is_nullable(%RTTI* %realQueryType)
  %queryIsImmutable = call i1 @plinth_is_immutable(%RTTI* %realQueryType)
  %forcedQueryIsNullable = or i1 %queryIsNullable, %queryForcedNullability
  %forcedQueryIsImmutable = or i1 %queryIsImmutable, %queryForcedImmutability
  %queryIsNotNullable = xor i1 %forcedQueryIsNullable, 1
  %queryIsNotImmutable = xor i1 %forcedQueryIsImmutable, 1
  %wildcardSpecNullabilityMatches = or i1 %queryIsNotNullable, %wildcardForcedSpecNullability
  %wildcardSpecImmutabilityMatches = or i1 %queryIsNotImmutable, %wildcardForcedSpecImmutability
  %wildcardSpecNullabilityMatchesOrIgnored = or i1 %wildcardSpecNullabilityMatches, %ignoreNullability
  %wildcardSpecImmutabilityMatchesOrIgnored = or i1 %wildcardSpecImmutabilityMatches, %ignoreImmutability
  %wildcardSpecTypeModifiersMatch = and i1 %wildcardSpecNullabilityMatchesOrIgnored, %wildcardSpecImmutabilityMatchesOrIgnored
  br i1 %wildcardSpecTypeModifiersMatch, label %checkWildcardSpecIsQueryWildcard, label %checkSortsEquality

checkWildcardSpecIsQueryWildcard:
  %wildcardSpecNumSuperTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardSpecType, i32 0, i32 4
  %wildcardSpecNumSubTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardSpecType, i32 0, i32 5
  %wildcardSpecNumSuperTypes = load i32* %wildcardSpecNumSuperTypesPtr
  %wildcardSpecNumSubTypes = load i32* %wildcardSpecNumSubTypesPtr
  %wildcardSpecTotalTypes = add i32 %wildcardSpecNumSuperTypes, %wildcardSpecNumSubTypes
  %wildcardSpecHasSuperTypes = icmp ne i32 %wildcardSpecNumSuperTypes, 0
  %wildcardSpecHasSubTypes = icmp ult i32 %wildcardSpecNumSuperTypes, %wildcardSpecTotalTypes
  %queryTypeIsWildcard = icmp eq i8 %realQuerySort, 12
  br i1 %queryTypeIsWildcard, label %checkWildcardSpecWildcardQuerySuperTypes, label %checkWildcardSpecSuperTypes


checkWildcardSpecWildcardQuerySuperTypes:
  %wildcardQueryType = bitcast %RTTI* %realQueryType to {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}*
  %wildcardQueryNumSuperTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardQueryType, i32 0, i32 4
  %wildcardQueryNumSubTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardQueryType, i32 0, i32 5
  %wildcardQueryNumSuperTypes = load i32* %wildcardQueryNumSuperTypesPtr
  %wildcardQueryNumSubTypes = load i32* %wildcardQueryNumSubTypesPtr
  %wildcardQueryTotalTypes = add i32 %wildcardQueryNumSuperTypes, %wildcardQueryNumSubTypes
  %wildcardQueryHasSuperTypes = icmp ne i32 %wildcardQueryNumSuperTypes, 0
  %wildcardQueryHasSubTypes = icmp ult i32 %wildcardQueryNumSuperTypes, %wildcardQueryTotalTypes
  br i1 %wildcardSpecHasSuperTypes, label %checkWildcardSpecWildcardQuerySuperTypesOuterLoop, label %checkWildcardSpecWildcardQuerySubTypes

checkWildcardSpecWildcardQuerySuperTypesOuterLoop:
  %wildcardSpecOuterLoopSuperTypeIndex = phi i32 [0, %checkWildcardSpecWildcardQuerySuperTypes], [%nextWildcardSpecOuterLoopSuperTypeIndex, %checkWildcardSpecWildcardQuerySuperTypesOuterLoopCheck]
  %wildcardSpecOuterLoopSuperTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardSpecType, i32 0, i32 6, i32 %wildcardSpecOuterLoopSuperTypeIndex
  %wildcardSpecOuterLoopSuperType = load %RTTI** %wildcardSpecOuterLoopSuperTypePtr
  br i1 %wildcardQueryHasSuperTypes, label %checkWildcardSpecWildcardQuerySuperTypesInnerLoop, label %checkSortsEquality

checkWildcardSpecWildcardQuerySuperTypesInnerLoop:
  %wildcardSpecInnerLoopSuperTypeIndex = phi i32 [0, %checkWildcardSpecWildcardQuerySuperTypesOuterLoop], [%nextWildcardSpecInnerLoopSuperTypeIndex, %checkWildcardSpecWildcardQuerySuperTypesInnerLoopCheck]
  %wildcardSpecInnerLoopSuperTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardQueryType, i32 0, i32 6, i32 %wildcardSpecInnerLoopSuperTypeIndex
  %wildcardSpecInnerLoopSuperType = load %RTTI** %wildcardSpecInnerLoopSuperTypePtr
  %wildcardSpecInnerLoopSuperTypeMatches = call i1 @plinth_check_type_matches(%RTTI* %wildcardSpecInnerLoopSuperType, %RTTI* %wildcardSpecOuterLoopSuperType, %TypeArgumentMapper* %queryMapper, %TypeArgumentMapper* %specMapper, i1 true, i1 true, i1 true)
  br i1 %wildcardSpecInnerLoopSuperTypeMatches, label %checkWildcardSpecWildcardQuerySuperTypesOuterLoopCheck, label %checkWildcardSpecWildcardQuerySuperTypeInSearchList

checkWildcardSpecWildcardQuerySuperTypeInSearchList:
  %wildcardSpecInnerLoopSuperTypeAndVFT = call {%RTTI*, %VFT*} @plinth_core_find_super_type(%RTTI* %wildcardSpecInnerLoopSuperType, %RTTI* %wildcardSpecOuterLoopSuperType, %TypeArgumentMapper* %specMapper)
  %wildcardSpecInnerLoopSuperTypeRTTI = extractvalue {%RTTI*, %VFT*} %wildcardSpecInnerLoopSuperTypeAndVFT, 0
  %wildcardSpecInnerLoopSuperTypeMatchesSearchList = icmp ne %RTTI* %wildcardSpecInnerLoopSuperTypeRTTI, null
  br i1 %wildcardSpecInnerLoopSuperTypeMatchesSearchList, label %checkWildcardSpecWildcardQuerySuperTypesOuterLoopCheck, label %checkWildcardSpecWildcardQuerySuperTypesInnerLoopCheck

checkWildcardSpecWildcardQuerySuperTypesInnerLoopCheck:
  %nextWildcardSpecInnerLoopSuperTypeIndex = add i32 %wildcardSpecInnerLoopSuperTypeIndex, 1
  %wildcardSpecInnerLoopHasNextSuperType = icmp ult i32 %nextWildcardSpecInnerLoopSuperTypeIndex, %wildcardQueryNumSuperTypes
  br i1 %wildcardSpecInnerLoopHasNextSuperType, label %checkWildcardSpecWildcardQuerySuperTypesInnerLoop, label %checkSortsEquality

checkWildcardSpecWildcardQuerySuperTypesOuterLoopCheck:
  %nextWildcardSpecOuterLoopSuperTypeIndex = add i32 %wildcardSpecOuterLoopSuperTypeIndex, 1
  %wildcardSpecOuterLoopHasNextSuperType = icmp ult i32 %nextWildcardSpecOuterLoopSuperTypeIndex, %wildcardSpecNumSuperTypes
  br i1 %wildcardSpecOuterLoopHasNextSuperType, label %checkWildcardSpecWildcardQuerySuperTypesOuterLoop, label %checkWildcardSpecWildcardQuerySubTypes

checkWildcardSpecWildcardQuerySubTypes:
  br i1 %wildcardSpecHasSubTypes, label %checkWildcardSpecWildcardQuerySubTypesOuterLoop, label %success

checkWildcardSpecWildcardQuerySubTypesOuterLoop:
  %wildcardSpecOuterLoopSubTypeIndex = phi i32 [0, %checkWildcardSpecWildcardQuerySubTypes], [%nextWildcardSpecOuterLoopSubTypeIndex, %checkWildcardSpecWildcardQuerySubTypesOuterLoopCheck]
  %wildcardSpecOuterLoopSubTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardSpecType, i32 0, i32 6, i32 %wildcardSpecOuterLoopSubTypeIndex
  %wildcardSpecOuterLoopSubType = load %RTTI** %wildcardSpecOuterLoopSubTypePtr
  br i1 %wildcardQueryHasSubTypes, label %checkWildcardSpecWildcardQuerySubTypesInnerLoop, label %checkSortsEquality

checkWildcardSpecWildcardQuerySubTypesInnerLoop:
  %wildcardSpecInnerLoopSubTypeIndex = phi i32 [0, %checkWildcardSpecWildcardQuerySubTypesOuterLoop], [%nextWildcardSpecInnerLoopSubTypeIndex, %checkWildcardSpecWildcardQuerySubTypesInnerLoopCheck]
  %wildcardSpecInnerLoopSubTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardQueryType, i32 0, i32 6, i32 %wildcardSpecInnerLoopSubTypeIndex
  %wildcardSpecInnerLoopSubType = load %RTTI** %wildcardSpecInnerLoopSubTypePtr
  %wildcardSpecInnerLoopSubTypeMatches = call i1 @plinth_check_type_matches(%RTTI* %wildcardSpecOuterLoopSubType, %RTTI* %wildcardSpecInnerLoopSubType, %TypeArgumentMapper* %specMapper, %TypeArgumentMapper* %queryMapper, i1 true, i1 true, i1 true)
  br i1 %wildcardSpecInnerLoopSubTypeMatches, label %checkWildcardSpecWildcardQuerySubTypesOuterLoopCheck, label %checkWildcardSpecWildcardQuerySubTypeInSearchList

checkWildcardSpecWildcardQuerySubTypeInSearchList:
  %wildcardSpecInnerLoopSubTypeAndVFT = call {%RTTI*, %VFT*} @plinth_core_find_super_type(%RTTI* %wildcardSpecOuterLoopSubType, %RTTI* %wildcardSpecInnerLoopSubType, %TypeArgumentMapper* %queryMapper)
  %wildcardSpecInnerLoopSubTypeRTTI = extractvalue {%RTTI*, %VFT*} %wildcardSpecInnerLoopSubTypeAndVFT, 0
  %wildcardSpecInnerLoopSubTypeMatchesSearchList = icmp ne %RTTI* %wildcardSpecInnerLoopSubTypeRTTI, null
  br i1 %wildcardSpecInnerLoopSubTypeMatchesSearchList, label %checkWildcardSpecWildcardQuerySubTypesOuterLoopCheck, label %checkWildcardSpecWildcardQuerySubTypesInnerLoopCheck

checkWildcardSpecWildcardQuerySubTypesInnerLoopCheck:
  %nextWildcardSpecInnerLoopSubTypeIndex = add i32 %wildcardSpecInnerLoopSubTypeIndex, 1
  %wildcardSpecInnerLoopHasNextSubType = icmp ult i32 %nextWildcardSpecInnerLoopSubTypeIndex, %wildcardQueryNumSubTypes
  br i1 %wildcardSpecInnerLoopHasNextSubType, label %checkWildcardSpecWildcardQuerySubTypesInnerLoop, label %checkSortsEquality

checkWildcardSpecWildcardQuerySubTypesOuterLoopCheck:
  %nextWildcardSpecOuterLoopSubTypeIndex = add i32 %wildcardSpecOuterLoopSubTypeIndex, 1
  %wildcardSpecOuterLoopHasNextSubType = icmp ult i32 %nextWildcardSpecOuterLoopSubTypeIndex, %wildcardSpecNumSubTypes
  br i1 %wildcardSpecOuterLoopHasNextSubType, label %checkWildcardSpecWildcardQuerySubTypesOuterLoop, label %success


checkWildcardSpecSuperTypes:
  br i1 %wildcardSpecHasSuperTypes, label %checkWildcardSpecSuperTypesLoop, label %checkWildcardSpecSubTypes

checkWildcardSpecSuperTypesLoop:
  %wildcardSpecSuperTypeIndex = phi i32 [0, %checkWildcardSpecSuperTypes], [%nextWildcardSpecSuperTypeIndex, %checkWildcardSpecSuperTypesLoopCheck]
  %wildcardSpecSuperTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardSpecType, i32 0, i32 6, i32 %wildcardSpecSuperTypeIndex
  %wildcardSpecSuperType = load %RTTI** %wildcardSpecSuperTypePtr
  %wildcardSpecSuperTypeMatches = call i1 @plinth_check_type_matches(%RTTI* %realQueryType, %RTTI* %wildcardSpecSuperType, %TypeArgumentMapper* %queryMapper, %TypeArgumentMapper* %specMapper, i1 true, i1 true, i1 true)
  br i1 %wildcardSpecSuperTypeMatches, label %checkWildcardSpecSuperTypesLoopCheck, label %checkWildcardSpecSuperTypeInTypeSearchList

checkWildcardSpecSuperTypeInTypeSearchList:
  %wildcardSpecSuperTypeAndVFT = call {%RTTI*, %VFT*} @plinth_core_find_super_type(%RTTI* %realQueryType, %RTTI* %wildcardSpecSuperType, %TypeArgumentMapper* %specMapper)
  %wildcardSpecSuperTypeRTTI = extractvalue {%RTTI*, %VFT*} %wildcardSpecSuperTypeAndVFT, 0
  %wildcardSpecSuperTypeMatchesSearchList = icmp ne %RTTI* %wildcardSpecSuperTypeRTTI, null
  br i1 %wildcardSpecSuperTypeMatchesSearchList, label %checkWildcardSpecSuperTypesLoopCheck, label %checkSortsEquality

checkWildcardSpecSuperTypesLoopCheck:
  %nextWildcardSpecSuperTypeIndex = add i32 %wildcardSpecSuperTypeIndex, 1
  %wildcardSpecHasMoreSuperTypes = icmp ult i32 %nextWildcardSpecSuperTypeIndex, %wildcardSpecNumSuperTypes
  br i1 %wildcardSpecHasMoreSuperTypes, label %checkWildcardSpecSuperTypesLoop, label %checkWildcardSpecSubTypes

checkWildcardSpecSubTypes:
  br i1 %wildcardSpecHasSubTypes, label %checkWildcardSpecSubTypesLoop, label %success

checkWildcardSpecSubTypesLoop:
  %wildcardSpecSubTypeIndex = phi i32 [%wildcardSpecNumSuperTypes, %checkWildcardSpecSubTypes], [%nextWildcardSpecSubTypeIndex, %checkWildcardSpecSubTypesLoopCheck]
  %wildcardSpecSubTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardSpecType, i32 0, i32 6, i32 %wildcardSpecSubTypeIndex
  %wildcardSpecSubType = load %RTTI** %wildcardSpecSubTypePtr
  %wildcardSpecSubTypeMatches = call i1 @plinth_check_type_matches(%RTTI* %wildcardSpecSubType, %RTTI* %realQueryType, %TypeArgumentMapper* %specMapper, %TypeArgumentMapper* %queryMapper, i1 true, i1 true, i1 true)
  br i1 %wildcardSpecSubTypeMatches, label %checkWildcardSpecSubTypesLoopCheck, label %checkWildcardSpecSubTypeInTypeSearchList

checkWildcardSpecSubTypeInTypeSearchList:
  %wildcardSpecSubTypeAndVFT = call {%RTTI*, %VFT*} @plinth_core_find_super_type(%RTTI* %wildcardSpecSubType, %RTTI* %realQueryType, %TypeArgumentMapper* %queryMapper)
  %wildcardSpecSubTypeRTTI = extractvalue {%RTTI*, %VFT*} %wildcardSpecSubTypeAndVFT, 0
  %wildcardSpecSubTypeMatchesSearchList = icmp ne %RTTI* %wildcardSpecSubTypeRTTI, null
  br i1 %wildcardSpecSubTypeMatchesSearchList, label %checkWildcardSpecSubTypesLoopCheck, label %checkSortsEquality

checkWildcardSpecSubTypesLoopCheck:
  %nextWildcardSpecSubTypeIndex = add i32 %wildcardSpecSubTypeIndex, 1
  %wildcardSpecHasMoreSubTypes = icmp ult i32 %nextWildcardSpecSubTypeIndex, %wildcardSpecTotalTypes
  br i1 %wildcardSpecHasMoreSubTypes, label %checkWildcardSpecSubTypesLoop, label %success



checkSortsEquality:
  %sortsEqual = icmp eq i8 %realQuerySort, %realSpecSort
  br i1 %sortsEqual, label %chooseSort, label %failure

chooseSort:
  switch i8 %realQuerySort, label %failure [i8 1,  label %object
                                            i8 2,  label %primitive
                                            i8 3,  label %array
                                            i8 4,  label %tuple
                                            i8 5,  label %function
                                            i8 6,  label %named   ; class
                                            i8 7,  label %named   ; compound
                                            i8 8,  label %named   ; interface
                                            i8 9,  label %success ; void
                                            i8 10, label %success ; null
                                            i8 11, label %typeParameter
                                            i8 12, label %wildcard]

object:
  %queryObject = bitcast %RTTI* %realQueryType to {%TypeSearchList*, i8, i1, i1}*
  %specObject = bitcast %RTTI* %realSpecType to {%TypeSearchList*, i8, i1, i1}*
  %queryObjectNullabilityPtr  = getelementptr {%TypeSearchList*, i8, i1, i1}* %queryObject, i32 0, i32 2
  %queryObjectImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1}* %queryObject, i32 0, i32 3
  %specObjectNullabilityPtr   = getelementptr {%TypeSearchList*, i8, i1, i1}* %specObject, i32 0, i32 2
  %specObjectImmutabilityPtr  = getelementptr {%TypeSearchList*, i8, i1, i1}* %specObject, i32 0, i32 3
  %queryObjectNullability = load i1* %queryObjectNullabilityPtr
  %queryObjectImmutability = load i1* %queryObjectImmutabilityPtr
  %specObjectNullability = load i1* %specObjectNullabilityPtr
  %specObjectImmutability = load i1* %specObjectImmutabilityPtr
  %realQueryObjectNullability = or i1 %queryObjectNullability, %queryForcedNullability
  %realQueryObjectImmutability = or i1 %queryObjectImmutability, %queryForcedImmutability
  %realSpecObjectNullability = or i1 %specObjectNullability, %forcedSpecNullability
  %realSpecObjectImmutability = or i1 %specObjectImmutability, %forcedSpecImmutability
  %objectEqualNullability = icmp eq i1 %realQueryObjectNullability, %realSpecObjectNullability
  %objectEqualOrIgnoredNullability = or i1 %objectEqualNullability, %ignoreNullability
  %objectEqualImmutability = icmp eq i1 %realQueryObjectImmutability, %realSpecObjectImmutability
  %objectEqualOrIgnoredImmutability = or i1 %objectEqualImmutability, %ignoreImmutability
  %objectEqualNullabilityImmutability = and i1 %objectEqualOrIgnoredNullability, %objectEqualOrIgnoredImmutability
  ret i1 %objectEqualNullabilityImmutability

primitive:
  %queryPrimitive = bitcast %RTTI* %realQueryType to {%TypeSearchList*, i8, i1, i8}*
  %specPrimitive = bitcast %RTTI* %realSpecType to {%TypeSearchList*, i8, i1, i8}*
  %queryPrimitiveNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i8}* %queryPrimitive, i32 0, i32 2
  %queryPrimitiveIdPtr = getelementptr {%TypeSearchList*, i8, i1, i8}* %queryPrimitive, i32 0, i32 3
  %specPrimitiveNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i8}* %specPrimitive, i32 0, i32 2
  %specPrimitiveIdPtr = getelementptr {%TypeSearchList*, i8, i1, i8}* %specPrimitive, i32 0, i32 3
  %queryPrimitiveNullability = load i1* %queryPrimitiveNullabilityPtr
  %queryPrimitiveId = load i8* %queryPrimitiveIdPtr
  %specPrimitiveNullability = load i1* %specPrimitiveNullabilityPtr
  %specPrimitiveId = load i8* %specPrimitiveIdPtr
  %realQueryPrimitiveNullability = or i1 %queryPrimitiveNullability, %queryForcedNullability
  %realSpecPrimitiveNullability = or i1 %specPrimitiveNullability, %forcedSpecNullability
  %primitiveEqualNullability = icmp eq i1 %realQueryPrimitiveNullability, %realSpecPrimitiveNullability
  %primitiveNullabilityMatches = or i1 %primitiveEqualNullability, %ignoreNullability
  %primitiveEqualId = icmp eq i8 %queryPrimitiveId, %specPrimitiveId
  %primitiveResult = and i1 %primitiveNullabilityMatches, %primitiveEqualId
  ret i1 %primitiveResult

array:
  %queryArray = bitcast %RTTI* %realQueryType to {%TypeSearchList*, i8, i1, i1, %RTTI*}*
  %specArray = bitcast %RTTI* %realSpecType to {%TypeSearchList*, i8, i1, i1, %RTTI*}*
  %queryArrayNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* %queryArray, i32 0, i32 2
  %queryArrayImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* %queryArray, i32 0, i32 3
  %specArrayNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* %specArray, i32 0, i32 2
  %specArrayImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* %specArray, i32 0, i32 3
  %queryArrayNullability = load i1* %queryArrayNullabilityPtr
  %queryArrayImmutability = load i1* %queryArrayImmutabilityPtr
  %specArrayNullability = load i1* %specArrayNullabilityPtr
  %specArrayImmutability = load i1* %specArrayImmutabilityPtr
  %realQueryArrayNullability = or i1 %queryArrayNullability, %queryForcedNullability
  %realQueryArrayImmutability = or i1 %queryArrayImmutability, %queryForcedImmutability
  %realSpecArrayNullability = or i1 %specArrayNullability, %forcedSpecNullability
  %realSpecArrayImmutability = or i1 %specArrayImmutability, %forcedSpecImmutability
  %arrayEqualNullability = icmp eq i1 %realQueryArrayNullability, %realSpecArrayNullability
  %arrayEqualOrIgnoredNullability = or i1 %arrayEqualNullability, %ignoreNullability
  %arrayEqualImmutability = icmp eq i1 %realQueryArrayImmutability, %realSpecArrayImmutability
  %arrayEqualOrIgnoredImmutability = or i1 %arrayEqualImmutability, %ignoreImmutability
  %arrayEqualNullabilityImmutability = and i1 %arrayEqualOrIgnoredNullability, %arrayEqualOrIgnoredImmutability
  br i1 %arrayEqualNullabilityImmutability, label %arrayContinue, label %failure
  
arrayContinue:
  %queryArrayBaseTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* %queryArray, i32 0, i32 4
  %specArrayBaseTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* %specArray, i32 0, i32 4
  %queryArrayBaseType = load %RTTI** %queryArrayBaseTypePtr
  %specArrayBaseType = load %RTTI** %specArrayBaseTypePtr
  %arrayResult = call i1 @plinth_check_type_matches(%RTTI* %queryArrayBaseType, %RTTI* %specArrayBaseType, %TypeArgumentMapper* %queryMapper, %TypeArgumentMapper* %specMapper, i1 false, i1 false, i1 false)
  ret i1 %arrayResult

tuple:
  %queryTuple = bitcast %RTTI* %realQueryType to {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}*
  %specTuple = bitcast %RTTI* %realSpecType to {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}*
  %queryTupleNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %queryTuple, i32 0, i32 2
  %queryTupleNumSubTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %queryTuple, i32 0, i32 3
  %specTupleNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %specTuple, i32 0, i32 2
  %specTupleNumSubTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %specTuple, i32 0, i32 3
  %queryTupleNullability = load i1* %queryTupleNullabilityPtr
  %queryTupleNumSubTypes = load i32* %queryTupleNumSubTypesPtr
  %specTupleNullability = load i1* %specTupleNullabilityPtr
  %specTupleNumSubTypes = load i32* %specTupleNumSubTypesPtr
  %realQueryTupleNullability = or i1 %queryTupleNullability, %queryForcedNullability
  %realSpecTupleNullability = or i1 %specTupleNullability, %forcedSpecNullability
  %tupleEqualNullability = icmp eq i1 %realQueryTupleNullability, %realSpecTupleNullability
  %tupleNullabilityMatches = or i1 %tupleEqualNullability, %ignoreNullability
  %tupleEqualNumSubTypes = icmp eq i32 %queryTupleNumSubTypes, %specTupleNumSubTypes
  %tuplePreliminaryResult = and i1 %tupleNullabilityMatches, %tupleEqualNumSubTypes
  br i1 %tuplePreliminaryResult, label %tupleLoopStart, label %failure

tupleLoopStart:
  %skipTupleLoop = icmp eq i32 %queryTupleNumSubTypes, 0
  br i1 %skipTupleLoop, label %success, label %tupleLoop

tupleLoop:
  %tupleLoopCounter = phi i32 [0, %tupleLoopStart], [%nextTupleLoopCounter, %tupleLoopCheck]
  %queryTupleSubTypePtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %queryTuple, i32 0, i32 4, i32 %tupleLoopCounter
  %specTupleSubTypePtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %specTuple, i32 0, i32 4, i32 %tupleLoopCounter
  %queryTupleSubType = load %RTTI** %queryTupleSubTypePtr
  %specTupleSubType = load %RTTI** %specTupleSubTypePtr
  %tupleElementComparison = call i1 @plinth_check_type_matches(%RTTI* %queryTupleSubType, %RTTI* %specTupleSubType, %TypeArgumentMapper* %queryMapper, %TypeArgumentMapper* %specMapper, i1 false, i1 false, i1 false)
  br i1 %tupleElementComparison, label %tupleLoopCheck, label %failure

tupleLoopCheck:
  %nextTupleLoopCounter = add i32 %tupleLoopCounter, 1
  %tupleLoopContinue = icmp ult i32 %nextTupleLoopCounter, %queryTupleNumSubTypes
  br i1 %tupleLoopContinue, label %tupleLoop, label %success

function:
  %queryFunction = bitcast %RTTI* %realQueryType to {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}*
  %specFunction = bitcast %RTTI* %realSpecType to {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}*
  %queryFunctionNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %queryFunction, i32 0, i32 2
  %queryFunctionImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %queryFunction, i32 0, i32 3
  %queryFunctionNumParametersPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %queryFunction, i32 0, i32 5
  %specFunctionNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specFunction, i32 0, i32 2
  %specFunctionImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specFunction, i32 0, i32 3
  %specFunctionNumParametersPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specFunction, i32 0, i32 5
  %queryFunctionNullability = load i1* %queryFunctionNullabilityPtr
  %queryFunctionImmutability = load i1* %queryFunctionImmutabilityPtr
  %queryFunctionNumParameters = load i32* %queryFunctionNumParametersPtr
  %specFunctionNullability = load i1* %specFunctionNullabilityPtr
  %specFunctionImmutability = load i1* %specFunctionImmutabilityPtr
  %specFunctionNumParameters = load i32* %specFunctionNumParametersPtr
  %realQueryFunctionNullability = or i1 %queryFunctionNullability, %queryForcedNullability
  %realSpecFunctionNullability = or i1 %specFunctionNullability, %forcedSpecNullability
  ; NOTE: function immutability cannot be forced, since it is not a form of data-immutability
  %functionEqualNullability = icmp eq i1 %realQueryFunctionNullability, %realSpecFunctionNullability
  %functionNullabilityMatches = or i1 %functionEqualNullability, %ignoreNullability
  %functionEqualImmutability = icmp eq i1 %queryFunctionImmutability, %specFunctionImmutability
  %functionEqualNumParameters = icmp eq i32 %queryFunctionNumParameters, %specFunctionNumParameters
  %functionNullabilityImmutability = and i1 %functionNullabilityMatches, %functionEqualImmutability
  %functionPreliminaryResult = and i1 %functionNullabilityImmutability, %functionEqualNumParameters
  br i1 %functionPreliminaryResult, label %functionReturnTypeCheck, label %failure

functionReturnTypeCheck:
  %queryFunctionReturnTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %queryFunction, i32 0, i32 4
  %specFunctionReturnTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specFunction, i32 0, i32 4
  %queryFunctionReturnType = load %RTTI** %queryFunctionReturnTypePtr
  %specFunctionReturnType = load %RTTI** %specFunctionReturnTypePtr
  %functionEqualReturnTypes = call i1 @plinth_check_type_matches(%RTTI* %queryFunctionReturnType, %RTTI* %specFunctionReturnType, %TypeArgumentMapper* %queryMapper, %TypeArgumentMapper* %specMapper, i1 false, i1 false, i1 false)
  br i1 %functionEqualReturnTypes, label %functionLoopStart, label %failure

functionLoopStart:
  %skipFunctionLoop = icmp eq i32 %queryFunctionNumParameters, 0
  br i1 %skipFunctionLoop, label %success, label %functionLoop

functionLoop:
  %functionLoopCounter = phi i32 [0, %functionLoopStart], [%nextFunctionLoopCounter, %functionLoopCheck]
  %queryFunctionParameterTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %queryFunction, i32 0, i32 6, i32 %functionLoopCounter
  %specFunctionParameterTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specFunction, i32 0, i32 6, i32 %functionLoopCounter
  %queryFunctionParameterType = load %RTTI** %queryFunctionParameterTypePtr
  %specFunctionParameterType = load %RTTI** %specFunctionParameterTypePtr
  %functionElementComparison = call i1 @plinth_check_type_matches(%RTTI* %queryFunctionParameterType, %RTTI* %specFunctionParameterType, %TypeArgumentMapper* %queryMapper, %TypeArgumentMapper* %specMapper, i1 false, i1 false, i1 false)
  br i1 %functionElementComparison, label %functionLoopCheck, label %failure

functionLoopCheck:
  %nextFunctionLoopCounter = add i32 %functionLoopCounter, 1
  %functionLoopContinue = icmp ult i32 %nextFunctionLoopCounter, %queryFunctionNumParameters
  br i1 %functionLoopContinue, label %functionLoop, label %success

named:
  %queryNamed = bitcast %RTTI* %realQueryType to {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}*
  %specNamed = bitcast %RTTI* %realSpecType to {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}*
  %queryNamedNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %queryNamed, i32 0, i32 2
  %queryNamedImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %queryNamed, i32 0, i32 3
  %queryNamedNumArgumentsPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %queryNamed, i32 0, i32 5, i32 0
  %specNamedNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specNamed, i32 0, i32 2
  %specNamedImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specNamed, i32 0, i32 3
  %specNamedNumArgumentsPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specNamed, i32 0, i32 5, i32 0
  %queryNamedNullability = load i1* %queryNamedNullabilityPtr
  %queryNamedImmutability = load i1* %queryNamedImmutabilityPtr
  %queryNamedNumArguments = load i32* %queryNamedNumArgumentsPtr
  %specNamedNullability = load i1* %specNamedNullabilityPtr
  %specNamedImmutability = load i1* %specNamedImmutabilityPtr
  %specNamedNumArguments = load i32* %specNamedNumArgumentsPtr
  %realQueryNamedNullability = or i1 %queryNamedNullability, %queryForcedNullability
  %realQueryNamedImmutability = or i1 %queryNamedImmutability, %queryForcedImmutability
  %realSpecNamedNullability = or i1 %specNamedNullability, %forcedSpecNullability
  %realSpecNamedImmutability = or i1 %specNamedImmutability, %forcedSpecImmutability
  %namedEqualNullability = icmp eq i1 %realQueryNamedNullability, %realSpecNamedNullability
  %namedEqualOrIgnoredNullability = or i1 %namedEqualNullability, %ignoreNullability
  %namedEqualImmutability = icmp eq i1 %realQueryNamedImmutability, %realSpecNamedImmutability
  %namedEqualOrIgnoredImmutability = or i1 %namedEqualImmutability, %ignoreImmutability
  %namedNullabilityImmutability = and i1 %namedEqualOrIgnoredNullability, %namedEqualOrIgnoredImmutability
  %namedEqualNumArguments = icmp eq i32 %queryNamedNumArguments, %specNamedNumArguments
  %namedPreliminaryResult = and i1 %namedNullabilityImmutability, %namedEqualNumArguments
  br i1 %namedPreliminaryResult, label %nameLengthCheck, label %failure

nameLengthCheck:
  %queryNamedNamePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %queryNamed, i32 0, i32 4
  %specNamedNamePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specNamed, i32 0, i32 4
  %queryNamedName = load %RawString** %queryNamedNamePtr
  %specNamedName = load %RawString** %specNamedNamePtr
  %queryNameLengthPtr = getelementptr %RawString* %queryNamedName, i32 0, i32 2
  %specNameLengthPtr = getelementptr %RawString* %specNamedName, i32 0, i32 2
  %queryNameLength = load i32* %queryNameLengthPtr
  %specNameLength = load i32* %specNameLengthPtr
  %equalNameLengths = icmp eq i32 %queryNameLength, %specNameLength
  br i1 %equalNameLengths, label %nameCheck, label %failure

nameCheck:
  %queryNameBytes = getelementptr %RawString* %queryNamedName, i32 0, i32 5, i32 0
  %specNameBytes = getelementptr %RawString* %specNamedName, i32 0, i32 5, i32 0
  %nameCompareResult = call i32 @strncmp(i8* %queryNameBytes, i8* %specNameBytes, i32 %queryNameLength)
  %namesMatch = icmp eq i32 %nameCompareResult, 0
  br i1 %namesMatch, label %typeArgumentLoopStart, label %failure

typeArgumentLoopStart:
  %skipTypeArgumentLoop = icmp eq i32 %queryNamedNumArguments, 0
  br i1 %skipTypeArgumentLoop, label %success, label %typeArgumentLoop

typeArgumentLoop:
  %typeArgumentLoopCounter = phi i32 [0, %typeArgumentLoopStart], [%nextTypeArgumentLoopCounter, %typeArgumentLoopCheck]
  %queryTypeArgumentPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %queryNamed, i32 0, i32 5, i32 1, i32 %typeArgumentLoopCounter
  %specTypeArgumentPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specNamed, i32 0, i32 5, i32 1, i32 %typeArgumentLoopCounter
  %queryTypeArgument = load %RTTI** %queryTypeArgumentPtr
  %specTypeArgument = load %RTTI** %specTypeArgumentPtr
  %typeArgumentsEquivalent = call i1 @plinth_check_type_matches(%RTTI* %queryTypeArgument, %RTTI* %specTypeArgument, %TypeArgumentMapper* %queryMapper, %TypeArgumentMapper* %specMapper, i1 false, i1 false, i1 %looselyMatchWildcards)
  br i1 %typeArgumentsEquivalent, label %typeArgumentLoopCheck, label %failure

typeArgumentLoopCheck:
  %nextTypeArgumentLoopCounter = add i32 %typeArgumentLoopCounter, 1
  %typeArgumentLoopContinue = icmp ult i32 %nextTypeArgumentLoopCounter, %queryNamedNumArguments
  br i1 %typeArgumentLoopContinue, label %typeArgumentLoop, label %success

typeParameter:
  %queryTypeParameter = bitcast %RTTI* %realQueryType to {%TypeSearchList*, i8, i1, i1, i32}*
  %specTypeParameter = bitcast %RTTI* %realSpecType to {%TypeSearchList*, i8, i1, i1, i32}*
  %queryTypeParameterNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %queryTypeParameter, i32 0, i32 2
  %queryTypeParameterImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %queryTypeParameter, i32 0, i32 3
  %queryTypeParameterIndexPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %queryTypeParameter, i32 0, i32 4
  %specTypeParameterNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %specTypeParameter, i32 0, i32 2
  %specTypeParameterImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %specTypeParameter, i32 0, i32 3
  %specTypeParameterIndexPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %specTypeParameter, i32 0, i32 4
  %queryTypeParameterNullability = load i1* %queryTypeParameterNullabilityPtr
  %queryTypeParameterImmutability = load i1* %queryTypeParameterImmutabilityPtr
  %queryTypeParameterIndex = load i32* %queryTypeParameterIndexPtr
  %specTypeParameterNullability = load i1* %specTypeParameterNullabilityPtr
  %specTypeParameterImmutability = load i1* %specTypeParameterImmutabilityPtr
  %specTypeParameterIndex = load i32* %specTypeParameterIndexPtr
  %realQueryTypeParameterNullability = or i1 %queryTypeParameterNullability, %queryForcedNullability
  %realQueryTypeParameterImmutability = or i1 %queryTypeParameterImmutability, %queryForcedImmutability
  %realSpecTypeParameterNullability = or i1 %specTypeParameterNullability, %forcedSpecNullability
  %realSpecTypeParameterImmutability = or i1 %specTypeParameterImmutability, %forcedSpecImmutability
  %typeParameterEqualNullability = icmp eq i1 %realQueryTypeParameterNullability, %realSpecTypeParameterNullability
  %typeParameterEqualOrIgnoredNullability = or i1 %typeParameterEqualNullability, %ignoreNullability
  %typeParameterEqualImmutability = icmp eq i1 %realQueryTypeParameterImmutability, %realSpecTypeParameterImmutability
  %typeParameterEqualOrIgnoredImmutability = or i1 %typeParameterEqualImmutability, %ignoreImmutability
  %typeParameterEqualIndex = icmp eq i32 %queryTypeParameterIndex, %specTypeParameterIndex
  %typeParameterNullibilityImmutability = and i1 %typeParameterEqualOrIgnoredNullability, %typeParameterEqualOrIgnoredImmutability
  %typeParameterResult = and i1 %typeParameterNullibilityImmutability, %typeParameterEqualIndex
  ret i1 %typeParameterResult

wildcard:
  %queryWildcard = bitcast %RTTI* %realQueryType to {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}*
  %specWildcard = bitcast %RTTI* %realSpecType to {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}*
  %queryWildcardNullabilityPtr   = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %queryWildcard, i32 0, i32 2
  %queryWildcardImmutabilityPtr  = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %queryWildcard, i32 0, i32 3
  %queryWildcardNumSuperTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %queryWildcard, i32 0, i32 4
  %queryWildcardNumSubTypesPtr   = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %queryWildcard, i32 0, i32 5
  %specWildcardNullabilityPtr   = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specWildcard, i32 0, i32 2
  %specWildcardImmutabilityPtr  = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specWildcard, i32 0, i32 3
  %specWildcardNumSuperTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specWildcard, i32 0, i32 4
  %specWildcardNumSubTypesPtr   = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specWildcard, i32 0, i32 5
  %queryWildcardNullability   = load i1* %queryWildcardNullabilityPtr
  %queryWildcardImmutability  = load i1* %queryWildcardImmutabilityPtr
  %queryWildcardNumSuperTypes = load i32* %queryWildcardNumSuperTypesPtr
  %queryWildcardNumSubTypes   = load i32* %queryWildcardNumSubTypesPtr
  %specWildcardNullability   = load i1* %specWildcardNullabilityPtr
  %specWildcardImmutability  = load i1* %specWildcardImmutabilityPtr
  %specWildcardNumSuperTypes = load i32* %specWildcardNumSuperTypesPtr
  %specWildcardNumSubTypes   = load i32* %specWildcardNumSubTypesPtr
  %realQueryWildcardNullability  = or i1 %queryWildcardNullability, %queryForcedNullability
  %realQueryWildcardImmutability = or i1 %queryWildcardImmutability, %queryForcedImmutability
  %realSpecWildcardNullability  = or i1 %specWildcardNullability, %forcedSpecNullability
  %realSpecWildcardImmutability = or i1 %specWildcardImmutability, %forcedSpecImmutability
  %wildcardEqualNullability  = icmp eq i1 %realQueryWildcardNullability, %realSpecWildcardNullability
  %wildcardEqualOrIgnoredNullability = or i1 %wildcardEqualNullability, %ignoreNullability
  %wildcardEqualImmutability = icmp eq i1 %realQueryWildcardImmutability, %realSpecWildcardImmutability
  %wildcardEqualOrIgnoredImmutability = or i1 %wildcardEqualImmutability, %ignoreImmutability
  %wildcardNullabilityImmutability = and i1 %wildcardEqualOrIgnoredNullability, %wildcardEqualOrIgnoredImmutability
  %wildcardNumSuperTypesMatch = icmp eq i32 %queryWildcardNumSuperTypes, %specWildcardNumSuperTypes
  %wildcardNumSubTypesMatch = icmp eq i32 %queryWildcardNumSubTypes, %specWildcardNumSubTypes
  %wildcardNumTypesMatch = and i1 %wildcardNumSuperTypesMatch, %wildcardNumSubTypesMatch
  %wildcardHeaderMatches = and i1 %wildcardNullabilityImmutability, %wildcardNumTypesMatch
  br i1 %wildcardHeaderMatches, label %wildcardCheckTypes, label %failure

wildcardCheckTypes:
  %numWildcardRTTIBlocks = add i32 %queryWildcardNumSuperTypes, %queryWildcardNumSubTypes
  %wildcardSkipCheckLoop = icmp eq i32 %numWildcardRTTIBlocks, 0
  br i1 %wildcardSkipCheckLoop, label %success, label %wildcardCheckLoop

wildcardCheckLoop:
  %wildcardIndex = phi i32 [0, %wildcardCheckTypes], [%nextWildcardIndex, %wildcardCheckLoopCheck]
  %queryWildcardRTTIPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %queryWildcard, i32 0, i32 6, i32 %wildcardIndex
  %specWildcardRTTIPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specWildcard, i32 0, i32 6, i32 %wildcardIndex
  %queryWildcardRTTI = load %RTTI** %queryWildcardRTTIPtr
  %specWildcardRTTI = load %RTTI** %specWildcardRTTIPtr
  %wildcardTypesEquivalent = call i1 @plinth_check_type_matches(%RTTI* %queryWildcardRTTI, %RTTI* %specWildcardRTTI, %TypeArgumentMapper* %queryMapper, %TypeArgumentMapper* %specMapper, i1 false, i1 false, i1 %looselyMatchWildcards)
  br i1 %wildcardTypesEquivalent, label %wildcardCheckLoopCheck, label %failure

wildcardCheckLoopCheck:
  %nextWildcardIndex = add i32 %wildcardIndex, 1
  %wildcardLoopContinue = icmp ult i32 %nextWildcardIndex, %numWildcardRTTIBlocks
  br i1 %wildcardLoopContinue, label %wildcardCheckLoop, label %success

failure:
  ret i1 false

success:
  ret i1 true
}





define %RTTI* @plinth_get_specialised_type_info(%RTTI* %type, %TypeArgumentMapper* %mapper) {
entry:
  ; inside this function, we need to store a TypeSearchList inside any new RTTI blocks we create
  ; however, it would be impossible to create a new TypeSearchList with the correct VFTs inside it
  ; without knowing the exact type we are generating in advance, and if we knew that, we wouldn't
  ; be using the runtime specialisation function
  ; luckily, this function is not intended to be used to generate RTTI that virtual functions can
  ; be looked up inside, just RTTI that we can check things against.
  ; so we can just copy the original TypeSearchList to any new RTTI blocks we generate here
  %oldTypeSearchListPtr = getelementptr %RTTI* %type, i32 0, i32 0
  %oldTypeSearchList = load %TypeSearchList** %oldTypeSearchListPtr
  %sortPtr = getelementptr %RTTI* %type, i32 0, i32 1
  %sort = load i8* %sortPtr
  switch i8 %sort, label %noChange [i8  1, label %noChange       ; object
                                    i8  2, label %noChange       ; primitive
                                    i8  3, label %array          ; array
                                    i8  4, label %tuple          ; tuple
                                    i8  5, label %function       ; function
                                    i8  6, label %named          ; class
                                    i8  7, label %named          ; compound
                                    i8  8, label %named          ; interface
                                    i8  9, label %noChange       ; void
                                    i8 10, label %noChange       ; null
                                    i8 11, label %typeParameter  ; type parameter
                                    i8 12, label %wildcard]      ; wildcard

noChange:
  ret %RTTI* %type

array:
  %arrayRTTI = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1, i1, %RTTI*}*
  %arrayBaseRTTIPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* %arrayRTTI, i32 0, i32 4
  %arrayBaseRTTI = load %RTTI** %arrayBaseRTTIPtr
  %specialisedArrayBaseRTTI = call %RTTI* @plinth_get_specialised_type_info(%RTTI* %arrayBaseRTTI, %TypeArgumentMapper* %mapper)
  %specialisedArrayBaseDiffers = icmp ne %RTTI* %arrayBaseRTTI, %specialisedArrayBaseRTTI
  br i1 %specialisedArrayBaseDiffers, label %createSpecialisedArray, label %noChange

createSpecialisedArray:
  %arrayRTTISize = ptrtoint {%TypeSearchList*, i8, i1, i1, %RTTI*}* getelementptr ({%TypeSearchList*, i8, i1, i1, %RTTI*}* null, i32 1) to i32
  %arrayAlloc = call i8* @calloc(i32 %arrayRTTISize, i32 1)
  %specialisedArray = bitcast i8* %arrayAlloc to {%TypeSearchList*, i8, i1, i1, %RTTI*}*
  %arrayTypeSearchListPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* %specialisedArray, i32 0, i32 0
  store %TypeSearchList* %oldTypeSearchList, %TypeSearchList** %arrayTypeSearchListPtr
  %arraySortIdPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* %specialisedArray, i32 0, i32 1
  store i8 3, i8* %arraySortIdPtr
  %oldArrayNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* %arrayRTTI, i32 0, i32 2
  %arrayNullability = load i1* %oldArrayNullabilityPtr
  %arrayNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* %specialisedArray, i32 0, i32 2
  store i1 %arrayNullability, i1* %arrayNullabilityPtr
  %oldArrayImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* %arrayRTTI, i32 0, i32 3
  %arrayImmutability = load i1* %oldArrayImmutabilityPtr
  %arrayImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* %specialisedArray, i32 0, i32 3
  store i1 %arrayImmutability, i1* %arrayImmutabilityPtr
  %newArrayBaseRTTIPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* %specialisedArray, i32 0, i32 4
  store %RTTI* %specialisedArrayBaseRTTI, %RTTI** %newArrayBaseRTTIPtr
  %castedSpecialisedArray = bitcast {%TypeSearchList*, i8, i1, i1, %RTTI*}* %specialisedArray to %RTTI*
  ret %RTTI* %castedSpecialisedArray

tuple:
  %tupleRTTI = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}*
  ; try not to waste any allocations here - pseudo code for processing tuples is:
  ; for (uint i = 0; i < tupleRTTI.num_sub_types; ++i)
  ; {
  ;   RTTI* existing = tupleRTTI.subTypes[i]
  ;   RTTI* specialised = plinth_get_specialised_type_info(existing)
  ;   if (existing != specialised)
  ;   {
  ;     RTTI* specialisedTuple = allocate_tuple_RTTI(tupleRTTI.num_sub_types)
  ;     specialisedTuple.sortId = 4 // (tuple)
  ;     specialisedTuple.nullability = tupleRTTI.nullability
  ;     specialisedTuple.num_sub_types = tupleRTTI.num_sub_types
  ;     specialisedTuple.subTypes[i] = specialised
  ;     for (uint j = 0; j < i; ++j)
  ;     {
  ;       specialisedTuple.subTypes[j] = tupleRTTI.subTypes[j]
  ;     }
  ;     for (uint k = i + 1; k < tupleRTTI.num_sub_types; ++k)
  ;     {
  ;       specialisedTuple.subTypes[k] = plinth_get_specialised_type_info(tupleRTTI.subTypes[k])
  ;     }
  ;     return specialisedTuple
  ;   }
  ; }
  ; return tupleRTTI
  %tupleNumElementsPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %tupleRTTI, i32 0, i32 3
  %tupleNumElements = load i32* %tupleNumElementsPtr
  %tupleHasElements = icmp ne i32 0, %tupleNumElements
  br i1 %tupleHasElements, label %tupleCheckLoop, label %noChange

tupleCheckLoop:
  %tupleCheckLoopIndex = phi i32 [0, %tuple], [%nextTupleCheckLoopIndex, %tupleCheckLoopCheck]
  %tupleSubTypeRTTIPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %tupleRTTI, i32 0, i32 4, i32 %tupleCheckLoopIndex
  %tupleSubTypeRTTI = load %RTTI** %tupleSubTypeRTTIPtr
  %specialisedTupleSubTypeRTTI = call %RTTI* @plinth_get_specialised_type_info(%RTTI* %tupleSubTypeRTTI, %TypeArgumentMapper* %mapper)
  %tupleSubTypeDiffers = icmp ne %RTTI* %tupleSubTypeRTTI, %specialisedTupleSubTypeRTTI
  %nextTupleCheckLoopIndex = add i32 %tupleCheckLoopIndex, 1
  br i1 %tupleSubTypeDiffers, label %createSpecialisedTuple, label %tupleCheckLoopCheck

tupleCheckLoopCheck:
  %tupleCheckLoopHasNext = icmp ult i32 %nextTupleCheckLoopIndex, %tupleNumElements
  br i1 %tupleCheckLoopHasNext, label %tupleCheckLoop, label %noChange

createSpecialisedTuple:
  %tupleRTTISizePtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* null, i32 0, i32 4, i32 %tupleNumElements
  %tupleRTTISize = ptrtoint %RTTI** %tupleRTTISizePtr to i32
  %tupleAlloc = call i8* @calloc(i32 %tupleRTTISize, i32 1)
  %specialisedTuple = bitcast i8* %tupleAlloc to {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}*
  %tupleTypeSearchListPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %specialisedTuple, i32 0, i32 0
  store %TypeSearchList* %oldTypeSearchList, %TypeSearchList** %tupleTypeSearchListPtr
  %tupleSortIdPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %specialisedTuple, i32 0, i32 1
  store i8 4, i8* %tupleSortIdPtr
  %oldTupleNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %tupleRTTI, i32 0, i32 2
  %tupleNullability = load i1* %oldTupleNullabilityPtr
  %tupleNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %specialisedTuple, i32 0, i32 2
  store i1 %tupleNullability, i1* %tupleNullabilityPtr
  %newTupleNumElementsPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %specialisedTuple, i32 0, i32 3
  store i32 %tupleNumElements, i32* %newTupleNumElementsPtr
  %newTupleElementPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %specialisedTuple, i32 0, i32 4, i32 %tupleCheckLoopIndex
  store %RTTI* %specialisedTupleSubTypeRTTI, %RTTI** %newTupleElementPtr
  %tupleHasPreviousElements = icmp ult i32 0, %tupleCheckLoopIndex
  br i1 %tupleHasPreviousElements, label %tupleCopyPreviousElementsLoop, label %tupleSpecialiseRemainingSubTypes

tupleCopyPreviousElementsLoop:
  %tupleCopyLoopIndex = phi i32 [0, %createSpecialisedTuple], [%tupleCopyElementsNextIndex, %tupleCopyPreviousElementsLoop]
  %oldTupleElementPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %tupleRTTI, i32 0, i32 4, i32 %tupleCopyLoopIndex
  %copiedTupleElement = load %RTTI** %oldTupleElementPtr
  %copiedTupleElementPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %specialisedTuple, i32 0, i32 4, i32 %tupleCopyLoopIndex
  store %RTTI* %copiedTupleElement, %RTTI** %copiedTupleElementPtr
  %tupleCopyElementsNextIndex = add i32 %tupleCopyLoopIndex, 1
  %tupleCopyHasNextElement = icmp ult i32 %tupleCopyElementsNextIndex, %tupleCheckLoopIndex
  br i1 %tupleCopyHasNextElement, label %tupleCopyPreviousElementsLoop, label %tupleSpecialiseRemainingSubTypes

tupleSpecialiseRemainingSubTypes:
  %tupleHasElementsRemaining = icmp ult i32 %nextTupleCheckLoopIndex, %tupleNumElements
  br i1 %tupleHasElementsRemaining, label %tupleSpecialiseSubTypesLoop, label %returnSpecialisedTuple

tupleSpecialiseSubTypesLoop:
  %tupleSpecialiseLoopIndex = phi i32 [%nextTupleCheckLoopIndex, %tupleSpecialiseRemainingSubTypes], [%nextTupleSpecialiseLoopIndex, %tupleSpecialiseSubTypesLoop]
  %oldTupleSubTypePtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %tupleRTTI, i32 0, i32 4, i32 %tupleSpecialiseLoopIndex
  %oldTupleSubType = load %RTTI** %oldTupleSubTypePtr
  %newTupleSubType = call %RTTI* @plinth_get_specialised_type_info(%RTTI* %oldTupleSubType, %TypeArgumentMapper* %mapper)
  %newTupleSubTypePtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %specialisedTuple, i32 0, i32 4, i32 %tupleSpecialiseLoopIndex
  store %RTTI* %newTupleSubType, %RTTI** %newTupleSubTypePtr
  %nextTupleSpecialiseLoopIndex = add i32 %tupleSpecialiseLoopIndex, 1
  %tupleSpecialiseLoopHasNextElement = icmp ult i32 %nextTupleSpecialiseLoopIndex, %tupleNumElements
  br i1 %tupleSpecialiseLoopHasNextElement, label %tupleSpecialiseSubTypesLoop, label %returnSpecialisedTuple

returnSpecialisedTuple:
  %castedSpecialisedTuple = bitcast {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %specialisedTuple to %RTTI*
  ret %RTTI* %castedSpecialisedTuple

function:
  %functionRTTI = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}*
  ; try not to waste any allocations here - pseudo code for processing functions is:
  ; RTTI* returnType = functionRTTI.returnType
  ; RTTI* specialisedReturnType = @plinth_get_specialised_type_info(returnType)
  ; RTTI* firstDifferingParam = null
  ; uint firstDifferingIndex = 0
  ; if (returnType == specialisedReturnType)
  ; {
  ;   for (uint i = 0; i < functionRTTI.num_params; ++i)
  ;   {
  ;     RTTI* paramType = functionRTTI.paramTypes[i]
  ;     RTTI* specialisedParamType = @plinth_get_specialised_type_info(paramType)
  ;     if (paramType != specialisedParamType)
  ;     {
  ;       firstDifferingParam = specialisedParamType
  ;       firstDifferingIndex = i
  ;       break
  ;     }
  ;   }
  ;   if (firstDifferingParam == null)
  ;   {
  ;     return functionRTTI
  ;   }
  ; }
  ; RTTI* specialisedFunction = allocate_function_rtti(functionRTTI.num_params)
  ; specialisedFunction.sortId = 5 // function
  ; specialisedFunction.nullability = functionRTTI.nullability
  ; specialisedFunction.immutability = functionRTTI.immutability
  ; specialisedFunction.returnType = specialisedReturnType
  ; specialisedFunction.num_params = functionRTTI.num_params
  ; uint firstSpecialisationIndex = 0
  ; if (firstDifferingParam != null)
  ; {
  ;   specialisedFunction.paramTypes[firstDifferingIndex] = firstDifferingParam
  ;   for (uint j = 0; j < firstDifferingIndex; ++j)
  ;   {
  ;     specialisedFunction.paramTypes[j] = functionRTTI.paramTypes[j]
  ;   }
  ;   firstSpecialisationIndex = firstDifferingIndex + 1
  ; }
  ; for (uint k = firstSpecialisationIndex; k < functionRTTI.num_params; ++k)
  ; {
  ;   specialisedFunction.paramTypes[k] = @plinth_get_specialised_type_info(functionRTTI.paramTypes[k])
  ; }
  ; return specialisedFunction
  %functionReturnTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionRTTI, i32 0, i32 4
  %functionReturnType = load %RTTI** %functionReturnTypePtr
  %specialisedFunctionReturnType = call %RTTI* @plinth_get_specialised_type_info(%RTTI* %functionReturnType, %TypeArgumentMapper* %mapper)
  %functionNumParamsPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionRTTI, i32 0, i32 5
  %functionNumParams = load i32* %functionNumParamsPtr
  %functionReturnTypeDiffers = icmp ne %RTTI* %functionReturnType, %specialisedFunctionReturnType
  br i1 %functionReturnTypeDiffers, label %createSpecialisedFunction, label %checkFunctionParamTypes

checkFunctionParamTypes:
  %functionHasParams = icmp ne i32 0, %functionNumParams
  br i1 %functionHasParams, label %checkFunctionParamTypesLoop, label %noChange

checkFunctionParamTypesLoop:
  %checkFunctionParamTypesIndex = phi i32 [0, %checkFunctionParamTypes], [%nextCheckFunctionParamTypesIndex, %checkFunctionParamTypesLoopCheck]
  %functionParamTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionRTTI, i32 0, i32 6, i32 %checkFunctionParamTypesIndex
  %functionParamType = load %RTTI** %functionParamTypePtr
  %specialisedFunctionParamType = call %RTTI* @plinth_get_specialised_type_info(%RTTI* %functionParamType, %TypeArgumentMapper* %mapper)
  %functionParamTypeDiffers = icmp ne %RTTI* %functionParamType, %specialisedFunctionParamType
  br i1 %functionParamTypeDiffers, label %createSpecialisedFunction, label %checkFunctionParamTypesLoopCheck

checkFunctionParamTypesLoopCheck:
  %nextCheckFunctionParamTypesIndex = add i32 %checkFunctionParamTypesIndex, 1
  %checkFunctionParamTypesHasNext = icmp ult i32 %nextCheckFunctionParamTypesIndex, %functionNumParams
  br i1 %checkFunctionParamTypesHasNext, label %checkFunctionParamTypesLoop, label %noChange

createSpecialisedFunction:
  %firstDifferingFunctionParam = phi %RTTI* [null, %function], [%specialisedFunctionParamType, %checkFunctionParamTypesLoop]
  %firstDifferingFunctionParamIndex = phi i32 [0, %function], [%checkFunctionParamTypesIndex, %checkFunctionParamTypesLoop]
  %functionRTTISizePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* null, i32 0, i32 6, i32 %functionNumParams
  %functionRTTISize = ptrtoint %RTTI** %functionRTTISizePtr to i32
  %functionAlloc = call i8* @calloc(i32 %functionRTTISize, i32 1)
  %specialisedFunction = bitcast i8* %functionAlloc to {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}*
  %functionTypeSearchListPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specialisedFunction, i32 0, i32 0
  store %TypeSearchList* %oldTypeSearchList, %TypeSearchList** %functionTypeSearchListPtr
  %functionSortIdPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specialisedFunction, i32 0, i32 1
  store i8 5, i8* %functionSortIdPtr
  %oldFunctionNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionRTTI, i32 0, i32 2
  %functionNullability = load i1* %oldFunctionNullabilityPtr
  %newFunctionNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specialisedFunction, i32 0, i32 2
  store i1 %functionNullability, i1* %newFunctionNullabilityPtr
  %oldFunctionImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionRTTI, i32 0, i32 3
  %functionImmutability = load i1* %oldFunctionImmutabilityPtr
  %newFunctionImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specialisedFunction, i32 0, i32 3
  store i1 %functionImmutability, i1* %newFunctionImmutabilityPtr
  %newFunctionReturnTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specialisedFunction, i32 0, i32 4
  store %RTTI* %specialisedFunctionReturnType, %RTTI** %newFunctionReturnTypePtr
  %newFunctionNumParamsPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specialisedFunction, i32 0, i32 5
  store i32 %functionNumParams, i32* %newFunctionNumParamsPtr
  %hasFirstDifferingFunctionParam = icmp ne %RTTI* %firstDifferingFunctionParam, null
  br i1 %hasFirstDifferingFunctionParam, label %copyFirstDifferingFunctionParam, label %specialiseFunctionParams

copyFirstDifferingFunctionParam:
  %firstDifferingFunctionParamPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specialisedFunction, i32 0, i32 6, i32 %firstDifferingFunctionParamIndex
  store %RTTI* %firstDifferingFunctionParam, %RTTI** %firstDifferingFunctionParamPtr
  %functionHasPreviousParams = icmp ult i32 0, %firstDifferingFunctionParamIndex
  %afterFirstDifferingFunctionParamIndex = add i32 %firstDifferingFunctionParamIndex, 1
  br i1 %functionHasPreviousParams, label %copyFunctionPreviousParams, label %specialiseFunctionParams

copyFunctionPreviousParams:
  %functionCopyPreviousParamsIndex = phi i32 [0, %copyFirstDifferingFunctionParam], [%nextFunctionCopyPreviousParamsIndex, %copyFunctionPreviousParams]
  %oldFunctionParamPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionRTTI, i32 0, i32 6, i32 %functionCopyPreviousParamsIndex
  %copiedFunctionParam = load %RTTI** %oldFunctionParamPtr
  %copiedFunctionParamPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specialisedFunction, i32 0, i32 6, i32 %functionCopyPreviousParamsIndex
  store %RTTI* %copiedFunctionParam, %RTTI** %copiedFunctionParamPtr
  %nextFunctionCopyPreviousParamsIndex = add i32 %functionCopyPreviousParamsIndex, 1
  %functionCopyHasNextParam = icmp ult i32 %nextFunctionCopyPreviousParamsIndex, %firstDifferingFunctionParamIndex
  br i1 %functionCopyHasNextParam, label %copyFunctionPreviousParams, label %specialiseFunctionParams

specialiseFunctionParams:
  %specialiseFunctionParamsStartIndex = phi i32 [0, %createSpecialisedFunction], [%afterFirstDifferingFunctionParamIndex, %copyFirstDifferingFunctionParam], [%afterFirstDifferingFunctionParamIndex, %copyFunctionPreviousParams]
  %functionHasRemainingParams = icmp ult i32 %specialiseFunctionParamsStartIndex, %functionNumParams
  br i1 %functionHasRemainingParams, label %specialiseFunctionParamsLoop, label %returnSpecialisedFunction

specialiseFunctionParamsLoop:
  %specialiseFunctionParamsIndex = phi i32 [%specialiseFunctionParamsStartIndex, %specialiseFunctionParams], [%nextSpecialiseFunctionParamsIndex, %specialiseFunctionParamsLoop]
  %nonSpecialisedFunctionParamPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionRTTI, i32 0, i32 6, i32 %specialiseFunctionParamsIndex
  %nonSpecialisedFunctionParam = load %RTTI** %nonSpecialisedFunctionParamPtr
  %specialisedFunctionParam = call %RTTI* @plinth_get_specialised_type_info(%RTTI* %nonSpecialisedFunctionParam, %TypeArgumentMapper* %mapper)
  %newFunctionParamPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specialisedFunction, i32 0, i32 6, i32 %specialiseFunctionParamsIndex
  store %RTTI* %specialisedFunctionParam, %RTTI** %newFunctionParamPtr
  %nextSpecialiseFunctionParamsIndex = add i32 %specialiseFunctionParamsIndex, 1
  %specialiseFunctionParamsHasNext = icmp ult i32 %nextSpecialiseFunctionParamsIndex, %functionNumParams
  br i1 %specialiseFunctionParamsHasNext, label %specialiseFunctionParamsLoop, label %returnSpecialisedFunction

returnSpecialisedFunction:
  %castedSpecialisedFunction = bitcast {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %specialisedFunction to %RTTI*
  ret %RTTI* %castedSpecialisedFunction

named:
  %namedRTTI = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}*
  ; try not to waste any allocations here - we use an algorithm extremely similar to the one used for tuples above
  %namedTypeNumArgumentsPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %namedRTTI, i32 0, i32 5, i32 0
  %namedTypeNumArguments = load i32* %namedTypeNumArgumentsPtr
  %namedTypeHasArguments = icmp ult i32 0, %namedTypeNumArguments
  br i1 %namedTypeHasArguments, label %checkNamedTypeArgumentsLoop, label %noChange

checkNamedTypeArgumentsLoop:
  %namedTypeCheckLoopIndex = phi i32 [0, %named], [%nextNamedTypeCheckLoopIndex, %checkNamedTypeArgumentsLoopCheck]
  %namedTypeArgumentRTTIPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %namedRTTI, i32 0, i32 5, i32 1, i32 %namedTypeCheckLoopIndex
  %namedTypeArgumentRTTI = load %RTTI** %namedTypeArgumentRTTIPtr
  %specialisedTypeArgumentRTTI = call %RTTI* @plinth_get_specialised_type_info(%RTTI* %namedTypeArgumentRTTI, %TypeArgumentMapper* %mapper)
  %namedTypeArgumentDiffers = icmp ne %RTTI* %namedTypeArgumentRTTI, %specialisedTypeArgumentRTTI
  %nextNamedTypeCheckLoopIndex = add i32 %namedTypeCheckLoopIndex, 1
  br i1 %namedTypeArgumentDiffers, label %specialiseNamedType, label %checkNamedTypeArgumentsLoopCheck

checkNamedTypeArgumentsLoopCheck:
  %namedTypeCheckLoopHasNext = icmp ult i32 %nextNamedTypeCheckLoopIndex, %namedTypeNumArguments
  br i1 %namedTypeCheckLoopHasNext, label %checkNamedTypeArgumentsLoop, label %noChange

specialiseNamedType:
  %namedRTTISizePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* null, i32 0, i32 5, i32 1, i32 %namedTypeNumArguments
  %namedRTTISize = ptrtoint %RTTI** %namedRTTISizePtr to i32
  %namedAlloc = call i8* @calloc(i32 %namedRTTISize, i32 1)
  %specialisedNamed = bitcast i8* %namedAlloc to {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}*
  %namedTypeSearchListPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specialisedNamed, i32 0, i32 0
  store %TypeSearchList* %oldTypeSearchList, %TypeSearchList** %namedTypeSearchListPtr
  %namedSortIdPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specialisedNamed, i32 0, i32 1
  store i8 %sort, i8* %namedSortIdPtr
  %oldNamedNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %namedRTTI, i32 0, i32 2
  %namedNullability = load i1* %oldNamedNullabilityPtr
  %newNamedNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specialisedNamed, i32 0, i32 2
  store i1 %namedNullability, i1* %newNamedNullabilityPtr
  %oldNamedImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %namedRTTI, i32 0, i32 3
  %namedImmutability = load i1* %oldNamedImmutabilityPtr
  %newNamedImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specialisedNamed, i32 0, i32 3
  store i1 %namedImmutability, i1* %newNamedImmutabilityPtr
  %oldNamedNamePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %namedRTTI, i32 0, i32 4
  %namedName = load %RawString** %oldNamedNamePtr
  %newNamedNamePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specialisedNamed, i32 0, i32 4
  store %RawString* %namedName, %RawString** %newNamedNamePtr
  %newNamedTypeNumArgumentsPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specialisedNamed, i32 0, i32 5, i32 0
  store i32 %namedTypeNumArguments, i32* %newNamedTypeNumArgumentsPtr
  %newNamedTypeArgumentPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specialisedNamed, i32 0, i32 5, i32 1, i32 %namedTypeCheckLoopIndex
  store %RTTI* %specialisedTypeArgumentRTTI, %RTTI** %newNamedTypeArgumentPtr
  %namedTypeHasPreviousArguments = icmp ult i32 0, %namedTypeCheckLoopIndex
  br i1 %namedTypeHasPreviousArguments, label %copyPreviousNamedTypeArgumentsLoop, label %specialiseNamedTypeArguments

copyPreviousNamedTypeArgumentsLoop:
  %namedCopyIndex = phi i32 [0, %specialiseNamedType], [%nextNamedCopyIndex, %copyPreviousNamedTypeArgumentsLoop]
  %oldCopyNamedTypeArgumentPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %namedRTTI, i32 0, i32 5, i32 1, i32 %namedCopyIndex
  %copiedNamedTypeArgument = load %RTTI** %oldCopyNamedTypeArgumentPtr
  %newCopyNamedTypeArgumentPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specialisedNamed, i32 0, i32 5, i32 1, i32 %namedCopyIndex
  store %RTTI* %copiedNamedTypeArgument, %RTTI** %newCopyNamedTypeArgumentPtr
  %nextNamedCopyIndex = add i32 %namedCopyIndex, 1
  %namedCopyLoopHasNext = icmp ult i32 %nextNamedCopyIndex, %namedTypeCheckLoopIndex
  br i1 %namedCopyLoopHasNext, label %copyPreviousNamedTypeArgumentsLoop, label %specialiseNamedTypeArguments

specialiseNamedTypeArguments:
  %namedHasTypeArgumentsRemaining = icmp ult i32 %nextNamedTypeCheckLoopIndex, %namedTypeNumArguments
  br i1 %namedHasTypeArgumentsRemaining, label %specialiseNamedTypeArgumentsLoop, label %returnSpecialisedNamed

specialiseNamedTypeArgumentsLoop:
  %namedSpecialiseIndex = phi i32 [%nextNamedTypeCheckLoopIndex, %specialiseNamedTypeArguments], [%nextNamedSpecialiseIndex, %specialiseNamedTypeArgumentsLoop]
  %oldNamedTypeArgumentPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %namedRTTI, i32 0, i32 5, i32 1, i32 %namedSpecialiseIndex
  %oldNamedTypeArgument = load %RTTI** %oldNamedTypeArgumentPtr
  %specialisedNamedTypeArgument = call %RTTI* @plinth_get_specialised_type_info(%RTTI* %oldNamedTypeArgument, %TypeArgumentMapper* %mapper)
  %specialisedNamedTypeArgumentPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specialisedNamed, i32 0, i32 5, i32 1, i32 %namedSpecialiseIndex
  store %RTTI* %specialisedNamedTypeArgument, %RTTI** %specialisedNamedTypeArgumentPtr
  %nextNamedSpecialiseIndex = add i32 %namedSpecialiseIndex, 1
  %namedSpecialiseLoopHasNext = icmp ult i32 %nextNamedSpecialiseIndex, %namedTypeNumArguments
  br i1 %namedSpecialiseLoopHasNext, label %specialiseNamedTypeArgumentsLoop, label %returnSpecialisedNamed

returnSpecialisedNamed:
  %castedSpecialisedNamed = bitcast {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %specialisedNamed to %RTTI*
  ret %RTTI* %castedSpecialisedNamed

typeParameter:
  %typeParameterRTTI = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1, i1, i32}*
  %typeParameterIndexPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %typeParameterRTTI, i32 0, i32 4
  %typeParameterIndex = load i32* %typeParameterIndexPtr
  %mapperNumTypeArgumentsPtr = getelementptr %TypeArgumentMapper* %mapper, i32 0, i32 0
  %mapperNumTypeArguments = load i32* %mapperNumTypeArgumentsPtr
  %typeParameterIsInBounds = icmp ult i32 %typeParameterIndex, %mapperNumTypeArguments
  br i1 %typeParameterIsInBounds, label %returnSpecialisedTypeParameter, label %typeParameterError

typeParameterError:
  %errorMessage = bitcast { %opaque*, %opaque*, i32, i8(%RawString*, i32)*, void (%RawString*, i32, i8)*, [44 x i8] }* @TypeArgumentMappingErrorMessage to %String*
  call void @plinth_stderr_write(%String* %errorMessage)
  call void @abort()
  unreachable

returnSpecialisedTypeParameter:
  %typeParamNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %typeParameterRTTI, i32 0, i32 2
  %typeParamNullability = load i1* %typeParamNullabilityPtr
  %typeParamImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* %typeParameterRTTI, i32 0, i32 3
  %typeParamImmutability = load i1* %typeParamImmutabilityPtr
  %mappedTypeArgumentPtr = getelementptr %TypeArgumentMapper* %mapper, i32 0, i32 1, i32 %typeParameterIndex
  %mappedTypeArgument = load %RTTI** %mappedTypeArgumentPtr
  %forcedTypeArgument = call %RTTI* @plinth_force_type_modifiers(%RTTI* %mappedTypeArgument, i1 true, i1 %typeParamNullability, i1 %typeParamImmutability)
  ret %RTTI* %forcedTypeArgument

wildcard:
  %wildcardRTTI = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}*
  ; try not to waste any allocations here - we use an algorithm extremely similar to the one used for tuples above
  %wildcardNumSuperTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardRTTI, i32 0, i32 4
  %wildcardNumSubTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardRTTI, i32 0, i32 5
  %wildcardNumSuperTypes = load i32* %wildcardNumSuperTypesPtr
  %wildcardNumSubTypes = load i32* %wildcardNumSubTypesPtr
  %wildcardTotalTypes = add i32 %wildcardNumSuperTypes, %wildcardNumSubTypes
  %wildcardHasTypes = icmp ne i32 %wildcardTotalTypes, 0
  br i1 %wildcardHasTypes, label %checkWildcardTypesLoop, label %noChange

checkWildcardTypesLoop:
  %wildcardCheckLoopIndex = phi i32 [0, %wildcard], [%nextWildcardCheckLoopIndex, %checkWildcardTypesLoopCheck]
  %wildcardTypeRTTIPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardRTTI, i32 0, i32 6, i32 %wildcardCheckLoopIndex
  %wildcardTypeRTTI = load %RTTI** %wildcardTypeRTTIPtr
  %specialisedWildcardTypeRTTI = call %RTTI* @plinth_get_specialised_type_info(%RTTI* %wildcardTypeRTTI, %TypeArgumentMapper* %mapper)
  %wildcardTypeDiffers = icmp ne %RTTI* %wildcardTypeRTTI, %specialisedWildcardTypeRTTI
  %nextWildcardCheckLoopIndex = add i32 %wildcardCheckLoopIndex, 1
  br i1 %wildcardTypeDiffers, label %specialiseWildcardType, label %checkWildcardTypesLoopCheck

checkWildcardTypesLoopCheck:
  %wildcardCheckLoopHasNext = icmp ult i32 %nextWildcardCheckLoopIndex, %wildcardTotalTypes
  br i1 %wildcardCheckLoopHasNext, label %checkWildcardTypesLoop, label %noChange

specialiseWildcardType:
  %wildcardRTTISizePtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* null, i32 0, i32 6, i32 %wildcardNumSubTypes
  %wildcardRTTISize = ptrtoint %RTTI** %wildcardRTTISizePtr to i32
  %wildcardAlloc = call i8* @calloc(i32 %wildcardRTTISize, i32 1)
  %specialisedWildcard = bitcast i8* %wildcardAlloc to {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}*
  %wildcardTypeSearchListPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specialisedWildcard, i32 0, i32 0
  store %TypeSearchList* %oldTypeSearchList, %TypeSearchList** %wildcardTypeSearchListPtr
  %wildcardSortIdPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specialisedWildcard, i32 0, i32 1
  store i8 %sort, i8* %wildcardSortIdPtr
  %oldWildcardNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardRTTI, i32 0, i32 2
  %wildcardNullability = load i1* %oldWildcardNullabilityPtr
  %newWildcardNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specialisedWildcard, i32 0, i32 2
  store i1 %wildcardNullability, i1* %newWildcardNullabilityPtr
  %oldWildcardImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardRTTI, i32 0, i32 3
  %wildcardImmutability = load i1* %oldWildcardImmutabilityPtr
  %newWildcardImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specialisedWildcard, i32 0, i32 3
  store i1 %wildcardImmutability, i1* %newWildcardImmutabilityPtr
  %newWildcardNumSuperTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specialisedWildcard, i32 0, i32 4
  store i32 %wildcardNumSuperTypes, i32* %newWildcardNumSuperTypesPtr
  %newWildcardNumSubTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specialisedWildcard, i32 0, i32 5
  store i32 %wildcardNumSubTypes, i32* %newWildcardNumSubTypesPtr
  %newWildcardTypeRTTIPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specialisedWildcard, i32 0, i32 6, i32 %wildcardCheckLoopIndex
  store %RTTI* %specialisedWildcardTypeRTTI, %RTTI** %newWildcardTypeRTTIPtr
  %wildcardHasPreviousTypes = icmp ult i32 0, %wildcardCheckLoopIndex
  br i1 %wildcardHasPreviousTypes, label %copyPreviousWildcardTypesLoop, label %specialiseWildcardTypes

copyPreviousWildcardTypesLoop:
  %wildcardCopyIndex = phi i32 [0, %specialiseWildcardType], [%nextWildcardCopyIndex, %copyPreviousWildcardTypesLoop]
  %oldCopyWildcardTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardRTTI, i32 0, i32 6, i32 %wildcardCopyIndex
  %copiedWildcardType = load %RTTI** %oldCopyWildcardTypePtr
  %newCopyWildcardTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specialisedWildcard, i32 0, i32 6, i32 %wildcardCopyIndex
  store %RTTI* %copiedWildcardType, %RTTI** %newCopyWildcardTypePtr
  %nextWildcardCopyIndex = add i32 %wildcardCopyIndex, 1
  %wildcardCopyLoopHasNext = icmp ult i32 %nextWildcardCopyIndex, %wildcardCheckLoopIndex
  br i1 %wildcardCopyLoopHasNext, label %copyPreviousWildcardTypesLoop, label %specialiseWildcardTypes

specialiseWildcardTypes:
  %wildcardHasTypeArgumentsRemaining = icmp ult i32 %nextWildcardCheckLoopIndex, %wildcardTotalTypes
  br i1 %wildcardHasTypeArgumentsRemaining, label %specialiseWildcardTypesLoop, label %returnSpecialisedWildcard

specialiseWildcardTypesLoop:
  %wildcardSpecialiseIndex = phi i32 [%nextWildcardCheckLoopIndex, %specialiseWildcardTypes], [%nextWildcardSpecialiseIndex, %specialiseWildcardTypesLoop]
  %oldWildcardTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardRTTI, i32 0, i32 6, i32 %wildcardSpecialiseIndex
  %oldWildcardType = load %RTTI** %oldWildcardTypePtr
  %newWildcardType = call %RTTI* @plinth_get_specialised_type_info(%RTTI* %oldWildcardType, %TypeArgumentMapper* %mapper)
  %newWildcardTypePtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specialisedWildcard, i32 0, i32 6, i32 %wildcardSpecialiseIndex
  store %RTTI* %newWildcardType, %RTTI** %newWildcardTypePtr
  %nextWildcardSpecialiseIndex = add i32 %wildcardSpecialiseIndex, 1
  %wildcardSpecialiseLoopHasNext = icmp ult i32 %nextWildcardSpecialiseIndex, %wildcardTotalTypes
  br i1 %wildcardSpecialiseLoopHasNext, label %specialiseWildcardTypesLoop, label %returnSpecialisedWildcard

returnSpecialisedWildcard:
  %castedSpecialisedWildcard = bitcast {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %specialisedWildcard to %RTTI*
  ret %RTTI* %castedSpecialisedWildcard
}


define protected i1 @plinth_is_nullable(%RTTI* %type) {
entry:
  %sortPtr = getelementptr %RTTI* %type, i32 0, i32 1
  %sort = load i8* %sortPtr
  switch i8 %sort, label %notApplicable [i8  1, label %nullable      ; object
                                         i8  2, label %nullable      ; primitive
                                         i8  3, label %nullable      ; array
                                         i8  4, label %nullable      ; tuple
                                         i8  5, label %nullable      ; function
                                         i8  6, label %nullable      ; class
                                         i8  7, label %nullable      ; compound
                                         i8  8, label %nullable      ; interface
                                         i8  9, label %notApplicable ; void
                                         i8 10, label %nullType      ; null
                                         i8 11, label %nullable      ; type parameter
                                         i8 12, label %nullable]     ; wildcard

nullType:
  ret i1 true

nullable:
  %nullableRTTI = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1}*
  %nullablePtr = getelementptr {%TypeSearchList*, i8, i1}* %nullableRTTI, i32 0, i32 2
  %isNullable = load i1* %nullablePtr
  ret i1 %isNullable

notApplicable:
  ret i1 false
}

define protected i1 @plinth_is_immutable(%RTTI* %type) {
entry:
  %sortPtr = getelementptr %RTTI* %type, i32 0, i32 1
  %sort = load i8* %sortPtr
  switch i8 %sort, label %notApplicable [i8  1, label %immutable     ; object
                                         i8  2, label %notApplicable ; primitive
                                         i8  3, label %immutable     ; array
                                         i8  4, label %notApplicable ; tuple
                                         i8  5, label %notApplicable ; function (function immutability is not data-immutability)
                                         i8  6, label %immutable     ; class
                                         i8  7, label %immutable     ; compound
                                         i8  8, label %immutable     ; interface
                                         i8  9, label %notApplicable ; void
                                         i8 10, label %notApplicable ; null
                                         i8 11, label %immutable     ; type parameter
                                         i8 12, label %immutable]    ; wildcard

immutable:
  %immutableRTTI = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1, i1}*
  %immutablePtr = getelementptr {%TypeSearchList*, i8, i1, i1}* %immutableRTTI, i32 0, i32 3
  %isImmutable = load i1* %immutablePtr
  ret i1 %isImmutable

notApplicable:
  ret i1 false
}


define %RTTI* @plinth_force_type_modifiers(%RTTI* %type, i1 %onlyAdd, i1 %forceNullable, i1 %forceImmutable) {
entry:
  %sortPtr = getelementptr %RTTI* %type, i32 0, i32 1
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
                                         i8 11, label %bothNullableImmutable  ; type parameter
                                         i8 12, label %bothNullableImmutable] ; wildcard

bothNullableImmutable:
  %nullableImmutableRTTI = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1, i1}*
  %immutablePtr = getelementptr {%TypeSearchList*, i8, i1, i1}* %nullableImmutableRTTI, i32 0, i32 3
  %currentImmutable = load i1* %immutablePtr
  %currentForcedImmutable = and i1 %currentImmutable, %onlyAdd
  %alteredImmutable = or i1 %currentForcedImmutable, %forceImmutable
  %immutabilityNeedsAltering = xor i1 %currentImmutable, %alteredImmutable
  br i1 %immutabilityNeedsAltering, label %makeAlteration, label %onlyNullable

onlyNullable:
  %nullableRTTI = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1}*
  %nullablePtr = getelementptr {%TypeSearchList*, i8, i1}* %nullableRTTI, i32 0, i32 2
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
                                         i8 11, label %alterTypeParameter  ; type parameter
                                         i8 12, label %alterWildcard]      ; wildcard

alterObject:
  %objectSizePtr = getelementptr {%TypeSearchList*, i8, i1, i1}* null, i32 1
  %objectSize = ptrtoint {%TypeSearchList*, i8, i1, i1}* %objectSizePtr to i32
  br label %generalAlterBoth

alterPrimitive:
  %primitiveSizePtr = getelementptr {%TypeSearchList*, i8, i1, i8}* null, i32 1
  %primitiveSize = ptrtoint {%TypeSearchList*, i8, i1, i8}* %primitiveSizePtr to i32
  br label %generalAlterNullable

alterArray:
  %arraySizePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*}* null, i32 1
  %arraySize = ptrtoint {%TypeSearchList*, i8, i1, i1, %RTTI*}* %arraySizePtr to i32
  br label %generalAlterBoth

alterTuple:
  %tupleRTTI = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}*
  %tupleNumSubTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* %tupleRTTI, i32 0, i32 3
  %tupleNumSubTypes = load i32* %tupleNumSubTypesPtr
  %tupleSizePtr = getelementptr {%TypeSearchList*, i8, i1, i32, [0 x %RTTI*]}* null, i32 0, i32 4, i32 %tupleNumSubTypes
  %tupleSize = ptrtoint %RTTI** %tupleSizePtr to i32
  br label %generalAlterNullable

alterFunction:
  %functionRTTI = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}*
  %functionNumParametersPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* %functionRTTI, i32 0, i32 5
  %functionNumParameters = load i32* %functionNumParametersPtr
  %functionSizePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RTTI*, i32, [0 x %RTTI*]}* null, i32 0, i32 6, i32 %functionNumParameters
  %functionSize = ptrtoint %RTTI** %functionSizePtr to i32
  br label %generalAlterNullable

alterNamed:
  %namedRTTI = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}*
  %namedNumArgumentsPtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* %namedRTTI, i32 0, i32 5, i32 0
  %namedNumArguments = load i32* %namedNumArgumentsPtr
  %namedSizePtr = getelementptr {%TypeSearchList*, i8, i1, i1, %RawString*, %TypeArgumentMapper}* null, i32 0, i32 5, i32 1, i32 %namedNumArguments
  %namedSize = ptrtoint %RTTI** %namedSizePtr to i32
  br label %generalAlterBoth

alterTypeParameter:
  %typeParameterSizePtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32}* null, i32 1
  %typeParameterSize = ptrtoint {%TypeSearchList*, i8, i1, i1, i32}* %typeParameterSizePtr to i32
  br label %generalAlterBoth

alterWildcard:
  %wildcardRTTI = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}*
  %wildcardNumSuperTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardRTTI, i32 0, i32 4
  %wildcardNumSubTypesPtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* %wildcardRTTI, i32 0, i32 5
  %wildcardNumSuperTypes = load i32* %wildcardNumSuperTypesPtr
  %wildcardNumSubTypes = load i32* %wildcardNumSubTypesPtr
  %wildcardTotalTypes = add i32 %wildcardNumSuperTypes, %wildcardNumSubTypes
  %wildcardSizePtr = getelementptr {%TypeSearchList*, i8, i1, i1, i32, i32, [0 x %RTTI*]}* null, i32 0, i32 6, i32 %wildcardTotalTypes
  %wildcardSize = ptrtoint %RTTI** %wildcardSizePtr to i32
  br label %generalAlterBoth

generalAlterNullable:
  %alterNullableSize = phi i32 [%primitiveSize, %alterPrimitive], [%tupleSize, %alterTuple], [%functionSize, %alterFunction]
  br label %reallocate

generalAlterBoth:
  %alterBothSize = phi i32 [%objectSize, %alterObject], [%arraySize, %alterArray], [%namedSize, %alterNamed], [%typeParameterSize, %alterTypeParameter], [%wildcardSize, %alterWildcard]
  br label %reallocate

reallocate:
  %size = phi i32 [%alterNullableSize, %generalAlterNullable], [%alterBothSize, %generalAlterBoth]
  %modifyImmutability = phi i1 [false, %generalAlterNullable], [true, %generalAlterBoth]
  %alloc = call i8* @calloc(i32 %size, i32 1)
  %typePointer = bitcast %RTTI* %type to i8*
  call i8* @memcpy(i8* %alloc, i8* %typePointer, i32 %size)
  br i1 %modifyImmutability, label %alterImmutability, label %alterNullability

alterImmutability:
  %oldRTTIWithImmutability = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1, i1}*
  %newRTTIWithImmutability = bitcast i8* %alloc to {%TypeSearchList*, i8, i1, i1}*
  %oldImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1}* %oldRTTIWithImmutability, i32 0, i32 3
  %newImmutabilityPtr = getelementptr {%TypeSearchList*, i8, i1, i1}* %newRTTIWithImmutability, i32 0, i32 3
  %oldImmutability = load i1* %oldImmutabilityPtr
  %oldForcedImmutability = and i1 %oldImmutability, %onlyAdd
  %newImmutability = or i1 %oldForcedImmutability, %forceImmutable
  store i1 %newImmutability, i1* %newImmutabilityPtr
  br label %alterNullability

alterNullability:
  %oldRTTIWithNullability = bitcast %RTTI* %type to {%TypeSearchList*, i8, i1}*
  %newRTTIWithNullability = bitcast i8* %alloc to {%TypeSearchList*, i8, i1}*
  %oldNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1}* %oldRTTIWithNullability, i32 0, i32 2
  %newNullabilityPtr = getelementptr {%TypeSearchList*, i8, i1}* %newRTTIWithNullability, i32 0, i32 2
  %oldNullability = load i1* %oldNullabilityPtr
  %oldForcedNullability = and i1 %oldNullability, %onlyAdd
  %newNullability = or i1 %oldForcedNullability, %forceNullable
  store i1 %newNullability, i1* %newNullabilityPtr
  %result = bitcast i8* %alloc to %RTTI*
  ret %RTTI* %result
}

