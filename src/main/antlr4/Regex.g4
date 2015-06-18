grammar Regex;

start returns [Int a]
    : t z { a = (0#a ?: 0) + (1#a ?: 0) }
    ;

z returns [Int a]
    : OR start { a = (1#a ?: 0) + 1 }
    |
    ;

t returns [Int a]
    : n x d { a = (0#a ?: 0) + (1#a ?: 0) + (2#a ?: 0) }
    ;

d returns [Int a]
    : t { a = 0#a ?: 0 }
    |
    ;

n returns [Int a]
    : C { a = 1 }
    | LBR start RBR { a = 2 + (1#a ?: 0) }
    ;

x returns [Int a]
    : AST { a = 1 }
    |
    ;

OR: '|';
AST: '*';
LBR: '(';
RBR: ')';
C: 'a'..'z';