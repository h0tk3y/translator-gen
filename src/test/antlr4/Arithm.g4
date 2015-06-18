grammar Arithm;

start returns [Int v]
    : t e { v = 0#v + (1#v ?: 0); }
    ;

e returns [Int? v]
    : PLUS t e { v = 1#v + (2#v ?: 0); }
    |
    ;

t returns [Int v]
    : f d { v = 0#v * (1#v ?: 1); }
    ;

d returns [Int? v]
    : MULT f d { v = 1#v * (2#v ?: 1); }
    | { v = 1; }
    ;

f returns [Int v]
    : N { v = 0#text.toInt(); }
    | LBR start RBR { v = 1#v; }
    ;

PLUS: '+';
MULT: '*';
LBR: '(';
RBR: ')';
N: '0'..'9';

