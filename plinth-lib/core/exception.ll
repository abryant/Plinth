; ModuleID = 'exception.ll'

%opaque = type opaque
%VFT = type [0 x %opaque*]
%RawString = type { %opaque*, %opaque*, i32, [0 x i8]}
%VFTSearchList = type {i32, [0 x {%RawString*, %VFT*}]}
%object = type { { %VFTSearchList*, { i8, i32 } }*, %VFT* }
%NamedRTTI = type { %VFTSearchList*, {i8, i32, i1, i1, %RawString*} }

declare protected %VFT* @plinth_core_find_vft(%VFTSearchList* %vftSearchList, %RawString* %searchName)

declare void @abort()

; This method is used in exception handling, to find out whether an exception is caught by a given handler
define i1 @plinth_exception_instanceof(i8* %exception, i8* %typeInfo) {
entry:
  %plinthObject = bitcast i8* %exception to %object*
  %objectRTTIPointer = getelementptr %object* %plinthObject, i32 0, i32 0
  %objectRTTI = load { %VFTSearchList*, {i8, i32} }** %objectRTTIPointer
  %searchListPointer = getelementptr { %VFTSearchList*, {i8, i32} }* %objectRTTI, i32 0, i32 0
  %searchList = load %VFTSearchList** %searchListPointer
  %rttiPointer = bitcast i8* %typeInfo to %NamedRTTI*
  %sortIdPointer = getelementptr %NamedRTTI* %rttiPointer, i32 0, i32 1, i32 0
  %sortId = load i8* %sortIdPointer
  %isClass = icmp eq i8 %sortId, 6
  br i1 %isClass, label %classRTTI, label %otherRTTI

classRTTI:
  %typeNamePointer = getelementptr %NamedRTTI* %rttiPointer, i32 0, i32 1, i32 4
  %typeName = load %RawString** %typeNamePointer
  %result = call %VFT* @plinth_core_find_vft(%VFTSearchList* %searchList, %RawString* %typeName)
  %isInstance = icmp ne %VFT* %result, null
  ret i1 %isInstance

otherRTTI:
; Exception checks must be againt class types, so we need to crash here to signal the error
  call void @abort()
  unreachable
}

