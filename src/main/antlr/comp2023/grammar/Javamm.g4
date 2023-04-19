grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

SLC : '//' ~[\n]* -> skip;
MLC : '/*' .*? '*/' -> skip;
INTEGER : [0] | [1-9][0-9]* ;
ID : [a-zA-Z$_][a-zA-Z0-9_]*;
WS : [ \n\t\r\f]+ -> skip ;


program
    : (importDeclaration)* (classDeclaration) EOF
    ;

importDeclaration: 'import' name+=ID ( '.' name+=ID )* ';';

classDeclaration: 'class' className=ID ('extends' superClassName=ID)? '{' (varDeclaration)* (methodDeclaration)* '}'? ';'? ;

varDeclaration: type varName=ID ';';

methodDeclaration
    : ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' parameterName=ID ')' '{' ( localVariables )* ( statement )* '}' #MainDeclaration
    | ('public')? retType methodName=ID '(' ( parameter ( ',' parameter )* )? ')' '{' ( localVariables )* ( statement )* 'return' retExpr ';' '}'#MetDeclaration
    ;

localVariables
    : type varName=ID ';'
    | varName=ID ('=' (ID | INT)) ';'
    ;

parameter: type parameterName=ID;

retType: type;

retExpr: expression;

type
    : ty='int' ( isArray='[' ']' )?
    | ty='int'
    | ty='boolean'
    | ty='String'
    | ty=ID
    ;

statement
    : expression ';' #ExpressionStatement
    | assignmentName=ID '=' expression ';' #Assignment
    | ID '[' expression ']' '=' expression ';' #BracketsAssignment
    | 'if' '(' expression ')' statement 'else' statement #IfElseStatement
    | 'while' '(' expression ')' statement #While
    | '{' ( statement )* '}' #CurlyBracesStatement
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
    | expression '.' methodCallName=ID '(' (expression (',' expression)*)? ')' #MethodCall
    | value=INTEGER #Integer
    | value=ID #Identifier
    | 'this' #This
    ;

