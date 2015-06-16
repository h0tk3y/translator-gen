grammar Regex;

start: t z;
z : OR start | ;
t: n x d;
d: t | ;
n: C | LBR start RBR;
x: | AST;

OR: '|';
AST: '*';
LBR: '(';
RBR: ')';
C: 'a'..'z';