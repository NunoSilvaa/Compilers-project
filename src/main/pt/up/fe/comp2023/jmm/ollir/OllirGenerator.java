package pt.up.fe.comp2023.jmm.ollir;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
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

        if(jmmNode.getJmmChild(0).getKind().equals("BinaryOp")){
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
        } else {
            if (!(jmmNode.getJmmChild(0).getKind().equals("MethodCall"))) {
                switch (jmmNode.getJmmChild(0).getKind()) {
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

            System.out.println("assignament" + jmmNode.getJmmChild(0));

            if (jmmNode.getJmmChild(0).getKind().equals("MethodCall")) {

                ollirCode.append("\t\t" + "invokevirtual(").append(jmmNode.getJmmChild(0).get("methodCallName")).append(" ")
                        .append(getOllirStringType(jmmNode.getJmmChild(0).get("methodCallName")))
                        .append(", \"<init>\");\n");
            }
        }

        /*System.out.println("name:" + jmmNode.get("assignmentName"));
        System.out.println("code: " + exprCode.visit(jmmNode.getJmmChild(0)));
        Type rawVarType = symbolTable.getFieldType(jmmNode.get("value"));
        var varType = (rawVarType != null ? typeToOllir(rawVarType.getName(), rawVarType.isArray()) : typeToOllir(getLocalVarType(jmmNode, jmmNode.get("value")),false));
        var lhsCode = new ExprCodeResult("", jmmNode.get("value") + varType);
        var rhsCode = exprCode.visit(jmmNode.getJmmChild(0));

        var code = new StringBuilder();
        code.append(lhsCode.prefixCode());
        code.append(rhsCode.prefixCode());

        if(rawVarType != null) code.append("putfield(this, " + lhsCode.value() + ", " + rhsCode.value() + ").V;\n");
        else code.append(lhsCode.value()).append(" :=").append(varType).append(" ").append(rhsCode.value()).append(";\n");

        ollirCode.append(code);*/

        return ollirCode.toString();
    }

    private String dealWithMethodCall(JmmNode jmmNode, String s) {
        String methodName = jmmNode.get("methodCallName");
        String returnType = ".V";

        if (symbolTable.getMethods().contains(methodName))
            for (String m : symbolTable.getMethods())
                if (m == methodName)
                    returnType = OllirUtils.getOllirType(symbolTable.getReturnType(m));
                else
                    returnType = ".V";

        if (jmmNode.getJmmChild(0).getKind().equals("This")){
            ollirCode.append("\t\tinvokevirtual(");

        }
        else {
            if (symbolTable.getImports().contains(jmmNode.getJmmChild(0).get("value")))
                ollirCode.append("\t\tinvokestatic(");
            else
                ollirCode.append("\t\tinvokevirtual(");
        }

        ollirCode.append(jmmNode.getJmmChild(0).get("value")).append(" , \"").append(methodName).append("\"");
        if(jmmNode.getNumChildren() > 1) {
            int i = 0;
            //JmmNode params = jmmNode.getJmmChild(1);
            for (var child : jmmNode.getChildren()) {
                if(i == 0) {
                    i++;
                    continue;
                }
                ollirCode.append(" , ");
                var param = exprCode.visit(child);
                ollirCode.append(param.value()).append(getOllirStringType(getType(jmmNode,param.value())));

            }
        }

        //System.out.println("identifier:" + jmmNode.getJmmChild(0).get("value"));
        ollirCode.append(")").append(returnType).append(";\n");
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

        System.out.println(symbolTable.getImports());
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