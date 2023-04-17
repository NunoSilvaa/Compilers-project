package pt.up.fe.comp2023.jmm.ollir;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        /*addVisit("VarDeclaration", this::dealWithVarDeclaration);
        addVisit("Parameter",this::dealWithParameters);*/

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

   private String dealWithMethodDeclaration(JmmNode jmmNode, String s) {
       /*var methodName = "";
       List<JmmNode> statements;

       boolean isMain = jmmNode.getKind().equals("MainDeclaration");

       if (isMain) {
           methodName = "main";
           ollirCode.append(getIndentation()).append(".method public static ").append(methodName).append("(");

           //statements = jmmNode.getJmmChild(0).getChildren();

       } else {
           // First child of MethodDeclaration is MethodHeader
           //JmmNode methodHeader = jmmNode.getJmmChild(0);
           methodName = (String) jmmNode.getObject("methodName");

           ollirCode.append(getIndentation()).append(".method public ").append(methodName).append("(");

           //statements = jmmNode.getJmmChild(1).getChildren();
       }

       var params = symbolTable.getParameters(methodName);

       System.out.println(params);

       var paramCode = params.stream()
               .map(OllirUtils::getCode).
               collect(Collectors.joining(", "));

       ollirCode.append(paramCode).append(")");
       //ollirCode.append(OllirUtils.getOllirType(symbolTable.getReturnType(methodName)));

       ollirCode.append(" {\n");

       this.incrementIndentation();

       for (var child : statements) {
           visit(child);
       }

       // return
       String returnString, returnReg;
       if (isMain) {
           returnString = ".V";
           returnReg = "";
       } else {
           returnString = OllirUtils.getOllirType(symbolTable.getReturnType(methodName)) + " ";
           System.out.println("linha 105" + symbolTable.getReturnType(methodName));
           //returnReg = visit(jmmNode.getJmmChild(2).getJmmChild(0), String.valueOf(new OllirInference(returnString, true)));
       }
       ollirCode.append(getIndentation()).append("ret").append(returnString).append(";\n");

       this.decrementIndentation();

       ollirCode.append(getIndentation()).append("}\n");*/

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
            ollirCode.append(getIndentation()).append(".field ").append(field.getName()).append(OllirUtils.getOllirType(field.getType())).append(";\n");
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

        this.decrementIndentation();

        ollirCode.append(getIndentation()).append("}\n");
        return "";
    }


    private String dealWithImport(JmmNode node, String s){
        for(String a : symbolTable.getImports()){
            s += "import " + a + ";\n";

        }
        return s;
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
