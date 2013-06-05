; ModuleID = 'exception.ll'

%opaque = type opaque
%RawString = type {%opaque*, %opaque*, i32, i8(%RawString*, i32)*, void (%RawString*, i32, i8)*, [0 x i8]}
%String    = type {%opaque*, %opaque*, i32, i8(%String*,    i32)*, void (%String*,    i32, i8)*}


declare i32 @_Unwind_RaiseException(i8* %exception)
declare void @plinth_throw_failed(i8* %exception, i32 %result)

define void @plinth_throw(i8* %exception) uwtable {
entry:
  %result = call i32 @_Unwind_RaiseException(i8* %exception)
  call void @plinth_throw_failed(i8* %exception, i32 %result)
  unreachable
}

declare void @plinth_stderr_write(%String* %array)
declare void @abort()

@OutOfMemoryErrorMessage = private unnamed_addr constant
                           {%opaque*, %opaque*, i32, i8(%RawString*, i32)*, void (%RawString*, i32, i8)*, [27 x i8]}
                           {
                             %opaque* null,
                             %opaque* null,
                             i32 73,
                             i8(%RawString*, i32)* @OutOfMemoryError_string_array_getter,
                             void (%RawString*, i32, i8)* @OutOfMemoryError_string_array_setter,
                             [27 x i8] c"Out of memory! Aborting...\0A"
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


define void @plinth_outofmemory_abort() {
entry:
  %errorMessage = bitcast {%opaque*, %opaque*, i32, i8(%RawString*, i32)*, void (%RawString*, i32, i8)*, [27 x i8]}* @OutOfMemoryErrorMessage to %String*
  call void @plinth_stderr_write(%String* %errorMessage)
  call void @abort()
  unreachable
}
