
parser grammar SelectParser;

options { tokenVocab=SelectLexer; }

@header {
package se.tre.freki.query;
}

query           : SELECT_KEYWORD (function|qualifier) BETWEEN_KEYWORD startTime=timestamp AND endTime=timestamp EOS*;

function        : functionName=FUNCTION_NAME FUNCTION_START (function|qualifier) FUNCTION_STOP;

qualifier       : metric=LABEL_NAME QUALIFIER_TAG_START tags+=tag (QUALIFIER_TAG_SEP tags+=tag)* QUALIFIER_TAG_STOP;

tag             : tagKey=tagQualifier operator tagValue=tagQualifier;

operator        : (EQUALS|NOTEQUALS);

tagQualifier    : simpleTag
                  | alternatingTag
                  | wildcardTag ;

simpleTag       : LABEL_NAME ;

alternatingTag  : LABEL_NAME (QUALIFIER_TAG_ALT LABEL_NAME)*;

wildcardTag     : QUALIFIER_TAG_WILD;

timestamp       : since
                | absolute
                | now;

since           : (week=DIGIT WEEK)? (day=DIGIT DAY)? (hour=DIGIT HOUR)? (minute=DIGIT MINUTE)? (second=DIGIT SECOND)?  AGO;

now             : NOW;

absolute        : DIGIT;
