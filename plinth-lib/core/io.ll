; ModuleID = 'io.ll'

%filedesc = type opaque

@stdin = external global %filedesc*
@stdout = external global %filedesc*
@stderr = external global %filedesc*

define i32 @plinth_stdin_read() {
entry:
  %in = load %filedesc** @stdin
  %val = call i32 @fgetc(%filedesc* %in)
  ret i32 %val
}

define void @plinth_stdout_write({ i32, [0 x i8] }* %array) {
entry:
  %lenptr = getelementptr {i32, [0 x i8]}* %array, i32 0, i32 0
  %len = load i32* %lenptr
  %out = load %filedesc** @stdout
  %check = icmp ult i32 0, %len
  br i1 %check, label %loop, label %exit

loop:
  %i = phi i32 [0, %entry], [%inc, %loop]
  %data = getelementptr {i32, [0 x i8]}* %array, i32 0, i32 1, i32 %i
  %c = load i8* %data
  %cext = zext i8 %c to i32
  call i32 @fputc(i32 %cext, %filedesc* %out)
  %inc = add i32 %i, 1
  %b = icmp ult i32 %inc, %len
  br i1 %b, label %loop, label %exit

exit:
  ret void
}

define void @plinth_stderr_write({ i32, [0 x i8] }* %array) {
entry:
  %lenptr = getelementptr {i32, [0 x i8]}* %array, i32 0, i32 0
  %len = load i32* %lenptr
  %err = load %filedesc** @stderr
  %check = icmp ult i32 0, %len
  br i1 %check, label %loop, label %exit

loop:
  %i = phi i32 [0, %entry], [%inc, %loop]
  %data = getelementptr {i32, [0 x i8]}* %array, i32 0, i32 1, i32 %i
  %c = load i8* %data
  %cext = zext i8 %c to i32
  call i32 @fputc(i32 %cext, %filedesc* %err)
  %inc = add i32 %i, 1
  %b = icmp ult i32 %inc, %len
  br i1 %b, label %loop, label %exit

exit:
  ret void
}

declare i32 @fgetc(%filedesc*)
declare i32 @fputc(i32, %filedesc*)
