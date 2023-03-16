package pt.up.fe.comp2023.analysis.table;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmSerializer;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable;

import java.util.List;
import java.util.stream.Collectors;

public class SymbolTableVisitor extends PreorderJmmVisitor<String, String> {
    private ImplementedSymbolTable symbolTable;
    private List<Report> reports;

    @Override
    protected void buildVisitor(){
        addVisit("Program", this::dealWithProgram);
        addVisit("ImportDeclaration", this::dealWithImport);
        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);

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
        s = ((s !=null) ? s : "");

        for(JmmNode child : node.getChildren()) {
            if(child.getKind().equals("ImportDeclaration")){
                String path = child.get("name");
                for (JmmNode grandChild : child.getChildren()){
                    path += '.' + grandChild.get("name");
                }
                this.symbolTable.addImports(path);
            }
            else {
                visit(child, null);
            }
        }
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
