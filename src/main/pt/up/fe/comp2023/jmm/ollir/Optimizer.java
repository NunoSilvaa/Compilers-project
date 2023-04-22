package pt.up.fe.comp2023.jmm.ollir;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable;

public class Optimizer implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {

        var ollirGenerator = new OllirGenerator(jmmSemanticsResult.getSymbolTable());
        ollirGenerator.visit(jmmSemanticsResult.getRootNode());

        var ollirCode = ollirGenerator.getCode();

        return new OllirResult(jmmSemanticsResult,ollirCode,jmmSemanticsResult.getReports());
    }
}
