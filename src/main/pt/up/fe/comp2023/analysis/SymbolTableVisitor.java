package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;
import java.util.stream.Collectors;

public class SymbolTableVisitor extends PreorderJmmVisitor<String, String> {

    private ImplementedSymbolTable symbolTable;
    private List<Report> reports;

    @Override
    protected void buildVisitor(){}
    public SymbolTableVisitor (ImplementedSymbolTable symbolTable, List<Report> reports) {
        this.symbolTable = symbolTable;
        this.reports = reports;

        addVisit("Program", this::dealWithProgram);
        addVisit("ImportDeclaration", this::dealWithImport);
        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);

        setDefaultVisit(this::defaultVisit);
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
        return null;
    }

    private String dealWithImport(JmmNode jmmNode, String s) {
        symbolTable.addImports(jmmNode.get("name"));
        return s + "IMPORT";
    }
    private String dealWithMethodDeclaration(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithClassDeclaration(JmmNode jmmNode, String s) {
        return null;
    }
}
