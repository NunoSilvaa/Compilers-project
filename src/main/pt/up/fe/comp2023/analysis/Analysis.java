package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsCollections;

import java.util.List;

public class Analysis implements JmmAnalysis {

    ImplementedSymbolTable symbolTable = new ImplementedSymbolTable();

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {

        List<Report> reports = new SymbolTableConstructor().visit(jmmParserResult.getRootNode(), symbolTable);

        if (jmmParserResult.getConfig().getOrDefault("debug", "false").equals("true")) {
            System.out.println("AST:\n");
            System.out.println(jmmParserResult.getRootNode().toTree());
            System.out.println("Symbol Table:\n");
            symbolTable.print();
        }

        return new JmmSemanticsResult(jmmParserResult, symbolTable, reports);
    }
}
