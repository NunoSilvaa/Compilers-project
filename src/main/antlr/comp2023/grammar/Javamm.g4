grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;
NEWLINE : '\n';
WS : [ \t\r\f]+ -> skip ;


program
    : (importOrClassDeclaration | statement)* EOF
    ;


importOrClassDeclaration
    : importDeclaration
    | classDeclaration
    ;

importDeclaration: 'import' subImport ( '.' subImport )* ';';

subImport: ID;

classDeclaration: 'class' className=ID ('extends' superClassName=ID)? '{' (varDeclaration)* (methodDeclaration)* '}'? ';'? ;

varDeclaration: type varName=ID ';';

methodDeclaration
    : ('public')? type methodName=ID '(' ( parameter ( ',' parameter )* )? ')' '{' ( varDeclaration )* ( statement )* 'return' expression ';' '}'
    | ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' parameterName=ID ')' '{' ( varDeclaration )* ( statement )* '}'
    ;

parameter: type parameterName=ID;

type
    : 'int[]'
    | 'boolean'
    | 'int'
    | 'String'
    | ID
    ;

statement
    : expression ';'
    | ID '=' INTEGER ';'
    | ID '=' expression ';'
    | ID '[' expression ']' '=' expression ';'
    | 'if' '(' expression ')' ('{' ( statement )* '}')? ('else' '{' ( statement )* '}')
    | 'if' '(' expression ')' statement 'else' statement
    | 'while' '(' expression ')' '{' ( statement )* '}'
    | 'while' '(' expression ')' statement
    | '{' (statement)* '}'
    | NEWLINE
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