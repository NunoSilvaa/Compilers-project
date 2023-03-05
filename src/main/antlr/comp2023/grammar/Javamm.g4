grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

program: importDeclaration* expression EOF;

importDeclaration: 'import' ID ( '.' ID )* ';';

classDeclaration: 'class' ID ('extends' ID)? '{' (varDeclaration)* (methodDeclaration)* '}';

varDeclaration: type ID ';';

methodDeclaration
    : ('public')? type ID '(' ( parameter ( ',' parameter )* )? ')' '{' ( varDeclaration )* ( statement )* 'return' expression ';' '}'
    | ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' ID ')' '{' ( varDeclaration )* ( statement )* '}'
    | ('public')? type ID '(' ( parameter ( ',' parameter )* )? ')' '{' '}'
    ;

parameter: type ID;

type
    : 'int' '[' ']'
    | 'boolean'
    | 'int'
    | 'void'
    | 'String'
    | ID
    ;

statement
    : expression ';'
    | ID '=' INTEGER ';'
    | ID '=' expression ';'
    | ID '[' expression ']' '=' expression ';'
    | 'if' '(' expression ')' ('{' ( statement )* '}')? ('else' '{' ( statement )* '}')?
    | 'if' '(' expression ')' statement 'else' statement
    | 'while' '(' expression ')' '{' ( statement )* '}'
    | 'while' '(' expression ')' statement
    | '{' (statement)* '}'
    ;

expression
    : '(' expression ')' #BinaryOp
    | expression '[' expression ']' #ArrayAccessChain
    | '!' expression #BinaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=( '<' | '>') expression #BinaryOp
    | expression op=('&&' | '||') expression #BinaryOp
    | expression '.' 'length' #Length
    | 'new' type '[' expression ']' #NewArray
    | 'new' ID '(' (expression (',' expression)*)? ')' #NewObject
    | classDeclaration #ClassExpression
    | expression '.' ID  #MemberAccess
    | expression '.' ID '(' (expression (',' expression)*)? ')' #MethodCall
    | value=INTEGER #Integer
    | value=ID #Identifier
    ;