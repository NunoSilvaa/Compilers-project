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
    private ExprToOllir code;
    private SymbolTable symbolTable;
    private int indentationLevel = 0;
    private final ExprToOllir exprCode;
    int counter;

    public OllirGenerator(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.ollirCode = new StringBuilder();
        this.indentationLevel = 0;
        this.exprCode = new ExprToOllir();
        this.counter = 0;
        this.code = new ExprToOllir();

    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithImport);
        addVisit("MainDeclaration", this::dealWithMethodDeclaration);
        addVisit("MetDeclaration", this::dealWithMethodDeclaration);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        //addVisit("Assignment", this::dealWithAssignments);
        addVisit("Assignment", this::dealWithExpression);
        //addVisit("BinaryOp",this::dealWithBinaryOp);
        addVisit("This", this::dealWithThis);
        addVisit("RetExpr", this::dealWithReturnStatements);
        setDefaultVisit(this::defaultVisit);
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

    private String dealWithExpression(JmmNode jmmNode,  String s) {

        if(jmmNode.getJmmChild(0).getKind().equals("BinaryOp")){
            //var lhsCode= exprCode.visit(jmmNode.getJmmChild(0));
            var lhsCode = new ExprCodeResult("",jmmNode.get("name"));
            var rhsCode= exprCode.visit(jmmNode.getJmmChild(0));

            ollirCode.append(lhsCode.prefixCode());
            ollirCode.append(rhsCode.prefixCode());

            switch (jmmNode.getJmmChild(0).getKind()){
                case ("Boolean"):
                    ollirCode.append(getIndentation()).append(jmmNode.get("name")).append(".bool :=.bool ").append(getBooleanValue(rhsCode.value())).append(".bool;\n");
                    break;
                default:
                    var type = getOllirStringType(getType(jmmNode, jmmNode.get("name")));
                    ollirCode.append(getIndentation()).append(jmmNode.get("name")).
                            append(type).append(" :=").append(type).append(" ").
                            append(rhsCode.value()).append(type).append(";\n");
                    break;
            }
        } else {
            switch (jmmNode.getJmmChild(0).getKind()){
                case ("Boolean"):
                    ollirCode.append(getIndentation()).append(jmmNode.get("name")).append(".bool :=.bool ").append(getBooleanValue(jmmNode.getJmmChild(0).get("value"))).append(".bool;\n");
                    break;
                default:
                    var type = getOllirStringType(getType(jmmNode, jmmNode.get("name")));
                    ollirCode.append(getIndentation()).append(jmmNode.get("name")).
                            append(type).append(" :=").append(type).append(" ").
                            append(jmmNode.getJmmChild(0).get("value")).append(type).append(";\n");
                    break;
            }
        }


        System.out.println("name:" + jmmNode.get("name"));
        System.out.println("code: " + exprCode.visit(jmmNode.getJmmChild(0)));

        return ollirCode.toString();
    }

    /*private String dealWithAssignments(JmmNode assignNode, String s) {
        switch (assignNode.getJmmChild(0).getKind()) {
            case ("Integer"):
                ollirCode.append(getIndentation()).append(assignNode.get("name")).append(".i32 :=.i32 ").append(assignNode.getJmmChild(0).get("value")).append(".i32;\n");
                break;
            case ("Boolean"):
                ollirCode.append(getIndentation()).append(assignNode.get("name")).append(".bool :=.bool ").append(getBooleanValue(assignNode.getJmmChild(0).get("value"))).append(".bool;\n");
                break;
            default:
                var type = getOllirStringType(getType(assignNode, assignNode.get("name")));
                ollirCode.append(getIndentation()).append(assignNode.get("name")).append(type).append(" :=").append(type).append(" ")
                        .append(assignNode.getJmmChild(0).get("value")).append(type).append(";\n");
                break;
        }

        System.out.println("assign" + assignNode.getJmmChild(0).getKind());

        for (var child : assignNode.getChildren()) {
            visit(child);
        }
        return "";
    }*/

    public String getType(JmmNode jmmNode, String var) {
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

        if (isMain) {
            methodName = "main";
            ollirCode.append(getIndentation()).append(".method public static ").append(methodName).append("(");
            if (!jmmNode.getChildren().isEmpty()) statements = jmmNode.getJmmChild(0).getChildren();

        } else {
            methodName = jmmNode.get("methodName");
            ollirCode.append(getIndentation()).append(".method public ").append(methodName).append("(");
            statements = jmmNode.getChildren();
        }

        //System.out.println(symbolTable.getLocalVariables(methodName));
        //System.out.println("statements" + jmmNode.getJmmChild(0));
        var params = symbolTable.getParameters(methodName);

        if (params.size() != 0) {

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

        for (var statement : statements) {
            visit(statement);
        }

        if (isMain) {
            ollirCode.append(getIndentation()).append("ret.V;\n");
        }
        /*for (var child : jmmNode.getChildren()) {
            if (child.getKind().equals("RetExpr")) {
                if (child.getJmmChild(0).getKind().equals("BinaryOp")) {
                    System.out.println("hereeeeee");
                    //ollirCode.append(getIndentation()).append("ret").append(getOllirType(symbolTable.getReturnType(methodName))).append(" ").append(child.getJmmChild(0).get("op")).append(getIndentation()).append(";\n");
                    Type ret = symbolTable.getReturnType(methodName);

                    if (child.getJmmChild(0).getKind().equals("Identifier")) {
                        ollirCode.append("\t\tret" + getOllirType(ret) + " " + child.getJmmChild(0) + getOllirType(ret)).append(";\n");
                    } else {
                        ollirCode.append(getIndentation()).append("ret").append(getOllirType(symbolTable.getReturnType(methodName))).
                                append(" ").append(child.getJmmChild(0).get("value")).
                                append(getOllirType(symbolTable.getReturnType(methodName))).append(";\n");
                    }
                }
            }
        }*/
        this.decrementIndentation();

        ollirCode.append(getIndentation()).append("}\n");
        return "";
    }

    private String dealWithReturnStatements(JmmNode jmmNode, String methodName) {
        //System.out.println("return statement: " + code.visit(jmmNode.getJmmChild(0)));
        if(jmmNode.getJmmChild(0).getKind().equals("BinaryOp")){
            System.out.println("return");
            var codeReturn = code.visit(jmmNode.getJmmChild(0));
            ollirCode.append(codeReturn.prefixCode());
            ollirCode
                    .append("\t\tret")
                    .append(".i32")
                    .append(" ");

            ollirCode.append(codeReturn.value());

            if(!codeReturn.value().contains(".")) {
                ollirCode.append(".i32");
            }
            ollirCode.append(";\n");
        } else {

            var codeReturn = code.visit(jmmNode.getJmmChild(0));

            if(!jmmNode.getJmmChild(0).getKind().equals("static void")){
                ollirCode.append(codeReturn.prefixCode());
                ollirCode
                        .append("\t\tret")
                        .append(".i32")
                        .append(" ");
                ollirCode.append(codeReturn.value()).append(".i32").append(";\n");
            } else {
                ollirCode.append(codeReturn.prefixCode());
                ollirCode.append(codeReturn.value());
            }
        }

        return ollirCode.toString();
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
        System.out.println("visiting: " + jmmNode.getKind());
        if(jmmNode.getNumChildren() == 0){
            return "";
        }
        return jmmNode.getChildren().stream().map(this::visit).collect(Collectors.joining());
    }

    private String dealWithThis(JmmNode jmmNode, String s) {
        return "this." + symbolTable.getClassName();
    }

}