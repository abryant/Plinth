; ModuleID = 'io.ll'

%opaque = type opaque

define void @plinth_stdout_write({ %opaque*, %opaque*, i32, [0 x i8] }* %array) {
entry:
  %lenptr = getelementptr {%opaque*, %opaque*, i32, [0 x i8]}* %array, i32 0, i32 2
  %len = load i32* %lenptr
  %check = icmp ult i32 0, %len
  br i1 %check, label %loop, label %exit

loop:
  %i = phi i32 [0, %entry], [%inc, %loop]
  %data = getelementptr {%opaque*, %opaque*, i32, [0 x i8]}* %array, i32 0, i32 3, i32 %i
  %c = load i8* %data
  %cext = zext i8 %c to i32
  call i32 @plinth_stdout_putc(i32 %cext)
  %inc = add i32 %i, 1
  %b = icmp ult i32 %inc, %len
  br i1 %b, label %loop, label %exit

exit:
  ret void
}

define void @plinth_stderr_write({ %opaque*, %opaque*, i32, [0 x i8] }* %array) {
entry:
  %lenptr = getelementptr {%opaque*, %opaque*, i32, [0 x i8]}* %array, i32 0, i32 2
  %len = load i32* %lenptr
  %check = icmp ult i32 0, %len
  br i1 %check, label %loop, label %exit

loop:
  %i = phi i32 [0, %entry], [%inc, %loop]
  %data = getelementptr {%opaque*, %opaque*, i32, [0 x i8]}* %array, i32 0, i32 3, i32 %i
  %c = load i8* %data
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
