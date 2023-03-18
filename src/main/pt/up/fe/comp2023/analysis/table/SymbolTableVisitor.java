package pt.up.fe.comp2023.analysis.table;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmSerializer;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SymbolTableVisitor extends PreorderJmmVisitor<String, String> {
    private ImplementedSymbolTable symbolTable;
    private List<Report> reports;

    private String scope;

    @Override
    protected void buildVisitor(){
        addVisit("Program", this::dealWithProgram);
        addVisit("ImportDeclaration", this::dealWithImport);
        //addVisit("MainDeclaration", this::dealWithMainDeclaration);
        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("VarDeclaration", this::dealWithVarDeclaration);
        addVisit("Parameter",this::dealWithParameters);

        setDefaultVisit(this::defaultVisit);
    }

    public ImplementedSymbolTable getSymbolTable(JmmNode node) {
        visit(node, null);
        return this.symbolTable;
    }
    public SymbolTableVisitor () {
        this.symbolTable = new ImplementedSymbolTable();
    }

    private String defaultVisit(JmmNode jmmNode, String s){
        String ret = s + jmmNode.getKind();
        String attributes = jmmNode.getAttributes()
                .stream()
                .filter(a->!a.equals("line"))
                .map(a->a + "="+ jmmNode.get(a))
                .collect(Collectors.joining(", ", "[", "]"));

        ret += ((attributes.length() > 2) ? attributes : "");
        return ret;
    }

    private String dealWithProgram(JmmNode node, String s) {
        //s = ((s !=null) ? s : "");
        return null;
    }

    private String dealWithImport(JmmNode jmmNode, String s) {
        ArrayList<String> valuesList = (ArrayList<String>) jmmNode.getObject("name");
        String importFull = String.join(".", valuesList);
        symbolTable.addImports(importFull);
        //System.out.println(jmmNode);
        return s + "IMPORT";
    }

    /*private String dealWithMainDeclaration(JmmNode jmmNode,String s) {
     return null;
    }*/

    private String dealWithMethodDeclaration(JmmNode jmmNode, String s) {
        // Get the name of the method from the "methodName" object
        this.scope = "METHOD";
        //System.out.println("ESTOU AQUI linha 74 ");
        System.out.println(jmmNode.getKind());
        if (jmmNode.getKind().equals("MainDeclaration")) { // MainDeclaration
            System.out.println("ESTOU AQUI linha 77");
            this.scope = "MAIN";
            this.symbolTable.addMethod("main", new Type("void", false));
            jmmNode.put("params","");

        } else { // MethodDeclarationOther
            //System.out.println(jmmNode.getKind());
            this.scope = "METHOD";
            String methodName = (String) jmmNode.getObject("methodName");
            //System.out.println(methodName);
            for (JmmNode parameterNode : jmmNode.getChildren()) {
                System.out.println("line 88:" + parameterNode.getKind());
                if (parameterNode.getKind().equals("RetType")) {
                    System.out.println("linha 90");
                    Type retType = ImplementedSymbolTable.getType(parameterNode.getChildren().get(0), "ty");
                    this.symbolTable.addMethod(methodName, retType);
                    System.out.println("linha 93:" + parameterNode.getKind());

                } else if (parameterNode.getKind().equals("RetExpr")) {
                    System.out.println("linha 96");
                    continue; // ignore
                } else {
                    visit(parameterNode);
                }
            }
        }
        return s + "METHOD_DECLARATION";
    }

    private String dealWithClassDeclaration(JmmNode jmmNode, String s) {
        // Get the name of the class from the "className" object
        String className = (String) jmmNode.getObject("className");
        symbolTable.setClassName(className);
        // Check if the class has a superclass
        boolean hasSuperclass = jmmNode.hasAttribute("superClassName");
        if(hasSuperclass){
            String superClassName = (String) jmmNode.getObject("superClassName");
            symbolTable.setSuper(superClassName);
        }
        //return s + "CLASS_DECLARATION";
        return null;
    }

    private String dealWithParameters(JmmNode jmmNode, String s) {

        /*Type type = symbolTable.getType(jmmNode.getChildren().get(0), "ty");
        Symbol symbol = new Symbol(type, jmmNode.get("parameterName"));

        symbolTable.getCurrentMethod().setParameters(symbol);
        System.out.println("hererrererererer");
        return null;*/

        s = ((s != null) ? s : "");

        if (scope.equals("METHOD")) {

            var parameterType = jmmNode.getChildren().get(0);
            var parameterValue = (String) jmmNode.getObject("parameterName");

            Type type = ImplementedSymbolTable.getType(parameterType, "ty");

            Symbol symbol = new Symbol(type, parameterValue);
            //System.out.println("hererrere");
            this.symbolTable.getCurrentMethod().setParameters(symbol);
        }
        return null;
    }

    public String dealWithVarDeclaration(JmmNode node, String s) {
        s = ((s != null) ? s : "");

        if (scope.equals("METHOD")) {
            System.out.println("hererrdewjkrvdbewnjfdfhdeweqdfgergwerdfere");
            if (node.getChildren().size() > 0) {
                String variableName = node.get("varName");

                Type localVarType = ImplementedSymbolTable.getType(node.getChildren().get(0), "ty");

                Symbol localVarSymbol = new Symbol(localVarType, variableName);
                this.symbolTable.getCurrentMethod().setLocalVariable(localVarSymbol);
            } else {
                String variableName = node.get("varName");

                Symbol localVarSymbol = new Symbol(new Type("", false), variableName);
                this.symbolTable.getCurrentMethod().setLocalVariable(localVarSymbol);
            }
        }

        return null;
    }


}
