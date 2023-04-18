package pt.up.fe.comp2023.jmm.ollir;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static pt.up.fe.comp2023.jmm.ollir.OllirUtils.getOllirType;

public class OllirGenerator extends PreorderJmmVisitor<String, String> {
    private final StringBuilder ollirCode;
    private ImplementedSymbolTable symbolTable;
    private String scope;
    private int indentationLevel = 0;
    private int tempVarNum;

    /*public String getOllirCode(JmmNode node) {
        return visit(node, "");
    }*/

    public OllirGenerator(ImplementedSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.ollirCode = new StringBuilder();
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithImport);
        //addVisit("ImportDeclaration", this::dealWithImport);
        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("VarDeclaration", this::dealWithStatements);
        //addVisit("Parameter",this::dealWithParameters);*/

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

    private String dealWithStatements(JmmNode jmmNode, String s){
        return "";
    }

    private String dealWithMethodDeclaration(JmmNode jmmNode, String s) {
        var methodName = "";
        //List<JmmNode> statements;

        boolean isMain = jmmNode.getKind().equals("MainDeclaration");

        ollirCode.append(".method public ");
        if(jmmNode.getKind().equals("MainDeclaration")){
            ollirCode.append("static ");
            methodName = "main";
        } else{
            methodName = jmmNode.get("methodName");
        }

        ollirCode.append(methodName).append("(");

        if(isMain) ollirCode.append("args.array.String");

        var params = symbolTable.getParameters(methodName);
        if(params.size() != 0) {

            Collections.reverse(params);
            var paramCode = params.stream()
                    .map(OllirUtils::getCode).
                    collect(Collectors.joining(", "));

            System.out.println("linha 84" + params);
            ollirCode.append(paramCode);
        }
        ollirCode.append(")").append(getOllirType(symbolTable.getReturnType(methodName)));
        ollirCode.append(" {\n");

        for( var statements : symbolTable.getLocalVariables(methodName)){

        }
        System.out.println(methodName);
        System.out.println("tem algo dentro:" + symbolTable.getLocalVariables(methodName));
        if(jmmNode.getKind().equals("MainDeclaration")){
            ollirCode.append("ret.V;\n");
        }
        ollirCode.append("}\n\n");

        return "";
    }




    private String dealWithClassDeclaration(JmmNode jmmNode, String s) {
        ollirCode.append(getIndentation()).append("public ").append(symbolTable.getClassName());
        var superClass = symbolTable.getSuper();

        if (superClass != null) {
            ollirCode.append(" extends ").append(superClass);
        }

        ollirCode.append(" {\n");

        this.incrementIndentation();

        // fields
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

        // methods
        for (var child : jmmNode.getChildren().subList(symbolTable.getFields().size(), jmmNode.getNumChildren())) {
            ollirCode.append("\n");
            visit(child);
        }
        System.out.println(jmmNode.getChildren().subList(symbolTable.getFields().size(), jmmNode.getNumChildren()));

        this.decrementIndentation();


        ollirCode.append(getIndentation()).append("}\n");
        return "";
    }


    private String dealWithImport(JmmNode node, String s){
        for(String a : symbolTable.getImports()){
            ollirCode.append("import ").append(a).append(";\n");

        }
        return "";
    }

    private String defaultVisit(JmmNode jmmNode, String s){
        /*String ret = s + jmmNode.getKind();
        String attributes = jmmNode.getAttributes()
                .stream()
                .filter(a->!a.equals("line"))
                .map(a->a + "="+ jmmNode.get(a))
                .collect(Collectors.joining(", ", "[", "]"));

        ret += ((attributes.length() > 2) ? attributes : "");
        return ret;*/
        return "";
    }


}