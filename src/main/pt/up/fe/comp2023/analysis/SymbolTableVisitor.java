package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable;

import java.util.List;

public class SymbolTableVisitor extends AJmmVisitor<ImplementedSymbolTable, List<Report>> {

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("importDeclaration", this::dealWithImport);
        addVisit("methodDeclaration", this::dealWithMethodDeclaration);
        addVisit("classDeclaration", this::dealWithClassDeclaration);
    }

    private List<Report> dealWithProgram(JmmNode jmmNode, ImplementedSymbolTable implementedSymbolTable) {
        return null;
    }

    private List<Report> dealWithImport(JmmNode jmmNode, ImplementedSymbolTable implementedSymbolTable) {
        return null;
    }

    private List<Report> dealWithMethodDeclaration(JmmNode jmmNode, ImplementedSymbolTable implementedSymbolTable) {
        return null;
    }

    private List<Report> dealWithClassDeclaration(JmmNode jmmNode, ImplementedSymbolTable implementedSymbolTable) {

        implementedSymbolTable.setClassName(jmmNode.get("className"));
        implementedSymbolTable.setSuper(jmmNode.get("superClassName"));

        return null;
    }
}
