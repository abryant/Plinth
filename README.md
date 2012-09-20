## A Toy Compiler

This project is mainly for me to experiment with writing a very small compiler, and to try implementing an LLVM code generator.

### Instructions

You will need: Java (JDK) >= 1.5, LLVM 3.2, and optionally GCC (for compilation to native code)

To run the compiler, you will need an LLVM shared library to be installed. The version it requires has not yet been released, so it must be built from the LLVM source repository (revision 164247 or later). Specifically, it looks for "LLVM-3.2svn" (libLLVM-3.2svn.so on Linux).

To compile the compiler:

    src$ javac -d ../bin -cp ../lib/jna.jar:. **/*.java

To run:

    bin$ java -cp ../lib/jna.jar:. eu.bryants.anthony.toylanguage.compiler.Compiler -d /path/to/output/folder /path/to/source.txt /path/to/source2.txt

To generate machine code from the given LLVM bitcode:

    llc binary.bc
    gcc -c binary.s
    gcc -o binary binary.o

To run your program and check the result:

    ./binary
    echo $?

Alternatively, you can run the LLVM bitcode generated by the compiler directly via the LLVM interpreter:

    lli binary.bc
    echo $?

