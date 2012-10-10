## Plinth

This project aims to implement a new programming language called "Plinth".

There is not yet much documentation on the syntax and semantics of the language, but more will be made available when more of the core language features have been implemented.

### Instructions

You will need: Java (JDK) >= 1.5, LLVM 3.2, and optionally GCC (for compilation to native code)

To run the compiler, you will need an LLVM shared library to be installed. The version it requires has not yet been released, so it must be built from the LLVM source repository (revision 164247 or later). Specifically, it looks for "LLVM-3.2svn" (libLLVM-3.2svn.so on Linux).

To compile the compiler:

    src$ javac -d ../bin -cp ../lib/jna.jar:. **/*.java

To run:

    bin$ java -cp ../lib/jna.jar:. eu.bryants.anthony.plinth.compiler.Compiler -o binary.pbc -l plinth-lib/core.pbc /path/to/source.pth /path/to/source2.pth

The files in plinth-src, including `string.pth`, `stdout.pth`, and `stderr.pth`, should be compiled along with your own source files to allow you to use strings and the standard I/O streams.

The file `plinth-lib/core.pbc` is the compiled version of `plinth-lib/core/*.ll`, and includes low level functions that must be linked to your program in order to use `stdin`, `stdout`, and `stderr`.

To generate an executable from the linked bitcode:

    llc binary.pbc -o binary.s
    gcc binary.s -o binary

To run your program and check the result:

    ./binary
    echo $?

Alternatively, you can run the LLVM bitcode generated by the compiler directly via the LLVM interpreter:

    lli binary.pbc
    echo $?

