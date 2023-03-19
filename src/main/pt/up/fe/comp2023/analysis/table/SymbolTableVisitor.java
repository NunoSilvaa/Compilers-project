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
        addVisit("ImportDeclaration", this::dealWithImport);
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

    private String dealWithImport(JmmNode jmmNode, String s) {
        ArrayList<String> valuesList = (ArrayList<String>) jmmNode.getObject("name");
        String importFull = String.join(".", valuesList);
        symbolTable.setImports(importFull);
        return s + "IMPORT";
    }

    private String dealWithMethodDeclaration(JmmNode jmmNode, String s) {
        // Get the name of the method from the "methodName" object
        this.scope = "METHOD";
        if (jmmNode.getKind().equals("MainDeclaration")) { // MainDeclaration
            this.scope = "MAIN";
            this.symbolTable.addMethod("main", new Type("void", false));

        } else { // MethodDeclaration
            String methodName = (String) jmmNode.getObject("methodName");
            for (JmmNode parameterNode : jmmNode.getChildren()) {
                if (parameterNode.getKind().equals("RetType")) {
                    Type retType = ImplementedSymbolTable.getType(parameterNode.getChildren().get(0), "ty");
                    this.symbolTable.addMethod(methodName, retType);

                } else if (parameterNode.getKind().equals("RetExpr")) {
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
        return s + "CLASS_DECLARATION";
    }

    private String dealWithParameters(JmmNode jmmNode, String s) {
        if (scope.equals("METHOD")) {

            var parameterType = jmmNode.getChildren().get(0);
            var parameterValue = (String) jmmNode.getObject("parameterName");

            Type type = ImplementedSymbolTable.getType(parameterType, "ty");

            Symbol symbol = new Symbol(type, parameterValue);
            this.symbolTable.getCurrentMethod().setParameters(symbol);
        }
        return s + "PARAMETER";
    }

    public String dealWithVarDeclaration(JmmNode jmmNode, String s) {
        String name = jmmNode.get("varName");
        String type = jmmNode.getJmmChild(0).get("ty");

        Type t = ImplementedSymbolTable.getType(jmmNode.getChildren().get(0), "ty");

        Symbol symbol = new Symbol(t, name);

        symbolTable.setField(symbol, false);

        return s + "VAR_DECLARATION";
    }


}
