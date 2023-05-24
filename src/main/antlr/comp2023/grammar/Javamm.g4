grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

SLC : '//' ~[\n]* -> skip;
MLC : '/*' .*? '*/' -> skip;
BOOLEAN : ('true' | 'false') ;
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
    : ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' methodName=ID ')' '{' ( localVariables )* ( statement )* '}' #MainDeclaration
    | ('public')? retType methodName=ID '(' ( parameter ( ',' parameter )* )? ')' '{' ( localVariables | statement )* 'return' retExpr ';' '}'#MetDeclaration
    ;

localVariables
    : type varName=ID ';'
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
    | 'this' '.' assignmentName=ID '=' expression ';' #ThisAssignment
    | bracketsAssignmentName=ID '[' expression ']' '=' expression ';' #BracketsAssignment
    | 'if' '(' expression ')' statement 'else' statement #IfElseStatement
    | 'while' '(' expression ')' statement #While
    | '{' ( statement )* '}' #CurlyBracesStatement
    ;

expression
    : '(' expression ')' #Parenthesis
    | expression '[' expression ']' #ArrayAccessChain
    | expression '.' 'length' #Length
    | expression '.' methodCallName=ID '(' (expression (',' expression)*)? ')' #MethodCall
    | '!' expression #Negation
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('<' | '>' | '<=' | '>=' | '==') expression #BinaryOp
    | expression op=('&&' | '||') expression #BinaryOp
    | 'new' type '[' expression ']' #NewArray
    | 'new' value=ID '(' (expression (',' expression)*)? ')' #NewObject
    | classDeclaration #ClassExpression
    | expression '.' accessName=ID  #MemberAccess
    | value=INTEGER #Integer
    | value=BOOLEAN #Boolean
    | value=ID #Identifier
    | 'this' #This
    ;

