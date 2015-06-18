grammar UserGrammar;

grammarSpec
    : GRAMMAR ID SEMI ruleSpec+ EOF
    ;

ruleSpec
    : ID returnsSpec? COLON body (OR body)* SEMI
    ;

returnsSpec
    : RETURNS LSQ oneReturn ( COMMA oneReturn )* RSQ
    ;

oneReturn
    : type=ID name=ID
    ;

body: literal block?
    | ID+ block?
    |
    ;

literal
    : APO str APO                               #stringLiteral
    | APO strChar APO                           #charLiteral
    | APO strChar APO DOT DOT APO strChar APO   #rangeLiteral
    | LBR literal RBR (AST | PLUS)              #nfLiteral
    | literal OR literal                        #orLiteral
    ;

block
    : LCU str RCU;

str: ~('\r' | '\n' | '"' | '\'') (~('\r' | '\n' | '"' | '\''))+?;
strChar: (~('\r' | '\n' | '"' | '\'')|':');

GRAMMAR: 'grammar';

COLON: ':';
OR: '|';
SEMI: ';';
APO: '\'';
DOT: '.';

LBR: '(';
RBR: ')';
AST: '*';
PLUS: '+';

LCU: '{';
RCU: '}';

LSQ: '[';
RSQ: ']';
RETURNS: 'returns';
ASG: '=';
COMMA: ',';

ID: ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_'|'?')*;

RE: '\r' -> skip;
BR: '\n' -> skip;
WS: [ \r\n\t] -> skip;

ANYCHAR: .;