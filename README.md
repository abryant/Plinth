# A Toy Compiler

This project is mainly for me to experiment with writing a very small compiler, and to try implementing an LLVM code generator.

## Instructions

To compile the compiler:
    src$ javac -d ../bin -cp ../lib/jna.jar:. **/*.java

To run:
    bin$ java -cp ../lib/jna.jar:. eu.bryants.anthony.toylanguage.compiler.Compiler /path/to/source.txt /path/to/binary.bc

To generate machine code from the given LLVM bitcode:
    llc binary.bc
    gcc -c binary.s
    gcc -o binary binary.o

To run your program and check the result:
    ./binary
    echo $?

