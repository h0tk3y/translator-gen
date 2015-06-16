grammar Arithm;

start: t e;
e : PLUS t e | ;
t: f d;
d: MULT f d | ;
f: N | LBR start RBR;

PLUS: '+';
MULT: '*';
LBR: '(';
RBR: ')';
N: '0'..'9';