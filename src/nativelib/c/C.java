package nativelib.c;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/*
 * Created on 4 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class C
{
  static
  {
    Native.register(Platform.isWindows() ? "msvcrt" : "c");
  }
  public static native Pointer malloc(int size);
  public static native void free(Pointer pointer);

  /**
   * Creates a native Pointer array to represent the specified PointerType[].
   * The resulting value is a single Pointer to the first element of the native array.
   * @param javaArray - the Java array to convert
   * @param nullTerminator - true to null terminate the array, false to leave it without a null terminator
   * @return the Pointer to the start of the native array
   */
  public static Pointer toNativePointerArray(PointerType[] javaArray, boolean nullTerminator)
  {
    Pointer[] pointerArray = new Pointer[javaArray.length];
    for (int i = 0; i < javaArray.length; i++)
    {
      pointerArray[i] = javaArray[i].getPointer();
    }
    Pointer result = C.malloc(pointerArray.length * Pointer.SIZE + (nullTerminator ? Pointer.SIZE : 0));
    result.write(0, pointerArray, 0, pointerArray.length);
    if (nullTerminator)
    {
      result.setPointer(pointerArray.length * Pointer.SIZE, Pointer.NULL);
    }
    return result;
  }
}
