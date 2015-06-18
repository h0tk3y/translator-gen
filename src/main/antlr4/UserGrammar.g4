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
    | block?
    ;

literal
    : literal (PLUS|AST|QUE)+ #quantLit
    | literal OR literal       #orLit
    | LPAREN literal RPAREN   #parensLit
    | APO str APO             #stringLit
    | APO strChar APO         #charLit
    | APO ANYCHAR APO DOT DOT APO ANYCHAR APO #rangeLit
    ;

block
    : ACTION;

str: ~('\r' | '\n' | '"' | '\'') (~('\r' | '\n' | '"' | '\''))+?;
strChar: (~('\r' | '\n' | '"' | '\'')|':');

ACTION
	:	'{'
		(	ACTION
        |	'/*' .*? '*/' // ('*/' | EOF)
        |	'//' ~[\r\n]*
        |	.
		)*?
		('}'|EOF)
	;

GRAMMAR: 'grammar';

COLON: ':';
OR: '|';
SEMI: ';';
APO: '\'';
DOT: '.';

LCU: '{';
RCU: '}';

LPAREN: '(';
RPAREN: ')';

PLUS: '+';
AST: '*';
QUE: '?';

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