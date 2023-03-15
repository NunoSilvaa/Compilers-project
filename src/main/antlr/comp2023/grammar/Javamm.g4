grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

SLC : '//' ~[\n]* ;
MLC : '/' .? '*/' ;
INTEGER : [0] | [1-9][0-9]* ;
ID : [a-zA-Z$_][a-zA-Z0-9_]* ;
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
    : expression ';' #ExpressionStatement
    | ID '=' expression ';' #Assignment
    | ID '[' expression ']' '=' expression ';' #BracketsAssignment
    | 'if' '(' expression ')' statement 'else' statement #IfElseStatement
    | 'while' '(' expression ')' statement #While
    | '{' ( statement )* '}' #CurlyBracesStatement
    | NEWLINE #NewLine
    ;

expression
    : '(' expression ')' #Parenthesis
    | expression '[' expression ']' #ArrayAccessChain
    | '!' expression #BinaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('<' | '>') expression #BinaryOp
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