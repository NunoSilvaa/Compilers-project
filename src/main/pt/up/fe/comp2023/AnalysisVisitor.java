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
import pt.up.fe.comp2023.analysis.table.SymbolTableVisitor;

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
        addVisit("BinaryOp", this::visitBinaryOp);

        setDefaultVisit(this::defaultVisit);
    }

    public ImplementedSymbolTable getSymbolTable(JmmNode node) {
        visit(node, null);
        return this.symbolTable;
    }

    private String defaultVisit(JmmNode jmmNode, String s) {
        String ret = s + jmmNode.getKind();
        String attributes = jmmNode.getAttributes()
                .stream()
                .filter(a -> !a.equals("line"))
                .map(a -> a + "=" + jmmNode.get(a))
                .collect(Collectors.joining(", ", "[", "]"));

        ret += ((attributes.length() > 2) ? attributes : "");
        return ret;
    }

    private String visitType(JmmNode node, String s) {
        if (PRIMITIVES.contains(node.get("ty")))
            return s;
        return s;
    }


    private String visitBinaryOp(JmmNode node, String s) {
        List<String> localVariableNames = new ArrayList<>();
        JmmNode methodNode = node;
        var methodNodeName = "";
        while (!methodNode.hasAttribute("methodName") && (!methodNode.getKind().equals("MainDeclaration"))) {
            methodNode = methodNode.getJmmParent();
        }
        if (methodNode.getKind().equals("MainDeclaration")) {
            methodNodeName = "main";
        } else {
            methodNodeName = methodNode.get("methodName");
        }
        Method method = symbolTable.getCurrentMethod();
        Type lhsType = new Type("impossible", false);
        Type rhsType = new Type("impossible", false);
        if (node.getChildren().get(0).getKind().equals("ArrayAccessChain")) {
            if (node.getChildren().get(0).getChildren().get(1).getKind().equals("Integer")) {
                lhsType = new Type("int", false);
            }
            for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                if (assignment.getName().equals(node.getChildren().get(0).getChildren().get(1).get("value"))) {
                    lhsType = assignment.getType();
                }
            }
        } else if (node.getChildren().get(0).getKind().equals("Integer")) {
            lhsType = new Type("int", false);
        } else if (node.getChildren().get(0).getKind().equals("Identifier")) {
            if (node.getChildren().get(0).get("value").equals("true") || node.getChildren().get(0).get("value").equals("false")) {
                lhsType = new Type("boolean", false);
            } else {
                for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
                    if (localVariable.getName().equals(node.getChildren().get(0).get("value"))) {
                        if (localVariable.getType().isArray()) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "array not indexed"));
                        }
                        if (!symbolTable.getAssignmentNames(methodNodeName).contains(node.getChildren().get(0).get("value"))) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "variable not assigned"));
                        }
                        lhsType = localVariable.getType();
                    }

                }
                for (Symbol parameter : symbolTable.getParameters(methodNodeName)) {
                    if (parameter.getName().equals(node.getChildren().get(0).get("value"))) {
                        if (parameter.getType().isArray()) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "array not indexed"));
                        }
                        lhsType = parameter.getType();
                    }
                }
            }
        } else if (node.getChildren().get(0).getKind().equals("MethodCall")) {
            for (Method method2 : symbolTable.getMethodsList()) {
                if (method2.getName().equals(node.getChildren().get(0).get("methodCallName"))) {
                    lhsType = method.getReturnType();
                }
            }
        } else if (node.getChildren().get(0).getKind().equals("Boolean")) {
            lhsType = new Type("boolean", false);
        } else if (node.getChildren().get(0).getKind().equals("MemberAccess")) {
            if (node.getChildren().get(0).getChildren().get(0).getKind().equals("This")) {
                for (Symbol field : symbolTable.getFields()) {
                    if (field.getName().equals(node.getChildren().get(0).get("accessName"))) {
                        lhsType = field.getType();
                    }
                }
            }
        } else if (node.getChildren().get(0).getKind().equals("BinaryOp")) {
            visitBinaryOp(node.getChildren().get(0), s);
            lhsType = new Type("int", false);
        } else {
            for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                if (assignment.getName().equals(node.getChildren().get(0).getChildren().get(1).get("value"))) {
                    lhsType = assignment.getType();
                }
            }
        }
        if (node.getChildren().get(1).getKind().equals("ArrayAccessChain")) {
            if (node.getChildren().get(1).getChildren().get(1).getKind().equals("Integer")) {
                rhsType = new Type("int", false);
            } else {
                for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                    if (assignment.getName().equals(node.getChildren().get(1).getChildren().get(1).get("value"))) {
                        rhsType = assignment.getType();
                    }
                }

            }
        } else if (node.getChildren().get(1).getKind().equals("Integer")) {
            rhsType = new Type("int", false);
        } else if (node.getChildren().get(1).getKind().equals("Identifier")) {
            if (node.getChildren().get(1).get("value").equals("true") || node.getChildren().get(1).get("value").equals("false")) {
                rhsType = new Type("boolean", false);
            } else {
                for (Symbol localVariable : symbolTable.getAssignments(methodNodeName)) {
                    if (localVariable.getName().equals(node.getChildren().get(1).get("value"))) {
                        if (!symbolTable.getAssignmentNames(methodNodeName).contains(node.getChildren().get(1).get("value"))) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "variable not assigned"));
                        }
                        if (localVariable.getType().isArray()) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "array not indexed"));
                        }
                        rhsType = localVariable.getType();
                    }
                }
                for (Symbol parameter : symbolTable.getParameters(methodNodeName)) {
                    if (parameter.getName().equals(node.getChildren().get(1).get("value"))) {
                        if (parameter.getType().isArray()) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "array not indexed"));
                        }
                        rhsType = parameter.getType();
                    }
                }
            }
        } else if (node.getChildren().get(1).getKind().equals("MethodCall")) {
            for (Method method2 : symbolTable.getMethodsList()) {
                if (method2.getName().equals(node.getChildren().get(1).get("methodCallName"))) {
                    rhsType = method.getReturnType();

                }
            }
        } else if (node.getChildren().get(1).getKind().equals("This")) {
            for (Symbol field : symbolTable.getFields()) {
                if (field.getName().equals(node.getJmmParent().get("accessName"))) {
                    rhsType = field.getType();
                }
            }
        } else if (node.getChildren().get(1).getKind().equals("Boolean")) {
            rhsType = new Type("boolean", false);
        } else if (node.getChildren().get(1).getKind().equals("BinaryOp")) {
            visitBinaryOp(node.getChildren().get(1), s);
            rhsType = new Type("int", false);
        } else {
            for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                if (assignment.getName().equals(node.getChildren().get(1).getChildren().get(1).get("value"))) {
                    rhsType = assignment.getType();
                }
            }
        }

        if (!lhsType.equals(rhsType)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
        }
        return s;
    }

    private String visitVariable(JmmNode node, String s) {
        List<String> localVariableNames = new ArrayList<>();
        List<String> importNames = new ArrayList<>();
        JmmNode methodNode = node;
        var methodNodeName = "";
        while (!methodNode.hasAttribute("methodName") && (!methodNode.getKind().equals("MainDeclaration"))) {
            methodNode = methodNode.getJmmParent();
        }
        if (methodNode.getKind().equals("MainDeclaration")) {
            methodNodeName = "main";
        } else {
            methodNodeName = methodNode.get("methodName");
        }

        for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
            localVariableNames.add(localVariable.getName());
        }
        if (node.getJmmParent().getKind().equals("Assignment")) {
            if (!localVariableNames.contains(node.getJmmParent().get("assignmentName"))) {
                if (!symbolTable.getFieldNames().contains(node.getJmmParent().get("assignmentName"))) {
                    if (!symbolTable.getParametersNames(methodNodeName).contains(node.getJmmParent().get("assignmentName"))) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Variable not declared!"));
                    }
                }
            }
        } else if (node.getJmmParent().getKind().equals("MethodCall")) {
            var jjj = localVariableNames.contains(node.get("value"));
            if (!localVariableNames.contains(node.get("value")) && !symbolTable.getParametersNames(methodNodeName).contains(node.get("value"))) {
                if (!symbolTable.getImports().contains(node.get("value")) && !symbolTable.getFieldNames().contains(node.get("value"))) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Variable not declared!"));
                }
            }

        } else if (!symbolTable.getFieldNames().contains(node.get("value"))) {
            if (!localVariableNames.contains(node.get("value")) && !symbolTable.getParametersNames(methodNodeName).contains(node.get("value"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Variable not declared!"));
            }
        }

        return s;
    }

    private String visitIfElseStatement(JmmNode node, String s) {
        List<String> localVariableNames = new ArrayList<>();
        JmmNode methodNode = node;
        var methodNodeName = "";
        while (!methodNode.hasAttribute("methodName") && (!methodNode.getKind().equals("MainDeclaration"))) {
            methodNode = methodNode.getJmmParent();
        }
        if (methodNode.getKind().equals("MainDeclaration")) {
            methodNodeName = "main";
        } else {
            methodNodeName = methodNode.get("methodName");
        }
        Method method = symbolTable.getCurrentMethod();
        if (node.getChildren().get(0).getKind().equals(("Identifier"))) {
            for (Symbol s1 : symbolTable.getLocalVariables(methodNodeName)) {
                if (s1.getName().equals(node.getChildren().get(0).get("value"))) {
                    if (!symbolTable.getAssignmentNames(methodNodeName).contains(node.getChildren().get(0).get("value"))) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "variable not assigned"));
                    }
                    if (!s1.getType().equals(new Type("boolean", false))) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Condition must be a boolean!"));
                    }
                }
            }
        } else if (node.getChildren().get(0).getKind().equals("BinaryOp")) {
            if (node.getChildren().get(0).get("op").equals("+") || node.getChildren().get(0).get("op").equals("-") || node.getChildren().get(0).get("op").equals("*") || node.getChildren().get(0).get("op").equals("/")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Condition must be a boolean!"));
            } else if (node.getChildren().get(0).get("op").equals("<") || node.getChildren().get(0).get("op").equals(">") || node.getChildren().get(0).get("op").equals("<=") || node.getChildren().get(0).get("op").equals(">=")) {
                if (node.getChildren().get(0).getChildren().get(0).getKind().equals("MemberAccess")) {
                    for (Symbol field : symbolTable.getFields()) {
                        if (field.getName().equals(node.getChildren().get(0).getChildren().get(0).get("accessName"))) {
                            if (!field.getType().equals(new Type("int", false))) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                            }
                        }
                    }
                } else {
                    for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                        if (assignment.getName().equals(node.getChildren().get(0).getChildren().get(0).get("value"))) {
                            if (!assignment.getType().equals(new Type("int", false))) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                            }
                        }
                    }
                }
                for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                    if (assignment.getName().equals(node.getChildren().get(0).getChildren().get(1).get("value"))) {
                        if (!assignment.getType().equals(new Type("int", false))) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                        }
                    }
                }
            } else if (node.getChildren().get(0).get("op").equals("&&") || node.getChildren().get(0).get("op").equals("||")) {
                if (node.getChildren().get(0).getChildren().get(0).getKind().equals("MemberAccess")) {
                    for (Symbol field : symbolTable.getFields()) {
                        if (field.getName().equals(node.getChildren().get(0).getChildren().get(0).get("accessName"))) {
                            if (!field.getType().equals(new Type("boolean", false))) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                            }
                        }
                    }
                } else {
                    for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                        if (assignment.getName().equals(node.getChildren().get(0).getChildren().get(0).get("value"))) {
                            if (!assignment.getType().equals(new Type("boolean", false))) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be booleans!"));
                            }
                        }
                    }
                }
                for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                    if (assignment.getName().equals(node.getChildren().get(0).getChildren().get(1).get("value"))) {
                        if (!assignment.getType().equals(new Type("boolean", false))) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be booleans!"));
                        }
                    }
                }
            }

        } else if (node.getChildren().get(0).getKind().equals("MemberAccess")) {
            if (node.getChildren().get(0).getChildren().get(0).getKind().equals("BinaryOp")) {
                if (node.getChildren().get(0).getChildren().get(0).get("op").equals("+") || node.getChildren().get(0).getChildren().get(0).get("op").equals("-") || node.getChildren().get(0).getChildren().get(0).get("op").equals("*") || node.getChildren().get(0).getChildren().get(0).get("op").equals("/")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Condition must be a boolean!"));
                } else if (node.getChildren().get(0).getChildren().get(0).get("op").equals("<") || node.getChildren().get(0).getChildren().get(0).get("op").equals(">") || node.getChildren().get(0).getChildren().get(0).get("op").equals("<=") || node.getChildren().get(0).getChildren().get(0).get("op").equals(">=")) {
                    if (node.getChildren().get(0).getChildren().get(0).getChildren().get(0).getKind().equals("MemberAccess")) {
                        for (Symbol field : symbolTable.getFields()) {
                            if (field.getName().equals(node.getChildren().get(0).getChildren().get(0).getChildren().get(0).get("accessName"))) {
                                if (!field.getType().equals(new Type("int", false))) {
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                                }
                            }
                        }
                    } else {
                        for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                            if (assignment.getName().equals(node.getChildren().get(0).getChildren().get(0).getChildren().get(0).get("value"))) {
                                if (!assignment.getType().equals(new Type("int", false))) {
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                                }
                            }
                        }
                    }
                    for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                        if (assignment.getName().equals(node.getChildren().get(0).get("accessName"))) {
                            if (!assignment.getType().equals(new Type("int", false))) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                            }
                        }
                    }
                } else if (node.getChildren().get(0).getChildren().get(0).get("op").equals("&&") || node.getChildren().get(0).getChildren().get(0).get("op").equals("||")) {
                    if (node.getChildren().get(0).getChildren().get(0).getChildren().get(0).getKind().equals("MemberAccess")) {
                        for (Symbol field : symbolTable.getFields()) {
                            if (field.getName().equals(node.getChildren().get(0).getChildren().get(0).getChildren().get(0).get("accessName"))) {
                                if (!field.getType().equals(new Type("boolean", false))) {
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                                }
                            }
                        }
                    } else {
                        for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                            if (assignment.getName().equals(node.getChildren().get(0).get("accessName"))) {
                                if (!assignment.getType().equals(new Type("boolean", false))) {
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be booleans!"));
                                }
                            }
                        }
                    }
                    for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                        if (assignment.getName().equals(node.getChildren().get(0).get("accessName"))) {
                            if (!assignment.getType().equals(new Type("boolean", false))) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be booleans!"));
                            }
                        }
                    }
                }
            }
        } else if (!node.getChildren().get(0).get("op").equals("==")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "Condition must be a boolean!"));
        }
        return s;
    }

    public String visitArrayAccessChain(JmmNode node, String s) {
        List<String> localVariableNames = new ArrayList<>();
        JmmNode methodNode = node;
        var methodNodeName = "";
        while (!methodNode.hasAttribute("methodName") && (!methodNode.getKind().equals("MainDeclaration"))) {
            methodNode = methodNode.getJmmParent();
        }
        if (methodNode.getKind().equals("MainDeclaration")) {
            methodNodeName = "main";
        } else {
            methodNodeName = methodNode.get("methodName");
        }
        for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
            if (localVariable.getName().equals(node.getChildren().get(0).get("value"))) {
                if (!symbolTable.getAssignmentNames(methodNodeName).contains(node.getChildren().get(0).get("value"))) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "variable not assigned"));
                }
                if (!localVariable.getType().isArray()) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Variable is not an array!"));
                } else {
                    if (!node.getChildren().get(1).getKind().equals("Integer")) {
                        if (node.getChildren().get(1).getKind().equals("Boolean"))
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Index must be an integer!"));
                        for (Symbol localVariable2 : symbolTable.getLocalVariables(methodNodeName)) {
                            if (localVariable2.getName().equals(node.getChildren().get(1).get("value"))) {
                                if (!symbolTable.getAssignmentNames(methodNodeName).contains(node.getChildren().get(1).get("value"))) {
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "variable not assigned"));
                                }
                                if (!localVariable2.getType().equals(new Type("int", false))) {
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Index must be an integer!"));
                                }

                            }
                        }
                    }
                }
            }
        }
        return s;
    }

    public String visitMethodCall(JmmNode node, String s) {
        List<String> localVariableNames = new ArrayList<>();
        JmmNode methodNode = node;
        var methodNodeName = "";
        while (!methodNode.hasAttribute("methodName") && (!methodNode.getKind().equals("MainDeclaration"))) {
            methodNode = methodNode.getJmmParent();
        }
        if (methodNode.getKind().equals("MainDeclaration")) {
            methodNodeName = "main";
        } else {
            methodNodeName = methodNode.get("methodName");
        }
        if (!symbolTable.getMethods().contains(node.get("methodCallName"))) {
            if (node.getChildren().get(0).getKind().equals("This")) {

                for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
                    if (!symbolTable.getMethods().contains(node.get("methodCallName"))) {
                        var localVariableTypeName = localVariable.getType().getName();

                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Method not declared!"));

                    }
                }
            } else {
                for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
                    if (localVariable.getName().equals(node.getChildren().get(0).get("value"))) {
                        var localVariableTypeName = localVariable.getType().getName();
                        if (symbolTable.getImports().contains(localVariableTypeName)) {
                            break;
                        } else if (symbolTable.getSuper() != null && symbolTable.getClassName().equals(localVariable.getType().getName()))
                            break;
                        else {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Method not declared!"));
                        }
                    }
                }
            }
        }
        if (!node.getJmmParent().getKind().equals("ExpressionStatement")) {
            if (symbolTable.getMethods().contains(node.get("methodCallName")) || symbolTable.getImports().contains(node.get("methodCallName"))) {
                if (node.getChildren().size() - 1 == symbolTable.getParameters(node.get("methodCallName")).size()) {
                    for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
                        if (node.getChildren().size() > 1) {
                            for (int i = 1; i < node.getChildren().size(); i++) {
                                if (localVariable.getName().equals(node.getChildren().get(i).get("value"))) {
                                    for (int j = 0; j < symbolTable.getParameters(node.get("methodCallName")).size(); j++) {
                                        if (i - 1 == j) {
                                            if (!symbolTable.getParameters(node.get("methodCallName")).get(j).getType().equals(localVariable.getType())) {
                                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Incompatible arguments type!"));
                                            }
                                        }
                                    }
                            /*if (!symbolTable.getParameters(node.get("methodCallName")).contains(localVariable)) {
                                var t = symbolTable.getParameters(node.get("methodCallName"));
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Incompatible arguments type!"));
                            }*/
                                }
                            }
                        }
                    /*if (localVariable.getName().equals(node.getChildren().get(1).get("value"))) {
                        if (!symbolTable.getParameters(node.get("methodCallName")).contains(localVariable)) {
                            var t = symbolTable.getParameters(node.get("methodCallName"));
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Incompatible arguments type!"));
                        }
                    }*/
                    }
                    /*boolean next = true;
                    for (int i = 1; i < node.getChildren().size(); i++) {
                        if (node.getChildren().get(i).getKind().equals("This")) {
                            next = false;
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "this can not be an argument of a method call"));
                            break;
                        }
                    }
                    if (next) {
                        for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
                            if (node.getChildren().size() > 1) {
                                for (int i = 1; i < node.getChildren().size(); i++) {
                                    if (localVariable.getName().equals(node.getChildren().get(i).get("value"))) {
                                        for (int j = 0; j < symbolTable.getParameters(node.get("methodCallName")).size(); j++) {
                                            if (i - 1 == j) {
                                                if (!symbolTable.getParameters(node.get("methodCallName")).get(j).getType().equals(localVariable.getType())) {
                                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Incompatible arguments type!"));
                                                }
                                            }
                                        }
                            /*if (!symbolTable.getParameters(node.get("methodCallName")).contains(localVariable)) {
                                var t = symbolTable.getParameters(node.get("methodCallName"));
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Incompatible arguments type!"));
                            }
                                    }
                                }
                            }
                    /*if (localVariable.getName().equals(node.getChildren().get(1).get("value"))) {
                        if (!symbolTable.getParameters(node.get("methodCallName")).contains(localVariable)) {
                            var t = symbolTable.getParameters(node.get("methodCallName"));
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Incompatible arguments type!"));
                        }
                    }
                        }
                    }*/
                } else {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Method call has the wrong number of arguments!"));
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
            methodNode = methodNode.getJmmParent();
        }
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
        }else if(node.getChildren().get(0).getKind().equals("BinaryOp")){
            if(node.getChildren().get(0).get("op").equals("+") || node.getChildren().get(0).get("op").equals("-") || node.getChildren().get(0).get("op").equals("*") || node.getChildren().get(0).get("op").equals("/")){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Condition must be a boolean!"));
            }else if(node.getChildren().get(0).get("op").equals("<") || node.getChildren().get(0).get("op").equals(">") || node.getChildren().get(0).get("op").equals("<=") || node.getChildren().get(0).get("op").equals(">=")){
                if(node.getChildren().get(0).getChildren().get(0).getKind().equals("MemberAccess")){
                    for(Symbol field : symbolTable.getFields()){
                        if(field.getName().equals(node.getChildren().get(0).getChildren().get(0).get("accessName"))){
                            if(!field.getType().equals(new Type("int", false))){
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                            }
                        }
                    }
                }else {
                    for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                        if (assignment.getName().equals(node.getChildren().get(0).getChildren().get(0).get("value"))) {
                            if (!assignment.getType().equals(new Type("int", false))) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                            }
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
                if(node.getChildren().get(0).getChildren().get(0).getKind().equals("MemberAccess")){
                    for(Symbol field : symbolTable.getFields()){
                        if(field.getName().equals(node.getChildren().get(0).getChildren().get(0).get("accessName"))){
                            if(!field.getType().equals(new Type("boolean", false))){
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                            }
                        }
                    }
                }else{
                    for(Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                        if (assignment.getName().equals(node.getChildren().get(0).getChildren().get(0).get("value"))) {
                            if (!assignment.getType().equals(new Type("boolean", false))) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be booleans!"));
                            }
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
            }

        }else if(node.getChildren().get(0).getKind().equals("MemberAccess")){
            if(node.getChildren().get(0).getChildren().get(0).getKind().equals("BinaryOp")) {
                if (node.getChildren().get(0).getChildren().get(0).get("op").equals("+") || node.getChildren().get(0).getChildren().get(0).get("op").equals("-") || node.getChildren().get(0).getChildren().get(0).get("op").equals("*") || node.getChildren().get(0).getChildren().get(0).get("op").equals("/")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Condition must be a boolean!"));
                } else if (node.getChildren().get(0).getChildren().get(0).get("op").equals("<") || node.getChildren().get(0).getChildren().get(0).get("op").equals(">") || node.getChildren().get(0).getChildren().get(0).get("op").equals("<=") || node.getChildren().get(0).getChildren().get(0).get("op").equals(">=")) {
                    if (node.getChildren().get(0).getChildren().get(0).getChildren().get(0).getKind().equals("MemberAccess")) {
                        for (Symbol field : symbolTable.getFields()) {
                            if (field.getName().equals(node.getChildren().get(0).getChildren().get(0).getChildren().get(0).get("accessName"))) {
                                if (!field.getType().equals(new Type("int", false))) {
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                                }
                            }
                        }
                    } else {
                        for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                            if (assignment.getName().equals(node.getChildren().get(0).getChildren().get(0).getChildren().get(0).get("value"))) {
                                if (!assignment.getType().equals(new Type("int", false))) {
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                                }
                            }
                        }
                    }
                    for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                        if (assignment.getName().equals(node.getChildren().get(0).get("accessName"))) {
                            if (!assignment.getType().equals(new Type("int", false))) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                            }
                        }
                    }
                } else if (node.getChildren().get(0).getChildren().get(0).get("op").equals("&&") || node.getChildren().get(0).getChildren().get(0).get("op").equals("||")) {
                    if (node.getChildren().get(0).getChildren().get(0).getChildren().get(0).getKind().equals("MemberAccess")) {
                        for (Symbol field : symbolTable.getFields()) {
                            if (field.getName().equals(node.getChildren().get(0).getChildren().get(0).getChildren().get(0).get("accessName"))) {
                                if (!field.getType().equals(new Type("boolean", false))) {
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be integers!"));
                                }
                            }
                        }
                    } else {
                        for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                            if (assignment.getName().equals(node.getChildren().get(0).get("accessName"))) {
                                if (!assignment.getType().equals(new Type("boolean", false))) {
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be booleans!"));
                                }
                            }
                        }
                    }
                    for (Symbol assignment : symbolTable.getAssignments(methodNodeName)) {
                        if (assignment.getName().equals(node.getChildren().get(0).get("accessName"))) {
                            if (!assignment.getType().equals(new Type("boolean", false))) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Both operands must be booleans!"));
                            }
                        }
                    }
                }
            }
        }
        else if(!node.getChildren().get(0).get("op").equals("==")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "Condition must be a boolean!"));
        }
        return s;
    }

    public String visitBracketsAssignment(JmmNode node, String s){
        List<String> localVariableNames = new ArrayList<>();
        JmmNode methodNode = node;
        var methodNodeName =  "";
        while (!methodNode.hasAttribute("methodName") && (!methodNode.getKind().equals("MainDeclaration"))){
            methodNode = methodNode.getJmmParent();
        }
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
                for (Symbol localVariable : symbolTable.getLocalVariables(methodNodeName)) {
                    if (localVariable.getName().equals(node.getChildren().get(1).get("value"))) {
                        if (!localVariable.getType().equals(new Type("int", false))) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Value assigned must be an integer!"));
                        }
                    }
                }

        }

        return s;
    }

    public String visitExpressionStatement(JmmNode node, String s){
        List<String> localVariableNames = new ArrayList<>();
        JmmNode methodNode = node;
        var methodNodeName =  "";
        while (!methodNode.hasAttribute("methodName") && (!methodNode.getKind().equals("MainDeclaration"))){
            methodNode = methodNode.getJmmParent();
        }
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
            }
            else if(node.getChildren().get(0).getKind().equals("MemberAccess")){
                if(node.getChildren().get(0).getChildren().get(0).getKind().equals("This")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "This cannot be used in the Main method"));
                }
            }
        }
        if(symbolTable.getMethods().contains(node.getChildren().get(0).get("methodCallName"))) {
            if (node.getChildren().get(0).getChildren().size() - 1 != symbolTable.getParameters(node.getChildren().get(0).get("methodCallName")).size()) {
                if(!node.getChildren().get(0).getChildren().get(0).getKind().equals("This")) {
                    if (!symbolTable.getImports().contains(node.getChildren().get(0).getChildren().get(0).get("value"))) {
                        var hhg = symbolTable.getImports();
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Method call has the wrong number of arguments!"));
                    }
                }else{
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Method call has the wrong number of arguments!"));
                }
            } else {
                if (node.getChildren().get(0).getKind().equals("MethodCall")) {
                    if (symbolTable.getMethods().contains(node.getChildren().get(0).get("methodCallName"))) {
                        for (int i = 0; i < symbolTable.getParameters(node.getChildren().get(0).get("methodCallName")).size(); i++) {
                            for (int j = 1; j < node.getChildren().get(0).getChildren().size(); j++) {
                                if (!node.getChildren().get(0).getChildren().get(j).getKind().equals("MemberAccess")) {
                                    for (int k = 0; k < symbolTable.getLocalVariables(methodNodeName).size(); k++) {
                                        if (!node.getChildren().get(0).getChildren().get(j).getKind().equals("MemberAccess")) {
                                            var u = 1;
                                            if (symbolTable.getLocalVariables(methodNodeName).get(k).getName().equals(node.getChildren().get(0).getChildren().get(j).get("value"))) {
                                                if (!symbolTable.getParameters(node.getChildren().get(0).get("methodCallName")).get(i).getType().equals(symbolTable.getLocalVariables(methodNodeName).get(k).getType())) {
                                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Parameter type does not match the type of the variable!"));
                                                    i++;
                                                } else {
                                                    i++;
                                                }
                                            }
                                        }
                                    }
                                        for (Symbol fields : symbolTable.getFields()) {
                                            if (fields.getName().equals(node.getChildren().get(0).getChildren().get(j).get("value")) /*|| fields.getName().equals(node.getChildren().get(0).getChildren().get(j).get("accessName"))*/) {
                                                if (!symbolTable.getParameters(node.getChildren().get(0).get("methodCallName")).get(i).getType().equals(fields.getType())) {
                                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Parameter type does not match the type of the variable!"));
                                                    i++;
                                                } else {
                                                    i++;
                                                }
                                            }
                                        }
                                } else {
                                    for (Symbol fields : symbolTable.getFields()) {
                                        if (fields.getName().equals(node.getChildren().get(0).getChildren().get(j).get("accessName"))) {
                                            if (!symbolTable.getParameters(node.getChildren().get(0).get("methodCallName")).get(i).getType().equals(fields.getType())) {
                                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Parameter type does not match the type of the variable!"));
                                                i++;
                                            } else {
                                                i++;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }else{
            if(!symbolTable.getImports().contains(node.getChildren().get(0).getChildren().get(0).get("value"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colEnd")), "Method does not exist!"));
            }
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
