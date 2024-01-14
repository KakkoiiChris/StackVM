package stackvm.cpu

enum class Instruction {
    HALT, //  0
    PUSH, //  1
    POP,  //  2
    DUP,  //  3
    ADD,  //  4
    SUB,  //  5
    MUL,  //  6
    DIV,  //  7
    MOD,  //  8
    NEG,  //  9
    AND,  // 10
    OR,   // 11
    NOT,  // 12
    EQU,  // 13
    GRT,  // 14
    GEQ,  // 15
    JMP,  // 16
    JIF,  // 17
    LOAD, // 18
    STORE // 19
}