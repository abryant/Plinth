; ModuleID = 'maths.ll'

define i64 @plinth_maths_bitcast_double_to_ulong(double %value) {
entry:
  %result = bitcast double %value to i64
  ret i64 %result
}

define i32 @plinth_maths_bitcast_float_to_uint(float %value) {
entry:
  %result = bitcast float %value to i32
  ret i32 %result
}
