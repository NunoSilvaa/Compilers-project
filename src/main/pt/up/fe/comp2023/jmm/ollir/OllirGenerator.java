package pt.up.fe.comp2023.jmm.ollir;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.*;
import java.util.stream.Collectors;

import static pt.up.fe.comp2023.jmm.ollir.OllirUtils.*;

public class OllirGenerator extends AJmmVisitor<String, String> {
    private final StringBuilder ollirCode;
    private SymbolTable symbolTable;
    private int indentationLevel = 0;

    public OllirGenerator(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.ollirCode = new StringBuilder();
        this.indentationLevel = 0;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithImport);
        //addVisit("ImportDeclaration", this::dealWithImport);
        addVisit("MainDeclaration", this::dealWithMethodDeclaration);
        addVisit("MetDeclaration", this::dealWithMethodDeclaration);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("Assignment", this::dealWithAssigments);
        addVisit("ExpressionStatement", this::dealWithExpression);
        addVisit("This",this::dealWithThis);


        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithExpression(JmmNode jmmNode,  String s) {

        return "";
    }

    public String getCode() {
        return ollirCode.toString();
    }

    private void incrementIndentation() {
        this.indentationLevel++;
    }

    private void decrementIndentation() {
        this.indentationLevel--;
    }

    private String getIndentation() {
        return "\t".repeat(indentationLevel);
    }

    private String dealWithAssigments(JmmNode assignNode, String s) {

        for( var child : assignNode.getChildren()){
            switch (assignNode.getJmmChild(0).getKind()){
                case("Integer"):
                    ollirCode.append(getIndentation()).append(assignNode.get("name")).append(".i32 :=.i32 ").append(assignNode.getJmmChild(0).get("value")).append(".i32;\n");
                    break;
                case("Boolean"):
                    ollirCode.append(getIndentation()).append(assignNode.get("name")).append(".bool :=.bool ").append(getBooleanValue(assignNode.getJmmChild(0).get("value"))).append(".bool;\n");
                    break;
                default:
                    var type = getOllirStringType(getType(assignNode,assignNode.get("name")));
                    ollirCode.append(getIndentation()).append(assignNode.get("name")).append(type).append(" :=").append(type).append(" ")
                            .append(assignNode.getJmmChild(0).get("value")).append(type).append(";\n");
                    break;
            }
        }

        for (var child : assignNode.getChildren()) {
            visit(child);
        }
        return "";
    }

    private String getType(JmmNode jmmNode, String var) {
        JmmNode node = jmmNode;

        while (!node.getKind().equals("MetDeclaration")){
            if (node.getKind().equals("MainMethod")) {
                for(Symbol varType : symbolTable.getLocalVariables("main")){
                    if (varType.getName().equals(var)) return varType.getType().getName();
                }
                return "";
            }
            node = node.getJmmParent();
        }
        String methodName = node.get("methodName");

        for(Symbol varType : symbolTable.getLocalVariables(methodName)){
            if (varType.getName().equals(var))
                return varType.getType().getName();
        }
        return "";
    }

    private String dealWithMethodDeclaration(JmmNode jmmNode, String s) {
        var methodName = "";
        List<JmmNode> statements = new ArrayList<>();

        boolean isMain = jmmNode.getKind().equals("MainDeclaration");

        if(isMain){
            methodName = "main";
            ollirCode.append(getIndentation()).append(".method public static ").append(methodName).append("(");
            if(!jmmNode.getChildren().isEmpty()) statements = jmmNode.getJmmChild(0).getChildren();

        } else{
            methodName = jmmNode.get("methodName");
            ollirCode.append(getIndentation()).append(".method public ").append(methodName).append("(");
            statements = jmmNode.getChildren();
        }

        //System.out.println(symbolTable.getLocalVariables(methodName));
        //System.out.println("statements" + jmmNode.getJmmChild(0));
        var params = symbolTable.getParameters(methodName);

        if(params.size() != 0) {

            Collections.reverse(params);
            var paramCode = params.stream()
                    .map(OllirUtils::getCode).
                    collect(Collectors.joining(", "));

            //System.out.println("params" + params);
            ollirCode.append(paramCode);
        }
        ollirCode.append(")").append(getOllirType(symbolTable.getReturnType(methodName)));
        ollirCode.append(" {\n");

        this.incrementIndentation();


        /*for (var child : statements) {
            if(child.getKind().equals("LocalVariables")){
                //visit(child);
                ollirCode.append(getIndentation()).append(child.get("varName")).
                        append(";\n");
                System.out.println("statemnets" + child);
            }
        }*/

        for (var statement : statements) {
            visit(statement);
        }


        if(isMain){
            ollirCode.append(getIndentation()).append("ret.V;\n");
        }

        for( var child : jmmNode.getChildren()){
            if(child.getKind().equals("RetExpr")){
                ollirCode.append(getIndentation()).append("ret").append(getOllirType(symbolTable.getReturnType(methodName))).
                        append(" ").append(child.getJmmChild(0).get("value")).
                        append(getOllirType(symbolTable.getReturnType(methodName))).append(";\n");
            }
        }

        this.decrementIndentation();

        ollirCode.append(getIndentation()).append("}\n");
        return "";
    }

    private String dealWithClassDeclaration(JmmNode jmmNode, String s) {

        ollirCode.append(" ").append("public ").append(symbolTable.getClassName());
        var superClass = symbolTable.getSuper();

        if (superClass != null) {
            ollirCode.append(" extends ").append(superClass);
        }

        ollirCode.append(" {\n");

        this.incrementIndentation();

        for (var field : symbolTable.getFields()) {
            ollirCode.append(getIndentation()).append(".field ").append(field.getName()).append(getOllirType(field.getType())).append(";\n");
        }
        ollirCode.append("\n");

        // default constructor
        ollirCode.append(getIndentation()).append(".construct ").append(symbolTable.getClassName()).append("().V {\n");
        this.incrementIndentation();
        ollirCode.append(getIndentation()).append("invokespecial(this, \"<init>\").V;\n");
        this.decrementIndentation();
        ollirCode.append(getIndentation()).append("}\n");


        for (var child : jmmNode.getChildren().subList(symbolTable.getFields().size(), jmmNode.getNumChildren())) {
            ollirCode.append("\n");
            visit(child);
        }

        this.decrementIndentation();
        ollirCode.append(getIndentation()).append("}\n");
        return "";
    }


    private String dealWithImport(JmmNode jmmNode, String s){
        for(String a : symbolTable.getImports()){
            ollirCode.append("import ").append(a).append(";\n");
        }
        ollirCode.append("\n");

        for (var child : jmmNode.getChildren()) {
            visit(child);
        }
        return "";
    }

    private String defaultVisit(JmmNode jmmNode, String s){
        return "";
    }

    private String dealWithThis(JmmNode jmmNode, String s) {
        return "this." + symbolTable.getClassName();
    }

}