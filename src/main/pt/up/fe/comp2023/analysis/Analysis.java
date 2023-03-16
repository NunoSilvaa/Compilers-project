package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class Analysis implements JmmAnalysis {

    private ImplementedSymbolTable symbolTable;
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {


        ImplementedSymbolTable symbolTable = new ImplementedSymbolTable();
        List<Report> reports = new ArrayList<>();

        /*if (jmmParserResult.getConfig().getOrDefault("debug", "false").equals("true")) {
            System.out.println("AST:\n");
            System.out.println(jmmParserResult.getRootNode().toTree());
            System.out.println("Symbol Table:\n");
            symbolTable.print();
        }*/
        JmmNode node = jmmParserResult.getRootNode();
        SymbolTableVisitor visitor = new SymbolTableVisitor(symbolTable, reports);
        visitor.visit(node, "");

        return new JmmSemanticsResult(jmmParserResult, symbolTable, reports);
    }
}
