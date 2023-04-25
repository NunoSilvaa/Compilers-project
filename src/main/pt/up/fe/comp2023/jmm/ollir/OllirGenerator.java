package pt.up.fe.comp2023.jmm.ollir;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable;

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
        addVisit("Assignment", this::dealWithExpression);
        addVisit("This", this::dealWithThis);
        addVisit("RetExpr", this::dealWithReturnStatements);
        addVisit("Boolean",this::dealWithBoolean);
        addVisit("MethodCall",this::dealWithMethodCall);
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

    private String dealWithBoolean(JmmNode jmmNode, String methodName) {
        return jmmNode.get("value") + getOllirStringType("boolean");
    }

    private String dealWithExpression(JmmNode jmmNode,  String s) {

        var lhsCode = new ExprCodeResult("", jmmNode.get("assignmentName"));
        var rhsCode = exprCode.visit(jmmNode.getJmmChild(0));

            ollirCode.append(lhsCode.prefixCode());
            ollirCode.append(rhsCode.prefixCode());

            switch (jmmNode.getJmmChild(0).getKind()) {
                case ("Boolean"):
                    ollirCode.append(getIndentation()).append(jmmNode.get("assignmentName")).append(".bool :=.bool ")
                            .append(getBooleanValue(rhsCode.value())).append(".bool;\n");
                    break;
                default:
                    var type = getOllirStringType(getType(jmmNode, jmmNode.get("assignmentName")));
                    ollirCode.append(getIndentation()).append(jmmNode.get("assignmentName")).
                            append(type).append(" :=").append(type).append(" ").
                            append(rhsCode.value()).append(type).append(";\n");
                    break;
            }
        /*} else {
            if(!(jmmNode.getJmmChild(0).getKind().equals("MethodCall"))){
                switch (jmmNode.getJmmChild(0).getKind()){
                    case ("Boolean"):
                        ollirCode.append(getIndentation()).append(jmmNode.get("assignmentName"))
                        .append(".bool :=.bool ")
                        .append(getBooleanValue(jmmNode.getJmmChild(0).get("value")))
                        .append(".bool;\n");
                        break;
                    default:
                        var type = getOllirStringType(getType(jmmNode, jmmNode.get("assignmentName")));
                        ollirCode.append(getIndentation()).append(jmmNode.get("assignmentName")).
                        append(type).append(" :=").append(type).append(" ").
                        append(jmmNode.getJmmChild(0).get("value")).append(type).append(";\n");
                        break;
                    }
                }
            }*/

        /*if(jmmNode.getJmmChild(0).getKind().equals("NewObject")){

            ollirCode.append("\t\t" + "invokespecial(").append(jmmNode.get("value"))
                    .append(getOllirStringType(jmmNode.get("value")))
                    .append(", \"<init>\").V;\n");
        }*/

        System.out.println("name:" + jmmNode.get("assignmentName"));
        System.out.println("code: " + exprCode.visit(jmmNode.getJmmChild(0)));
        return ollirCode.toString();
    }

    private String dealWithMethodCall(JmmNode method, String s) {
        //lida com métodos do import

        JmmNode methodCall = method.getJmmChild(0);
        JmmNode identifier = method.getJmmChild(1);
        System.out.println("methodCall" + methodCall);
        //System.out.println("methodCall:" + method.getJmmChild(0));
        //String idType = getOllirStringType(identifier.get("value"));
        StringBuilder code = new StringBuilder();

        if (identifier.getKind().equals("Identifier")) {
            code.append("\t\tinvokestatic("  + identifier.get("value")   + ", \"" + methodCall.get("value") + "\"");

            if (methodCall.getChildren().size() > 0)
                code.append(", ");
            boolean grandchildren = false;
            for (JmmNode grandchild : methodCall.getChildren()) {
                grandchildren = true;
                if(!grandchild.get("value").equals(identifier.get("value")))
                    code.append(visit(grandchild) + ", ");

            }
            if (grandchildren)
                code.delete(code.length() - 2, code.length());
            //code.append(")." + parseType(table.getMethod(methodCall.get("name")).getReturnType().getName()));

            code.append(").V; \n");
        } else {

            code.append("invokevirtual(").append(getOllirType((Type) symbolTable.getLocalVariables(identifier.get("value"))))
                    .append(getOllirType((Type) symbolTable.getLocalVariables(identifier.get("value"))))
                    .append( ", \"").append(methodCall.get("methodCallName"));

        }

        ollirCode.append(code);

        return "";
    }

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
            ollirCode.append(getIndentation()).append(".method public static ").append(methodName).append("(").append("args.array.String");
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
            visit(statement,methodName);
        }

        if (isMain) {
            ollirCode.append(getIndentation()).append("ret.V;\n");
        }

        this.decrementIndentation();

        ollirCode.append(getIndentation()).append("}\n");
        return "";
    }

    private String dealWithReturnStatements(JmmNode jmmNode, String methodName) {

        ExprCodeResult retStat = exprCode.visit(jmmNode.getJmmChild(0));
        var retType = getOllirType(symbolTable.getReturnType(methodName));
        ollirCode.append(retStat.prefixCode()).append(getIndentation())
                .append("ret").append(retType)
                .append(" ").append(retStat.value())
                .append(retType).append(";\n");

        return methodName;
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
        //System.out.println("visiting: " + jmmNode.getKind());
        if(jmmNode.getNumChildren() == 0){
            return "";
        }
        return jmmNode.getChildren().stream().map(this::visit).collect(Collectors.joining());
    }

    private String dealWithThis(JmmNode jmmNode, String s) {
        return "this." + symbolTable.getClassName();
    }

}