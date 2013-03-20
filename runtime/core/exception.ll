; ModuleID = 'exception.ll'

%opaque = type opaque
%VFT = type [0 x %opaque*]
%RawString = type { %opaque*, %opaque*, i32, [0 x i8]}
%VFTSearchList = type {i32, [0 x {%RawString*, %VFT*}]}
%object = type { { %VFTSearchList*, { i8, i32 } }*, %VFT* }
%NamedRTTI = type { i8, i32, i1, i1, %RawString*}

@OutOfMemoryErrorMessage = private hidden unnamed_addr constant { %opaque*, %opaque*, i32, [27 x i8] } {%opaque* null, %opaque* null, i32 27, [27 x i8] c"Out of memory! Aborting...\0A"}
@BadRTTIErrorMessage = private hidden unnamed_addr constant { %opaque*, %opaque*, i32, [32 x i8] } {%opaque* null, %opaque* null, i32 32, [32 x i8] c"Bad Exception RTTI! Aborting...\0A"}

declare protected %VFT* @plinth_core_find_vft(%VFTSearchList* %vftSearchList, %RawString* %searchName)

declare void @plinth_stderr_write({ %opaque*, %opaque*, i32, [0 x i8] }* %array)

declare void @abort()

declare i32 @_Unwind_RaiseException(i8* %exception)
declare void @plinth_throw_failed(i8* %exception, i32 %result)

define void @plinth_throw(i8* %exception) uwtable {
entry:
  %result = call i32 @_Unwind_RaiseException(i8* %exception)
  call void @plinth_throw_failed(i8* %exception, i32 %result)
  unreachable
}

; This method is used in exception handling, to find out whether an exception is caught by a given handler
define i1 @plinth_exception_instanceof(i8* %exception, i8* %typeInfo) {
entry:
  %plinthObject = bitcast i8* %exception to %object*
  %objectRTTIPointer = getelementptr %object* %plinthObject, i32 0, i32 0
  %objectRTTI = load { %VFTSearchList*, {i8, i32} }** %objectRTTIPointer
  %searchListPointer = getelementptr { %VFTSearchList*, {i8, i32} }* %objectRTTI, i32 0, i32 0
  %searchList = load %VFTSearchList** %searchListPointer
  %rttiPointer = bitcast i8* %typeInfo to %NamedRTTI*
  %sortIdPointer = getelementptr %NamedRTTI* %rttiPointer, i32 0, i32 0
  %sortId = load i8* %sortIdPointer
  %isClass = icmp eq i8 %sortId, 6
  %isInterface = icmp eq i8 %sortId, 8
  %canCheckType = or i1 %isClass, %isInterface
  br i1 %canCheckType, label %checkRTTI, label %badRTTI

checkRTTI:
  %typeNamePointer = getelementptr %NamedRTTI* %rttiPointer, i32 0, i32 4
  %typeName = load %RawString** %typeNamePointer
  %result = call %VFT* @plinth_core_find_vft(%VFTSearchList* %searchList, %RawString* %typeName)
  %isInstance = icmp ne %VFT* %result, null
  ret i1 %isInstance

badRTTI:
; Exception checks must be against class or interface types, so we need to crash here to signal the error
  %errorMessage = bitcast { %opaque*, %opaque*, i32, [32 x i8] }* @BadRTTIErrorMessage to { %opaque*, %opaque*, i32, [0 x i8] }*
  call void @plinth_stderr_write({ %opaque*, %opaque*, i32, [0 x i8] }* %errorMessage)
  call void @abort()
  unreachable
}

define void @plinth_outofmemory_abort() {
entry:
  %errorMessage = bitcast { %opaque*, %opaque*, i32, [27 x i8] }* @OutOfMemoryErrorMessage to { %opaque*, %opaque*, i32, [0 x i8] }*
  call void @plinth_stderr_write({ %opaque*, %opaque*, i32, [0 x i8] }* %errorMessage)
  call void @abort()
  unreachable
}

