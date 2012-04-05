package nativelib.c;

import com.sun.jna.Memory;
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
   * @param garbageCollect - true if the native pointer array should be garbage collected when it goes out of scope in Java code,
   *                         false to allocate it using malloc(), forcing it to be freed manually using C.free() or in the native code.
   * @return the Pointer to the start of the native array
   */
  public static Pointer toNativePointerArray(PointerType[] javaArray, boolean nullTerminator, boolean garbageCollect)
  {
    Pointer[] pointerArray = new Pointer[javaArray.length];
    for (int i = 0; i < javaArray.length; i++)
    {
      pointerArray[i] = javaArray[i].getPointer();
    }
    int size = pointerArray.length * Pointer.SIZE + (nullTerminator ? Pointer.SIZE : 0);
    if (size == 0)
    {
      // don't bother allocating zero bytes of memory, malloc() can return null in this case anyway
      return Pointer.NULL;
    }
    Pointer result = garbageCollect ? new Memory(size) : C.malloc(size);
    result.write(0, pointerArray, 0, pointerArray.length);
    if (nullTerminator)
    {
      result.setPointer(pointerArray.length * Pointer.SIZE, Pointer.NULL);
    }
    return result;
  }
}
