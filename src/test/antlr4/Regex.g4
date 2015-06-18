grammar Regex;

start returns [Int a]
    : t z { a = (0#a ?: 0) + 1#a; }
    ;

z returns [Int a]
    : OR start { a = 1#a + 1 }
    | { a = 0; }
    ;

t returns [Int a]
    : n x d { a = 0#a + 1#a + 2#a; }
    ;

d returns [Int a]
    : t { a = 0#a; }
    | { a = 0; }
    ;

n returns [Int a]
    : C { a = 1 }
    | LBR start RBR { a = 2 + 1#a; }
    ;

x returns [Int a]
    : AST { a = 1; }
    | { a = 0; }
    ;

OR: '|';
AST: '*';
LBR: '(';
RBR: ')';
C: 'a'..'z';