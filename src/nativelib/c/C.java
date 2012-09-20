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
      pointerArray[i] = javaArray[i] == null ? null : javaArray[i].getPointer();
    }
    int size = pointerArray.length * Pointer.SIZE + (nullTerminator ? Pointer.SIZE : 0);
    if (size == 0)
    {
      // don't bother allocating zero bytes of memory, malloc() can return null in this case anyway
      return null;
    }
    Pointer result = garbageCollect ? new Memory(size) : C.malloc(size);
    result.write(0, pointerArray, 0, pointerArray.length);
    if (nullTerminator)
    {
      result.setPointer(pointerArray.length * Pointer.SIZE, Pointer.NULL);
    }
    return result;
  }

  /**
   * A very simple class which contains an abstract method to convert a Pointer to some other type T.
   * @author Anthony Bryant
   * @param <T> - the type that this will convert Pointers to
   */
  public static abstract class PointerConverter<T>
  {
    public abstract T convert(Pointer pointer);
  }

  /**
   * Reads from a native array of pointers (such as one created by <code>toNativePointerArray()</code>) into the specified java array.
   * The native array is assumed to have the same length as the java array.
   *
   * Since we usually want to convert to a PointerType here, a PointerConverter is required to convert each JNA Pointer into the base type of the array.
   * @param nativePointer - the native pointer to read the array contents from
   * @param javaArray - the java array to put the array contents into
   * @param converter - the converter that can convert from a Pointer to the base type of the array
   */
  public static <T> void readNativePointerArray(Pointer nativePointer, T[] javaArray, PointerConverter<T> converter)
  {
    Pointer[] pointerArray = new Pointer[javaArray.length];
    nativePointer.read(0, pointerArray, 0, javaArray.length);
    for (int i = 0; i < javaArray.length; ++i)
    {
      javaArray[i] = converter.convert(pointerArray[i]);
    }
  }
}
