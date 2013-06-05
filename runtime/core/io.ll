; ModuleID = 'io.ll'

%opaque = type opaque

%String = type { %opaque*, %opaque*, i32, i8(%String*, i32)*, void (%String*, i32, i8)* }

define void @plinth_stdout_write(%String* %array) {
entry:
  %lenptr = getelementptr %String* %array, i32 0, i32 2
  %len = load i32* %lenptr
  %check = icmp ult i32 0, %len
  %getterPtr = getelementptr %String* %array, i32 0, i32 3
  %getter = load i8(%String*, i32)** %getterPtr
  br i1 %check, label %loop, label %exit

loop:
  %i = phi i32 [0, %entry], [%inc, %loop]
  %c = call i8 %getter(%String* %array, i32 %i)
  %cext = zext i8 %c to i32
  call i32 @plinth_stdout_putc(i32 %cext)
  %inc = add i32 %i, 1
  %b = icmp ult i32 %inc, %len
  br i1 %b, label %loop, label %exit

exit:
  ret void
}

define void @plinth_stderr_write(%String* %array) {
entry:
  %lenptr = getelementptr %String* %array, i32 0, i32 2
  %len = load i32* %lenptr
  %check = icmp ult i32 0, %len
  %getterPtr = getelementptr %String* %array, i32 0, i32 3
  %getter = load i8(%String*, i32)** %getterPtr
  br i1 %check, label %loop, label %exit

loop:
  %i = phi i32 [0, %entry], [%inc, %loop]
  %c = call i8 %getter(%String* %array, i32 %i)
  %cext = zext i8 %c to i32
  call i32 @plinth_stderr_putc(i32 %cext)
  %inc = add i32 %i, 1
  %b = icmp ult i32 %inc, %len
  br i1 %b, label %loop, label %exit

exit:
  ret void
}

declare i32 @plinth_stdout_putc(i32)
declare i32 @plinth_stderr_putc(i32)
