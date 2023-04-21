package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable;
import pt.up.fe.comp2023.analysis.table.SymbolTableVisitor;
import pt.up.fe.specs.util.collections.SpecsCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Analysis implements JmmAnalysis {

    private ImplementedSymbolTable symbolTable;
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {

        List<Report> reports = new ArrayList<Report>();

        JmmNode node = jmmParserResult.getRootNode();
        SymbolTableVisitor visitor = new SymbolTableVisitor();
        AnalysisVisitor analysisVisitor = new AnalysisVisitor();
        Map<ImplementedSymbolTable, List<Report>> symbolTableAndReports = visitor.getSymbolTableAndReports(node);
        ImplementedSymbolTable symbolTable = symbolTableAndReports.entrySet().iterator().next().getKey();
        List<Report> symbolTableReports = symbolTableAndReports.entrySet().iterator().next().getValue();
        /*ImplementedSymbolTable symbolTable = visitor.getSymbolTable(node);
        List<Report> symbolTableReports = symbolTable.getReports(node);*/
        List<Report> analysisReports = analysisVisitor.getReports(node, symbolTable);

        if(symbolTableReports != null && symbolTableReports.size() > 0)
            reports.addAll(symbolTableReports);

        if(analysisReports != null && analysisReports.size() > 0)
            reports.addAll(analysisReports);

        System.out.println(reports);

        /*ImplementedSymbolTable symbolTable = new ImplementedSymbolTable();

        List<Report> symbolTableReports = new SymbolTableVisitor().visit(jmmParserResult.getRootNode(), symbolTable);*/

        /*if(reports.isEmpty())
            return new JmmSemanticsResult(jmmParserResult, symbolTable, Collections.emptyList());*/
        return new JmmSemanticsResult(jmmParserResult, symbolTable, reports);
    }
}
