package pt.up.fe.comp2023.analysis.table;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmSerializer;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SymbolTableVisitor extends PreorderJmmVisitor<String, String> {
    private ImplementedSymbolTable symbolTable;
    private List<Report> reports =  new ArrayList<>();
    private String scope;

    @Override
    protected void buildVisitor(){
        addVisit("ImportDeclaration", this::dealWithImport);
        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("VarDeclaration", this::dealWithVarDeclaration);
        //addVisit("Parameter",this::dealWithParameters);

        setDefaultVisit(this::defaultVisit);
    }

    public Map<ImplementedSymbolTable, List<Report>> getSymbolTableAndReports(JmmNode node) {
        Map<ImplementedSymbolTable, List<Report>> tableAndReports = new HashMap<>();
        visit(node, null);
        tableAndReports.put(this.symbolTable, this.reports);
        return tableAndReports;
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
            List<Symbol> localVariables = new ArrayList<>();
            List<Symbol> parameters = new ArrayList<>();
            List<Symbol> assignments = new ArrayList<>();
            this.symbolTable.addMethod("main", new Type("void", false), localVariables, parameters, assignments);
        } else { // MethodDeclaration
            String methodName = (String) jmmNode.getObject("methodName");
            Method method = new Method(methodName);
            for (JmmNode parameterNode : jmmNode.getChildren()) {
                if (parameterNode.getKind().equals("RetType")) {
                    Type retType = ImplementedSymbolTable.getType(parameterNode.getChildren().get(0), "ty");
                    method.setReturnType(retType);
                } else if (parameterNode.getKind().equals("RetExpr")) {
                    continue; // ignore
                } else if (parameterNode.getKind().equals("LocalVariables")) {
                    for (JmmNode localVariable : parameterNode.getChildren()) {
                        Type variableType = ImplementedSymbolTable.getType(localVariable, "ty");
                        String variableName = (String) parameterNode.getObject("varName");
                        Symbol variableSymbol = new Symbol(variableType, variableName);
                        method.setLocalVariable(variableSymbol);
                    }
                } else if(parameterNode.getKind().equals("Parameter")){
                    var parameterType = parameterNode.getChildren().get(0);

                    Type type = ImplementedSymbolTable.getType(parameterType, "ty");
                    var parameterValue = (String) parameterNode.getObject("parameterName");

                    Symbol parameterSymbol = new Symbol(type, parameterValue);
                    method.setParameters(parameterSymbol);
                } else if(parameterNode.getKind().equals("Assignment")){
                    for(JmmNode assignmentNode : parameterNode.getChildren()){
                        Type assignmentType = new Type("boolean", false);
                        var assignmentName = (String) parameterNode.getObject("assignmentName");
                        if(assignmentNode.getKind().equals("Integer"))
                            assignmentType = new Type("int", false);
                        else if(assignmentNode.getKind().equals("NewObject")) {
                            for (Symbol localVariable : method.getLocalVariables()) {
                                if (localVariable.getName().equals(assignmentName))
                                    assignmentType = new Type(localVariable.getType().getName(), false);
                            }
                        }
                        else if(assignmentNode.getKind().equals("NewArray")){
                            for(Symbol localVariable : method.getLocalVariables()){
                                if(localVariable.getName().equals(assignmentName))
                                    assignmentType = new Type(localVariable.getType().getName(), true);
                            }
                            assignmentType = new Type("int", true);
                        }
                        else if(assignmentNode.getKind().equals("Identifier")){
                            for (Symbol localVariable : method.getLocalVariables()) {
                                if (localVariable.getName().equals(assignmentName)) {
                                    for(Symbol localVariable2 : method.getLocalVariables()){
                                        if(localVariable2.getName().equals(assignmentNode.get("value"))){
                                            if(localVariable2.getType().getName().equals(symbolTable.getClassName()) && !localVariable.getType().getName().equals(symbolTable.getSuper()))
                                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(assignmentNode.get("lineStart")), Integer.parseInt(assignmentNode.get("colEnd")), "Class does not extendsuperclass"));
                                        }

                                    }
                                    assignmentType = new Type(localVariable.getType().getName(), false);
                                }
                            }
                        }
                        for(Symbol localVariable : method.getLocalVariables()){
                            if(localVariable.getName().equals(assignmentName)) {
                                var localVariableType = localVariable.getType();
                                if (!localVariable.getType().getName().equals(assignmentType.getName()))
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(assignmentNode.get("lineStart")), Integer.parseInt(assignmentNode.get("colEnd")), "Assignment type mismatch"));

                            }
                        }
                        Symbol assignmentSymbol = new Symbol(assignmentType, assignmentName);
                        method.setAssignment(assignmentSymbol);

                    }
                    //var assignmentType = ImplementedSymbolTable.getType(parameterNode, "ty");
                }
            }
            for(Symbol localVariable : method.getLocalVariables()){
                var localVariableName = localVariable.getType().isArray();
                var methodAssignments = method.getAssignments();
                /*if(localVariable.getType().isArray() && !method.getAssignments().contains(localVariable))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "Assignment is not an array!"));*/
                if(!method.getAssignments().contains(localVariable))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "variable not assigned"));
            }
            this.symbolTable.addMethod(methodName, method.getReturnType(), method.getLocalVariables(), method.getParameters(), method.getAssignments());
        }
        System.out.println("Methods" + this.symbolTable.getMethods());
        return s + "METHOD_DECLARATION";
        /*this.scope = "METHOD";
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
        return s + "METHOD_DECLARATION";*/
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

    /*private String dealWithParameters(JmmNode jmmNode, String s) {
        if (scope.equals("METHOD")) {

            var parameterType = jmmNode.getChildren().get(0);
            var parameterValue = (String) jmmNode.getObject("parameterName");

            Type type = ImplementedSymbolTable.getType(parameterType, "ty");

            var name = this.symbolTable.getCurrentMethod().getName();
            System.out.println("Current " + this.symbolTable.getCurrentMethod().getName());
            Symbol symbol = new Symbol(type, parameterValue);
            this.symbolTable.getCurrentMethod().setParameters(symbol);
        }
        return s + "PARAMETER";
    }*/

    public String dealWithVarDeclaration(JmmNode jmmNode, String s) {
        String name = jmmNode.get("varName");
        String type = jmmNode.getJmmChild(0).get("ty");

        Type t = ImplementedSymbolTable.getType(jmmNode.getChildren().get(0), "ty");

        Symbol symbol = new Symbol(t, name);

        symbolTable.setField(symbol, false);

        return s + "VAR_DECLARATION";
    }

    public List<Report> getReports(JmmNode node) {
        visit(node, null);
        return this.reports;
    }
}
