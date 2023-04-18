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

import java.util.ArrayList;
import java.util.Arrays;
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
        addVisit("Identifier", this::visitVariable);
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
        node.getChildren().get(0);
        JmmNode methodNode = node;
        while (!methodNode.hasAttribute("methodName")){
            methodNode = methodNode.getJmmParent();
        }
        Type lhsType = symbolTable.getVariableType(methodNode.get("methodName"), node.getChildren().get(0).get("value"));
        //Type lhsType = getType(node.getChildren().get(0), "ty");
        //Type lhsType = getType(node.getChildren().get(0), "ty");
        Type rhsType = symbolTable.getVariableType(methodNode.get("methodName"), node.getChildren().get(1).get("value"));
        //Type rhsType = getType(node.getChildren().get(1), "ty");

        if(!lhsType.equals(rhsType)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "Both operands must be integers!"));
        }
        return s;
    }

    private String visitVariable(JmmNode node, String s){
        JmmNode methodNode = node;
        while (!methodNode.hasAttribute("methodName")){
            methodNode = methodNode.getJmmParent();
        }

        if(!symbolTable.getLocalVariables(methodNode.get("methodName")).contains(node.get("value"))){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "Variable not declared!"));
        }

        return s;}

    private String visitAssignment(JmmNode node, String s){
        JmmNode methodNode = node;
        while (!methodNode.hasAttribute("methodName")){
            methodNode = methodNode.getJmmParent();
        }
        for(Symbol local : symbolTable.getLocalVariables(methodNode.get("methodName"))){

        }
        Type lhsType = symbolTable.getVariableType(methodNode.get("methodName"), node.getChildren().get(0).get("value"));
        //Type lhsType = getType(node.getChildren().get(0), "ty");
        Type rhsType = symbolTable.getVariableType(methodNode.get("methodName"), node.getChildren().get(1).get("value"));
        //Type rhsType = getType(node.getChildren().get(1), "ty");

        if(!lhsType.equals(rhsType)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "Both operands must be integers!"));
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
