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
        addVisit("ExpressionStatement", this::visitExpressionStatement);
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
        Method method = symbolTable.getCurrentMethod();
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
                for(Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)){
                    if(localVariable.getName().equals(node.getChildren().get(0).get("value"))){
                        if(localVariable.getType().isArray()){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "array not indexed"));
                        }
                       /* var yyu = symbolTable.getMethods();
                        method.setUsedVariable(node.getChildren().get(0).get("value"));*/
                        if(!symbolTable.getAssignmentNames(methodNodeName).contains(node.getChildren().get(0).get("value"))){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "variable not assigned"));
                        }
                        lhsType = localVariable.getType();
                        var t = 1;
                    }

                }
                for(Symbol parameter : symbolTable.getParameters(methodNodeName)){
                    if(parameter.getName().equals(node.getChildren().get(0).get("value"))){
                        if(parameter.getType().isArray()){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "array not indexed"));
                        }
                        lhsType = parameter.getType();
                        var t = 1;
                    }
                }
            //lhsType = symbolTable.getVariableType(methodNodeName, node.getChildren().get(0).get("value"));
        }
        }else if(node.getChildren().get(0).getKind().equals("MethodCall")){
            var jdjj = symbolTable.getMethodsList();
            for(Method method2 : symbolTable.getMethodsList()){
                var hdh = method2.getName();
                if(method2.getName().equals(node.getChildren().get(0).get("methodCallName"))){
                    /*if(!method2.getReturnType().equals(new Type("int", false))){

                    }*/
                    lhsType = method.getReturnType();
                    var t = 1;
                }
            }
            var sh = 1;
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
                for(Symbol localVariable : symbolTable.getAssignments(methodNodeName)){
                    if(localVariable.getName().equals(node.getChildren().get(1).get("value"))){
                        if(!symbolTable.getAssignmentNames(methodNodeName).contains(node.getChildren().get(1).get("value"))){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "variable not assigned"));
                        }
                        if(localVariable.getType().isArray()){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "array not indexed"));
                        }
                        //method.setUsedVariable(node.getChildren().get(1).get("value"));
                        rhsType = localVariable.getType();
                        var t = 1;
                    }
                }
                for(Symbol parameter : symbolTable.getParameters(methodNodeName)){
                    if(parameter.getName().equals(node.getChildren().get(1).get("value"))){
                        if(parameter.getType().isArray()){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "array not indexed"));
                        }
                        rhsType = parameter.getType();
                        var t = 1;
                    }
                }
                //lhsType = symbolTable.getVariableType(methodNodeName, node.getChildren().get(0).get("value"));
            }
        }else if(node.getChildren().get(1).getKind().equals("MethodCall")){
            var jdjj = symbolTable.getMethodsList();
            for(Method method2 : symbolTable.getMethodsList()){
                var hdh = method2.getName();
                if(method2.getName().equals(node.getChildren().get(1).get("methodCallName"))){
                    /*if(!method2.getReturnType().equals(new Type("int", false))){

                    }*/
                    rhsType = method.getReturnType();
                    var t = 1;
                }
            }
            var sh = 1;
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
        List<String> importNames = new ArrayList<>();
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
        var tdd = symbolTable.getImports();
       /* for(Symbol importNames : symbolTable.getImports()){
            importNames.add(importNames.getName());
        }*/
        var teste2 = node.get("value");
        var teste4 = node.getKind();
        //var teste4 = node.getJmmParent().get("assignmentName");
        if(node.getJmmParent().getKind().equals("Assignment")) {
            if (!localVariableNames.contains(node.getJmmParent().get("assignmentName"))) {
                if (!symbolTable.getFieldNames().contains(node.getJmmParent().get("assignmentName"))) {
                    if(!symbolTable.getParametersNames(methodNodeName).contains(node.getJmmParent().get("assignmentName"))){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Variable not declared!"));
                    }
                }
            }
        }else if(node.getJmmParent().getKind().equals("MethodCall")){
            if(!localVariableNames.contains(node.get("value")) && !symbolTable.getParametersNames(methodNodeName).contains(node.get("value"))){
                if(!symbolTable.getImports().contains(node.get("value"))){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Variable not declared!"));
                }
            }

        }else if(!symbolTable.getFieldNames().contains(node.get("value"))){
            var ggf = symbolTable.getFieldNames();
            var ge = symbolTable.getParameters(methodNodeName);
            if(!localVariableNames.contains(node.get("value")) && !symbolTable.getParametersNames(methodNodeName).contains(node.get("value"))){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Variable not declared!"));
            }
        }
        /*else {
            var ge = symbolTable.getParameters(methodNodeName);
            if(!localVariableNames.contains(node.get("value")) && !symbolTable.getParametersNames(methodNodeName).contains(node.get("value"))){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Variable not declared!"));
            }
        }*/

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
        Method method = symbolTable.getCurrentMethod();
        if(node.getChildren().get(0).getKind().equals(("Identifier"))){
            //method.setUsedVariable(node.getChildren().get(0).get("value"));
            for(Symbol s1 : symbolTable.getLocalVariables(methodNodeName)){
                if(s1.getName().equals(node.getChildren().get(0).get("value"))){
                    if(!symbolTable.getAssignmentNames(methodNodeName).contains(node.getChildren().get(0).get("value"))){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "variable not assigned"));
                    }
                    if(!s1.getType().equals(new Type("boolean", false))){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Condition must be a boolean!"));
                    }
                }
            }
        }else if(node.getChildren().get(0).getKind().equals("BinaryOp")){
            if(node.getChildren().get(0).get("op").equals("+") || node.getChildren().get(0).get("op").equals("-") || node.getChildren().get(0).get("op").equals("*") || node.getChildren().get(0).get("op").equals("/")){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Condition must be a boolean!"));
            }else if(node.getChildren().get(0).get("op").equals("<") || node.getChildren().get(0).get("op").equals(">")){
                for(Symbol assignment : symbolTable.getAssignments(methodNodeName)){
                    if(assignment.getName().equals(node.getChildren().get(0).getChildren().get(0).get("value"))){
                        if(!assignment.getType().equals(new Type("int", false))){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                        }
                    }
                }
                for(Symbol assignment : symbolTable.getAssignments(methodNodeName)){
                    if(assignment.getName().equals(node.getChildren().get(0).getChildren().get(1).get("value"))){
                        if(!assignment.getType().equals(new Type("int", false))){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                        }
                    }
                }
            }else if(node.getChildren().get(0).get("op").equals("&&") || node.getChildren().get(0).get("op").equals("||")){
                for(Symbol assignment : symbolTable.getAssignments(methodNodeName)){
                    if(assignment.getName().equals(node.getChildren().get(0).getChildren().get(0).get("value"))){
                        if(!assignment.getType().equals(new Type("boolean", false))){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be booleans!"));
                        }
                    }
                }
                for(Symbol assignment : symbolTable.getAssignments(methodNodeName)){
                    if(assignment.getName().equals(node.getChildren().get(0).getChildren().get(1).get("value"))){
                        if(!assignment.getType().equals(new Type("boolean", false))){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be booleans!"));
                        }
                    }
                }
                /*var lhsType = getType(node.getChildren().get(0).getChildren().get(0), "ty");
                var rhsType = getType(node.getChildren().get(0).getChildren().get(1), "ty");
                if(!lhsType.equals(new Type("boolean", false)) || !rhsType.equals(new Type("boolean", false))){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be booleans!"));
                }*/
            }

        }
        else {
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
        var bb = symbolTable.getLocalVariables(methodNodeName);
        for(Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
            if(localVariable.getName().equals(node.getChildren().get(0).get("value"))){
                var hhh = symbolTable.getAssignmentNames(methodNodeName);
                if(!symbolTable.getAssignmentNames(methodNodeName).contains(node.getChildren().get(0).get("value"))){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "variable not assigned"));
                }
                if(!localVariable.getType().isArray()){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Variable is not an array!"));
                }
                else{
                    if(!node.getChildren().get(1).getKind().equals("Integer")){
                        if(node.getChildren().get(1).getKind().equals("Boolean"))
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Index must be an integer!"));
                        for(Symbol localVariable2 : symbolTable.getLocalVariables(methodNodeName)){
                            if(localVariable2.getName().equals(node.getChildren().get(1).get("value"))){
                                if(!symbolTable.getAssignmentNames(methodNodeName).contains(node.getChildren().get(1).get("value"))){
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "variable not assigned"));
                                }
                                if(!localVariable2.getType().equals(new Type("int", false))){
                                    //if(me)
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
            if(node.getChildren().get(0).getKind().equals("This")){
                var varValue = node.getChildren().get(1).get("value");
                var varKind = node.getChildren().get(0).getKind();
                for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
                    /*if (localVariable.getName().equals(node.getChildren().get(1).get("value"))) {*/
                    if(!symbolTable.getMethods().contains(node.get("methodCallName"))) {
                        var localVariableTypeName = localVariable.getType().getName();
                        var teste2 = node.getChildren().get(1).get("value");
                        var teste3 = symbolTable.getClassName();
                        var fjfj = symbolTable.getMethods();
                        var teste4 = symbolTable.getSuper();

                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Method not declared!"));

                    }
                        //symbolTable.getImports();
                        //var teste = 1;
                        //if(localVariable.getType(). )
                    //}
                }
            }
            else {
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
                //if()
            }
            //if(node.getChildren().get(0).get("value"))
            //reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "Method not declared!"));
        }
        if(!node.getJmmParent().getKind().equals("ExpressionStatement")){
        if(symbolTable.getMethods().contains(node.get("methodCallName")) || symbolTable.getImports().contains(node.get("methodCallName"))) {
            for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
                if(node.getChildren().size() > 1) {
                    if (localVariable.getName().equals(node.getChildren().get(1).get("value"))) {
                        if (!symbolTable.getParameters(node.get("methodCallName")).contains(localVariable)) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Incompatible arguments type!"));
                        }
                    }
                }
            }
        }
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
                    if(!symbolTable.getAssignmentNames(methodNodeName).contains(node.getChildren().get(0).get("value"))){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "variable not assigned"));
                    }
                    if(!s1.getType().equals(new Type("boolean", false))){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Condition must be a boolean!"));
                    }
                }
            }
        }else if(node.getChildren().get(0).get("op").equals("<") || node.getChildren().get(0).get("op").equals(">")){
            for(Symbol assignment : symbolTable.getAssignments(methodNodeName)){
                if(assignment.getName().equals(node.getChildren().get(0).getChildren().get(0).get("value"))){
                    if(!assignment.getType().equals(new Type("int", false))){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                    }
                }
            }
            for(Symbol assignment : symbolTable.getAssignments(methodNodeName)){
                if(assignment.getName().equals(node.getChildren().get(0).getChildren().get(1).get("value"))){
                    if(!assignment.getType().equals(new Type("int", false))){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                    }
                }
            }
        }else if(node.getChildren().get(0).get("op").equals("&&") || node.getChildren().get(0).get("op").equals("||")){
            for(Symbol assignment : symbolTable.getAssignments(methodNodeName)){
                if(assignment.getName().equals(node.getChildren().get(0).getChildren().get(0).get("value"))){
                    if(!assignment.getType().equals(new Type("boolean", false))){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be booleans!"));
                    }
                }
            }
            for(Symbol assignment : symbolTable.getAssignments(methodNodeName)){
                if(assignment.getName().equals(node.getChildren().get(0).getChildren().get(1).get("value"))){
                    if(!assignment.getType().equals(new Type("boolean", false))){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be booleans!"));
                    }
                }
            }
                /*var lhsType = getType(node.getChildren().get(0).getChildren().get(0), "ty");
                var rhsType = getType(node.getChildren().get(0).getChildren().get(1), "ty");
                if(!lhsType.equals(new Type("boolean", false)) || !rhsType.equals(new Type("boolean", false))){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be booleans!"));
                }*/
        }
        else {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "Condition must be a boolean!"));
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
        if(node.getChildren().get(0).getKind().equals("Boolean"))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Index must be an integer!"));
        if(node.getChildren().get(1).getKind().equals("Boolean"))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Value assigned must be an integer!"));

        if (node.getChildren().get(0).getKind().equals("Identifier")) {
            /*if(node.getChildren().get(0).get("value").equals("true") || node.getChildren().get(0).get("value").equals("false")){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Index must be an integer!"));
            } else {*/
                for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
                    if (localVariable.getName().equals(node.getChildren().get(0).get("value"))) {
                        if (!localVariable.getType().equals(new Type("int", false))) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Index must be an integer!"));
                        }else {
                            for(Symbol assignment : symbolTable.getAssignments(methodNodeName)){
                                if(assignment.getName().equals(node.getChildren().get(0).get("value"))){

                                }
                            }
                        }
                    //}
                }
                for(Symbol parameter : symbolTable.getParameters(methodNodeName)){
                    if(parameter.getName().equals(node.getChildren().get(0).get("value"))){
                        if(!parameter.getType().equals(new Type("int", false))){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Index must be an integer!"));
                        }
                    }
                }
            }
        }

        if (node.getChildren().get(1).getKind().equals("Identifier")) {
           /* if(node.getChildren().get(1).get("value").equals("true") || node.getChildren().get(1).get("value").equals("false")){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Value assigned must be an integer!"));
            } else {*/
                for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
                    if (localVariable.getName().equals(node.getChildren().get(1).get("value"))) {
                        if (!localVariable.getType().equals(new Type("int", false))) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Value assigned must be an integer!"));
                        }
                    }
                }
            //}

        }
        /*if(node.getChildren().get(1).equals("BinaryOp")){

        }*/

        return s;
    }

    public String visitExpressionStatement(JmmNode node, String s){
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
        if(methodNodeName.equals("main")){
            if(node.getChildren().get(0).getKind().equals("MethodCall")){
                if(node.getChildren().get(0).getChildren().get(0).getKind().equals("This"))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "This cannot be used in the Main method"));
                else{
                    /*for(String method : symbolTable.getMethods()){

                    }*/
                }
            }
            else if(node.getChildren().get(0).getKind().equals("MemberAccess")){
                if(node.getChildren().get(0).getChildren().get(0).getKind().equals("This")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "This cannot be used in the Main method"));
                }
            }
        }
        if(node.getChildren().get(0).getKind().equals("MethodCall")){
            if(symbolTable.getMethods().contains(node.getChildren().get(0).get("methodCallName"))){
                   /* for (JmmNode child : node.getChildren().get(0).getChildren()) {
                        if (!child.equals(node.getChildren().get(0).getChildren().get(0))) {
                            if (!child.getKind().equals("MemberAccess")) {
                                //for (Symbol parameter : symbolTable.getParameters(node.getChildren().get(0).get("methodCallName"))) {
                                for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
                                    if (localVariable.getName().equals(child.get("value"))) {
                                        for (Symbol parameter : symbolTable.getParameters(node.getChildren().get(0).get("methodCallName"))) {
                                            if (!child.getKind().equals("MemberAccess")) {
                                                if (localVariable.getName().equals(child.get("value"))) {
                                                    child++;
                                                    if (!parameter.getType().equals(localVariable.getType())) {
                                                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Parameter type does not match the type of the variable!"));
                                                        //break;
                                                    }
                                                    // var jj =1;
                                                    // break;
                                                }

                                            } else {
                                                for (Symbol fields : symbolTable.getFields()) {
                                                    if (fields.getName().equals(child.get("accessName"))) {
                                                        if (!parameter.getType().equals(fields.getType())) {
                                                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Parameter type does not match the type of the variable!"));
                                                        }
                                                    }
                                                }
                                            }
                                            for (Symbol fields : symbolTable.getFields()) {
                                                if (fields.getName().equals(child.get("value"))) {
                                                    if (!parameter.getType().equals(fields.getType())) {
                                                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Parameter type does not match the type of the variable!"));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
            }*/
                for(int i = 0; i < symbolTable.getParameters(node.getChildren().get(0).get("methodCallName")).size(); i++){
                    for(int j = 1; j < node.getChildren().get(0).getChildren().size(); j++){
                        if(!node.getChildren().get(0).getChildren().get(j).getKind().equals("MemberAccess")) {
                            for (int k = 0; k < symbolTable.getLocalVariables(methodNodeName).size(); k++) {
                                if (!node.getChildren().get(0).getChildren().get(j).getKind().equals("MemberAccess")) {
                                    if (symbolTable.getLocalVariables(methodNodeName).get(k).getName().equals(node.getChildren().get(0).getChildren().get(j).get("value"))) {
                                        if (!symbolTable.getParameters(node.getChildren().get(0).get("methodCallName")).get(i).getType().equals(symbolTable.getLocalVariables(methodNodeName).get(k).getType())) {
                                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Parameter type does not match the type of the variable!"));
                                            i++;
                                        }else {
                                            i++;
                                        }
                                    }
                                }
                            }
                            for (Symbol fields : symbolTable.getFields()) {
                                if (fields.getName().equals(node.getChildren().get(0).getChildren().get(j).get("value"))) {
                                    if (!symbolTable.getParameters(node.getChildren().get(0).get("methodCallName")).get(i).getType().equals(fields.getType())) {
                                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Parameter type does not match the type of the variable!"));
                                    }
                                }
                            }
                        }
                         else {
                            for (Symbol fields : symbolTable.getFields()) {
                                if (fields.getName().equals(node.getChildren().get(0).getChildren().get(j).get("accessName"))) {
                                    if (!symbolTable.getParameters(node.getChildren().get(0).get("methodCallName")).get(i).getType().equals(fields.getType())) {
                                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Parameter type does not match the type of the variable!"));
                                    }
                                }
                            }
                        }
                        }
                    }
                }

                /*for(Symbol parameter : symbolTable.getParameters(node.getChildren().get(0).get("methodCallName"))) {
                    for (JmmNode child : node.getChildren().get(0).getChildren()) {
                        if (!child.equals(node.getChildren().get(0).getChildren().get(0))) {
                            if (!child.getKind().equals("MemberAccess")) {
                                for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
                                    if (!child.getKind().equals("MemberAccess")) {
                                        if (localVariable.getName().equals(child.get("value"))) {
                                            if (!parameter.getType().equals(localVariable.getType())) {
                                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Parameter type does not match the type of the variable!"));
                                                //break;
                                            }
                                           // var jj =1;
                                           // break;
                                        }
                                    }

                                }
                                for (Symbol fields : symbolTable.getFields()) {
                                    if (fields.getName().equals(child.get("value"))) {
                                        if (!parameter.getType().equals(fields.getType())) {
                                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Parameter type does not match the type of the variable!"));
                                        }
                                    }
                                }
                            } else {
                                for (Symbol fields : symbolTable.getFields()) {
                                    if (fields.getName().equals(child.get("accessName"))) {
                                        if (!parameter.getType().equals(fields.getType())) {
                                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Parameter type does not match the type of the variable!"));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


            }*/
        }
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
