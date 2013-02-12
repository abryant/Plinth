; ModuleID = 'ExceptionTest.ll'

%Exception_VFT = type { void (%Exception*)* }
%Exception = type { { %VFTSearchList*, { i8, i32 } }*, %o_VFT*, %Exception_VFT*, %string }
%VFTSearchList = type { i32, [0 x { %AB*, [0 x %opaque*]* }] }
%AB = type { { %VFTSearchList*, { i8, i32 } }*, %o_VFT*, i32, [0 x i8] }
%o_VFT = type { %string (%object*)* }
%string = type { %AB*, %AI* }
%AI = type { { %VFTSearchList*, { i8, i32 } }*, %o_VFT*, i32, [0 x i32] }
%object = type { { %VFTSearchList*, { i8, i32 } }*, %o_VFT* }
%opaque = type opaque

declare void @_Unwind_RaiseException(i8*)

@_INSTANCE_RTTI_C9ExceptionE = external constant { %VFTSearchList*, { i8, i32, i1, i1, %AB* } }
@_STR_Yay_2C_20Exceptions_21 = external constant { { %VFTSearchList*, { i8, i32 } }*, %o_VFT*, i32, [16 x i8] }

define i32 @main(i32 %argc) uwtable {
  call i32 @plinth_stdout_putc(i32 97)
  %result = call i32 @foo(i32 %argc)
  ret i32 %result
}

define i32 @foo(i32 %argc) uwtable {
entry:
  call i32 @plinth_stdout_putc(i32 98)
  %result = invoke i32 @getResult()
              to label %return
              unwind label %foo_lpad

foo_lpad:
  %lpad_result = landingpad {i8*,i32}
                   personality i8* bitcast (i32 (i32,i32,i64,i8*,i8*)* @my_personality to i8*)
                   catch i8* bitcast ({ %VFTSearchList*, { i8, i32, i1, i1, %AB* } }* @_INSTANCE_RTTI_C9ExceptionE to i8*)
  call i32 @plinth_stdout_putc(i32 43)
  %value = extractvalue {i8*,i32} %lpad_result, 0
  %catch = tail call i8* @plinth_catch(i8* %value) nounwind
  %caughtObject = bitcast i8* %catch to %Exception*
  call void @printException(%Exception* %caughtObject)
  ret i32 99;

return:
  call i32 @plinth_stdout_putc(i32 100)
  ret i32 %result
}

define i32 @getResult() uwtable {
  call i32 @plinth_stdout_putc(i32 104)
  %exceptionObject = call %Exception* @createException()
  %exceptionObjectCasted = bitcast %Exception* %exceptionObject to i8*
  %exception = tail call i8* @plinth_create_exception(i8* %exceptionObjectCasted)
  invoke void @_Unwind_RaiseException(i8* %exception)
    to label %return
    unwind label %getResult_lpad

getResult_lpad:
  %lpad_result = landingpad {i8*,i32}
                   personality i8* bitcast (i32 (i32,i32,i64,i8*,i8*)* @my_personality to i8*)
                   cleanup
  call i32 @plinth_stdout_putc(i32 42)
  resume {i8*,i32} %lpad_result

return:
  unreachable
}

define %Exception* @createException() {
entry:
  %0 = alloca %string
  %e = alloca %Exception*
  %1 = getelementptr %string* %0, i32 0, i32 1
  store %AI* null, %AI** %1
  call void @_C6string__cAB(%string* %0, %AB* bitcast ({ { %VFTSearchList*, { i8, i32 } }*, %o_VFT*, i32, [16 x i8] }* @_STR_Yay_2C_20Exceptions_21 to %AB*))
  %2 = load %string* %0
  %3 = call %Exception* @_A9Exception()
  call void @_C9Exception__cV6stringE(%Exception* %3, %string %2)
  store %Exception* %3, %Exception** %e
  ret %Exception* %3
}

define void @printException(%Exception* %exc) {
  %e = alloca %Exception*
  store %Exception* %exc, %Exception** %e
  %1 = load %Exception** %e
  %2 = getelementptr %Exception* %1, i32 0, i32 2
  %3 = load %Exception_VFT** %2
  %4 = getelementptr %Exception_VFT* %3, i32 0, i32 0
  %5 = load void (%Exception*)** %4
  call void %5(%Exception* %1)
  ret void
}


declare i8* @calloc(i32, i32)
declare void @free(i8*)

define i32 @my_personality(i32 %version, i32 %action, i64 %class, i8* %exception, i8* %context) {
entry:
  call i32 @plinth_stdout_putc(i32 35)
  %ascii = add i32 %action, 65
  call i32 @plinth_stdout_putc(i32 %ascii)
  %result = tail call i32 @plinth_personality(i32 %version, i32 %action, i64 %class, i8* %exception, i8* %context)
  %asciiResult = add i32 %result, 80
  call i32 @plinth_stdout_putc(i32 %asciiResult)
  ret i32 %result
}

define i32 @foo_personality(i32 %version, i32 %action, i64 %class, i8* %exception, i8* %context) {
entry:
  call i32 @plinth_stdout_putc(i32 33)
  %ascii = add i32 %action, 65
  call i32 @plinth_stdout_putc(i32 %ascii)
  %result = tail call i32 @plinth_personality(i32 %version, i32 %action, i64 %class, i8* %exception, i8* %context)
  %asciiResult = add i32 %result, 80
  call i32 @plinth_stdout_putc(i32 %asciiResult)
  ret i32 %result
}

declare %Exception* @_A9Exception()
declare void @_C9Exception__cV6stringE(%Exception* %this, %string %message)
declare void @_M9Exception_print_v_(%Exception* %this)
declare void @_C6string__cAB(%string*, %AB*)

declare i32 @plinth_personality(i32 %version, i32 %actions, i64 %class, i8* %exception, i8* %context)
declare i8* @plinth_create_exception(i8* %plinthExceptionObject)
declare void @plinth_throw(i8* %exception)
declare i8* @plinth_catch(i8* %exception)

declare i32 @plinth_stdout_putc(i32 %c)

