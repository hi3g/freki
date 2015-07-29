grammar QueryStatements;

@header {
	package se.tre.freki.query;
}

query 	: type (function|metric) fields (where)?;

type	:	('SELECT'|'select');

function	: functiontype '(' (function|metric) ')';

functiontype 	: ('SUM'|'sum')
				| ('MAX'|'max')
				| ('MIN'|'min')
				| ('AVG'|'avg')
				;

metric	:	 Identifier;



fields 	: '{' tagpairs (',' tagpairs)* '}';

tagpairs	: tagkey operator tagvalue;

tagkey : Identifier ('|' Identifier)*;

tagvalue : Identifier ('|' Identifier)*;

where 	: ('WHERE'|'where') expr (operation expr)*;

operation	: (('AND'|'and')|('OR'|'or'));

expr	: Identifier EQUALSS Identifier
		| Identifier EQUALSS Identifier
		| Identifier LESS Identifier
		| Identifier MORE Identifier
		| Identifier NOTEQUALS Identifier
		;

operator	: (EQUALS|NOTEQUALS);
Identifier 	: ALPHANUMERIC;

fragment ALPHANUMERIC	: (ALLOWEDCHARS)+;
fragment ALLOWEDCHARS : '-'
			| '_'
			| '.'
			| INT
			| ALPHA
			;

fragment ALPHA	: [a-zA-Z]+;
fragment INT	: [0-9]+;

LESS		:	'<';
MORE		:	'>';
EQUALSS		:	'==';
EQUALS		:	'=';
NOTEQUALS 	:	'!=';

WS 	: [ \t\r\n]+ -> skip;
