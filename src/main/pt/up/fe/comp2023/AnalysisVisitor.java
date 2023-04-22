package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable;
import pt.up.fe.comp2023.analysis.table.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class AnalysisVisitor extends PreorderJmmVisitor<String, String> {
    static final List<String> PRIMITIVES = Arrays.asList("int", "void", "boolean");
    static final List<String> ARITHMETIC_OP = Arrays.asList("ADD", "SUB", "MUL", "DIV");

    private ImplementedSymbolTable symbolTable;
    private List<Report> reports = new ArrayList<>();

    /*public AnalysisVisitor(){
        addVisit("Type", this::visitType);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("ReturnStatement", this::visitReturnStatement);
        addVisit("BinaryOp", this::visitBinaryOp);
    }*/

    @Override
    protected void buildVisitor() {
        addVisit("Type", this::visitType);
        addVisit("BracketsAssignment", this::visitBracketsAssignment);
        addVisit("Identifier", this::visitVariable);
        addVisit("IfElseStatement", this::visitIfElseStatement);
        addVisit("ArrayAccessChain", this::visitArrayAccessChain);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("While", this::visitWhileStatement);
        //addVisit("LocalVarsAndAssignment", this::visitLocalVarsAndAssignment);
        //addVisit("Assignment", this::visitAssignment);
        //addVisit("ClassDeclaration", this::visitClassDeclaration);
        //addVisit("ReturnStatement", this::visitReturnStatement);
        addVisit("BinaryOp", this::visitBinaryOp);

        setDefaultVisit(this::defaultVisit);
    }

    public ImplementedSymbolTable getSymbolTable(JmmNode node) {
        visit(node, null);
        return this.symbolTable;
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
    private String visitType(JmmNode node, String s){
        if(PRIMITIVES.contains(node.get("ty")))
            return s;
        return s;
    }

    private List<Report>visitClassDeclaration(JmmNode node, SymbolTable symbolTable){
        return null;
    }

    private List<Report>visitReturnStatement(JmmNode node, SymbolTable symbolTable){
        return null;
    }

    private String visitBinaryOp(JmmNode node, String s){
        List<String> localVariableNames = new ArrayList<>();
        JmmNode methodNode = node;
        var methodNodeName =  "";
        while (!methodNode.hasAttribute("methodName") && (!methodNode.getKind().equals("MainDeclaration"))){
            var teste = methodNode.getKind().equals("MainDeclaration");
            methodNode = methodNode.getJmmParent();
        }
        //List<Symbol> teste = symbolTable.getLocalVariables(methodNode.get("methodName"));
        if(methodNode.getKind().equals("MainDeclaration")){
            methodNodeName = "main";
        }
        else{
            methodNodeName = methodNode.get("methodName");
        }
        Type lhsType = new Type("impossible", false);
        Type rhsType = new Type("impossible", false);
        if(node.getChildren().get(0).getKind().equals("ArrayAccessChain")){
            var t1 = symbolTable.getAssignments(methodNodeName);
            //Type rhsType = symbolTable.getVariableType(methodNodeName, node.getChildren().get(1).getChildren().get(1).get("value"));
            //var t = 1;
            if(node.getChildren().get(0).getChildren().get(1).getKind().equals("Integer")){
                lhsType = new Type("int", false);
                var te = 1;
            }
            for(Symbol assignment : symbolTable.getAssignments(methodNodeName)){
                if(assignment.getName().equals(node.getChildren().get(0).getChildren().get(1).get("value"))){
                    lhsType = assignment.getType();
                    var t = 1;
                }
                var t = 1;
            }
        }else if(node.getChildren().get(0).getKind().equals("Integer")){
            lhsType = new Type("int", false);
        }
        else if(node.getChildren().get(0).getKind().equals("Identifier")){
            if(node.getChildren().get(0).get("value").equals("true") || node.getChildren().get(0).get("value").equals("false")){
                lhsType = new Type("boolean", false);
            }
            else{
                for(Symbol assignment : symbolTable.getAssignments(methodNodeName)){
                    if(assignment.getName().equals(node.getChildren().get(0).get("value"))){
                        lhsType = assignment.getType();
                        var t = 1;
                    }
                }
            //lhsType = symbolTable.getVariableType(methodNodeName, node.getChildren().get(0).get("value"));
        }
        }
        else{
            for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                if (assignment.getName().equals(node.getChildren().get(0).getChildren().get(1).get("value"))) {
                    lhsType = assignment.getType();
                    var t = 1;
                }
            }
            //lhsType = symbolTable.getVariableType(methodNodeName, node.getChildren().get(0).get("value"));
        }
        if(node.getChildren().get(1).getKind().equals("ArrayAccessChain")){
            var t1 = symbolTable.getAssignments(methodNodeName);
            //Type rhsType = symbolTable.getVariableType(methodNodeName, node.getChildren().get(1).getChildren().get(1).get("value"));
            //var t = 1;
            if(node.getChildren().get(1).getChildren().get(1).getKind().equals("Integer")){
                rhsType = new Type("int", false);
                var te = 1;
            }else {
                var tete = symbolTable.getAssignments(methodNodeName);
                for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                    if (assignment.getName().equals(node.getChildren().get(1).getChildren().get(1).get("value"))) {
                        rhsType = assignment.getType();
                        var t = 1;
                    }
                    var t = 1;
                }
            }
        }else if(node.getChildren().get(1).getKind().equals("Integer")){
            rhsType = new Type("int", false);
        }else if(node.getChildren().get(1).getKind().equals("Identifier")){
            if(node.getChildren().get(1).get("value").equals("true") || node.getChildren().get(1).get("value").equals("false")){
                rhsType = new Type("boolean", false);
            }
            else{
                for(Symbol assignment : symbolTable.getAssignments(methodNodeName)){
                    if(assignment.getName().equals(node.getChildren().get(1).get("value"))){
                        rhsType = assignment.getType();
                        var t = 1;
                    }
                }
                //lhsType = symbolTable.getVariableType(methodNodeName, node.getChildren().get(0).get("value"));
            }
        }
        else {
            for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                if (assignment.getName().equals(node.getChildren().get(1).getChildren().get(1).get("value"))) {
                    rhsType = assignment.getType();
                    var t = 1;
                    //Type lhsType = getType(node.getChildren().get(0), "ty");
                    //Type lhsType = getType(node.getChildren().get(0), "ty");
                    //rhsType = symbolTable.getVariableType(methodNodeName, node.getChildren().get(1).get("value"));
                    //Type rhsType = getType(node.getChildren().get(1), "ty");
                }
            }
        }

        if(!lhsType.equals(rhsType)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
        }
        return s;
    }

    private String visitVariable(JmmNode node, String s){
        List<String> localVariableNames = new ArrayList<>();
        JmmNode methodNode = node;
        var methodNodeName =  "";
        while (!methodNode.hasAttribute("methodName") && (!methodNode.getKind().equals("MainDeclaration"))){
            var teste = methodNode.getKind().equals("MainDeclaration");
            methodNode = methodNode.getJmmParent();
        }
        //List<Symbol> teste = symbolTable.getLocalVariables(methodNode.get("methodName"));
        if(methodNode.getKind().equals("MainDeclaration")){
            methodNodeName = "main";
        }
        else{
            methodNodeName = methodNode.get("methodName");
        }
        //var teste2 = node.getJmmParent().get("assignmentName");

        for(Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
            var teste3 = localVariable.getName();
            localVariableNames.add(localVariable.getName());
        }
        var teste2 = node.get("value");
        var teste4 = node.getKind();
        //var teste4 = node.getJmmParent().get("assignmentName");
        if(node.getJmmParent().getKind().equals("Assignment")){
            if (!localVariableNames.contains(node.getJmmParent().get("assignmentName"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Variable not declared!"));
            }
        }else {
            if(!localVariableNames.contains(node.get("value"))){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Variable not declared!"));
            }
        }

        /*if(!symbolTable.getLocalVariables(methodNode.get("methodName")).contains(node.getJmmParent().get("assignmentName"))){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "Variable not declared!"));
        }*/

        return s;}

    private String visitIfElseStatement(JmmNode node, String s){
        List<String> localVariableNames = new ArrayList<>();
        JmmNode methodNode = node;
        var methodNodeName =  "";
        while (!methodNode.hasAttribute("methodName") && (!methodNode.getKind().equals("MainDeclaration"))){
            var teste = methodNode.getKind().equals("MainDeclaration");
            methodNode = methodNode.getJmmParent();
        }
        //List<Symbol> teste = symbolTable.getLocalVariables(methodNode.get("methodName"));
        if(methodNode.getKind().equals("MainDeclaration")){
            methodNodeName = "main";
        }
        else{
            methodNodeName = methodNode.get("methodName");
        }
        if(node.getChildren().get(0).getKind().equals(("Identifier"))){
            for(Symbol s1 : symbolTable.getLocalVariables(methodNodeName)){
                if(s1.getName().equals(node.getChildren().get(0).get("value"))){
                    if(!s1.getType().equals(new Type("boolean", false))){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Condition must be a boolean!"));
                    }
                }
            }
        }else {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "Condition must be a boolean!"));
        }
        return s;
    }

    public String visitArrayAccessChain(JmmNode node, String s){
        List<String> localVariableNames = new ArrayList<>();
        JmmNode methodNode = node;
        var methodNodeName =  "";
        while (!methodNode.hasAttribute("methodName") && (!methodNode.getKind().equals("MainDeclaration"))){
            var teste = methodNode.getKind().equals("MainDeclaration");
            methodNode = methodNode.getJmmParent();
        }
        //List<Symbol> teste = symbolTable.getLocalVariables(methodNode.get("methodName"));
        if(methodNode.getKind().equals("MainDeclaration")){
            methodNodeName = "main";
        }
        else{
            methodNodeName = methodNode.get("methodName");
        }

        for(Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
            if(localVariable.getName().equals(node.getChildren().get(0).get("value"))){
                if(!localVariable.getType().isArray()){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Variable is not an array!"));
                }
                else{
                    if(!node.getChildren().get(1).getKind().equals("Integer")){
                        for(Symbol localVariable2 : symbolTable.getLocalVariables(methodNodeName)){
                            if(localVariable2.getName().equals(node.getChildren().get(1).get("value"))){
                                if(!localVariable2.getType().equals(new Type("int", false))){
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Index must be an integer!"));
                                }
                            }
                        }
                    }
                }
            }
        }
        /*if(symbolTable.getLocalVariables("methodName").contains(node.getChildren().get(0).get("value"))){
            sy
        }*/
        return s;
    }

    public String visitMethodCall(JmmNode node, String s) {
        List<String> localVariableNames = new ArrayList<>();
        JmmNode methodNode = node;
        var methodNodeName =  "";
        while (!methodNode.hasAttribute("methodName") && (!methodNode.getKind().equals("MainDeclaration"))){
            var teste = methodNode.getKind().equals("MainDeclaration");
            methodNode = methodNode.getJmmParent();
        }
        //List<Symbol> teste = symbolTable.getLocalVariables(methodNode.get("methodName"));
        if(methodNode.getKind().equals("MainDeclaration")){
            methodNodeName = "main";
        }
        else{
            methodNodeName = methodNode.get("methodName");
        }
        List<String> methodsss = symbolTable.getMethods();
        if (!symbolTable.getMethods().contains(node.get("methodCallName"))) {
            var varValue = node.getChildren().get(0).get("value");
            var varKind = node.getChildren().get(0).getKind();
            for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
                if (localVariable.getName().equals(node.getChildren().get(0).get("value"))) {
                    var localVariableTypeName = localVariable.getType().getName();
                    var teste2 = node.getChildren().get(0).get("value");
                    var teste3 = symbolTable.getClassName();
                    var teste4 = symbolTable.getSuper();
                    if (symbolTable.getImports().contains(localVariableTypeName)) {
                        break;
                    } else if (symbolTable.getSuper() != null && symbolTable.getClassName().equals(localVariable.getType().getName()))
                        break;
                    else {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Method not declared!"));
                    }
                    //symbolTable.getImports();
                    //var teste = 1;
                    //if(localVariable.getType(). )
                }
            }
            //if(node.getChildren().get(0).get("value"))
            //reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "Method not declared!"));
        }
        return s;
    }

    public String visitWhileStatement(JmmNode node, String s){
        List<String> localVariableNames = new ArrayList<>();
        JmmNode methodNode = node;
        var methodNodeName =  "";
        while (!methodNode.hasAttribute("methodName") && (!methodNode.getKind().equals("MainDeclaration"))){
            var teste = methodNode.getKind().equals("MainDeclaration");
            methodNode = methodNode.getJmmParent();
        }
        //List<Symbol> teste = symbolTable.getLocalVariables(methodNode.get("methodName"));
        if(methodNode.getKind().equals("MainDeclaration")){
            methodNodeName = "main";
        }
        else{
            methodNodeName = methodNode.get("methodName");
        }
        if(node.getChildren().get(0).getKind().equals(("Identifier"))){
            for(Symbol s1 : symbolTable.getLocalVariables(methodNodeName)){
                if(s1.getName().equals(node.getChildren().get(0).get("value"))){
                    if(!s1.getType().equals(new Type("boolean", false))){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Condition must be a boolean!"));
                    }
                }
            }
        }
        return s;
    }

    public String visitBracketsAssignment(JmmNode node, String s){
        List<String> localVariableNames = new ArrayList<>();
        JmmNode methodNode = node;
        var methodNodeName =  "";
        while (!methodNode.hasAttribute("methodName") && (!methodNode.getKind().equals("MainDeclaration"))){
            var teste = methodNode.getKind().equals("MainDeclaration");
            methodNode = methodNode.getJmmParent();
        }
        //List<Symbol> teste = symbolTable.getLocalVariables(methodNode.get("methodName"));
        if(methodNode.getKind().equals("MainDeclaration")){
            methodNodeName = "main";
        }
        else{
            methodNodeName = methodNode.get("methodName");
        }

        for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)){
            if(localVariable.getName().equals(node.get("bracketsAssignmentName"))){
                if(!localVariable.getType().isArray()){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Variable is not an array!"));
                }
            }
        }

        if(node.getChildren().get(0).getKind().equals("Integer")){
            return s;
        } else if (node.getChildren().get(0).getKind().equals("Identifier")) {
            if(node.getChildren().get(0).get("value").equals("true") || node.getChildren().get(0).get("value").equals("false")){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Index must be an integer!"));
            } else {
                for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
                    if (localVariable.getName().equals(node.getChildren().get(0).get("value"))) {
                        if (!localVariable.getType().equals(new Type("int", false))) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Index must be an integer!"));
                        }
                    }
                }
            }

        }
        /*if(node.getChildren().get(1).equals("BinaryOp")){

        }*/

        return s;
    }

    public String visitLocalVarsAndAssignment(JmmNode node, String s){
        for(Symbol localVariable : symbolTable.getLocalVariables(node.get("methodName"))){
            var teste = symbolTable.getAssignments(node.get("methodName"));
            var teste2 = localVariable.getName();
            var teste3= 2;
        }
        return s;
    }

    public static Type getType(JmmNode jmmNode, String attribute) {
        String strType = jmmNode.get(attribute);
        Type type = new Type(strType, jmmNode.hasAttribute("isArray"));
        return type;
    }

    public List<Report> getReports(JmmNode node, ImplementedSymbolTable symbolTable){
        this.symbolTable = symbolTable;
        visit(node, null);
        return this.reports;
    }

}
