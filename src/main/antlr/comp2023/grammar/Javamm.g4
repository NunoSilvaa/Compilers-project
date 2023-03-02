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
  : ('public')? type ID '(' ( type ID ( ',' type ID )* )? ')' '{' ( varDeclaration )* ( statement )* 'return' expression ';' '}'
  | ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' ID ')' '{' ( varDeclaration )* ( statement )* '}'
  ;

type
      : 'int' '[' ']'
      | 'boolean'
      | 'int'
      | ID
      ;

statement
    : expression ';'
    | ID '=' INTEGER ';'
    | ID '=' expression ';'
    | ID '[' expression ']' '=' expression ';'
    | 'if' '(' expression ')' statement 'else' statement
    | 'while' '(' expression ')' statement
    ;

expression
    : '(' expression ')' #BinaryOp
    | expression '[' expression ']' #BinaryOp
    | '!' expression #BinaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=( '<' | '>') expression #BinaryOp
    | expression op=('&&' | '||') expression #BinaryOp
    | expression '.' 'length' #Length
    | classDeclaration #ClassExpression
    | value=INTEGER #Integer
    | value=ID #Identifier
    ;