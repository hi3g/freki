
lexer grammar SelectLexer;

@header {
package se.tre.freki.query;
}

SELECT_KEYWORD      : 'SELECT';
BETWEEN_KEYWORD     : 'BETWEEN' -> pushMode(TimeMode);

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

AND             : 'AND';
NOW             : 'NOW';
AGO             : 'AGO';

WEEK            :'w';
DAY             :'d';
HOUR            :'H';
MINUTE          :'M';
SECOND          :'s';

DIGIT           : [0-9]+;

EOS : ';' WS2* EOF
    | WS2* EOF
    ;

WS2  : [ \t\r\n]+ -> skip;
