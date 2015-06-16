grammar UserGrammar;

grammarSpec
    : GRAMMAR ID SEMI ruleSpec+ EOF
    ;

ruleSpec
    : ID COLON body (OR body)* SEMI
    ;

body: literal
    | ID+
    |
    ;

literal
    : APO str APO                               #stringLiteral
    | APO strChar APO                           #charLiteral
    | APO strChar APO DOT DOT APO strChar APO   #rangeLiteral
    ;

str: (~('\r' | '\n' | '"' | '\'')|':') (~('\r' | '\n' | '"' | '\'')|':')+?;
strChar: (~('\r' | '\n' | '"' | '\'')|':');

GRAMMAR: 'grammar';

COLON: ':';
OR: '|';
SEMI: ';';
APO: '\'';
DOT: '.';

ID: ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;

RE: '\r' -> skip;
BR: '\n' -> skip;
WS: [ \r\n\t] -> skip;

ANYCHAR: .;