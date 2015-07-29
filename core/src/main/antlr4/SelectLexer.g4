
lexer grammar SelectLexer;

SELECT_KEYWORD      : 'SELECT';
BETWEEN_KEYWORD	    : 'BETWEEN' -> pushMode(TimeMode);

LABEL_NAME          : [0-9a-zA-Z-./_]+ ;

FUNCTION_NAME       : [0-9A-Za-z]+ ;
FUNCTION_START      : '(' ;
FUNCTION_STOP       : ')' ;

QUALIFIER_TAG_START : '{' ;
QUALIFIER_TAG_STOP  : '}' ;
QUALIFIER_TAG_SEP   : ',' ;
QUALIFIER_TAG_ALT   : '|' ;
QUALIFIER_TAG_WILD  : '*' ;

EQUALS      :   '=';
NOTEQUALS   :   '!=';

WS  : [ \t\r\n]+ -> skip;

mode TimeMode;

TIMESTAMP	: [0-9]+ ;
AND             : 'AND' ;

EOS : ';' WS2* EOF
    | WS2* EOF
    ;

WS2  : [ \t\r\n]+ -> skip;
