package pt.up.fe.comp2023.jmm.ollir;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2023.JavammBaseListener;

import static pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable.getType;
import static pt.up.fe.comp2023.jmm.ollir.OllirUtils.getOllirStringType;

public class ExprToOllir extends PreorderJmmVisitor<Void, ExprCodeResult> {
    private int counter;

    public ExprToOllir(){

        this.counter = 0;
    }

    @Override
    protected void buildVisitor() {
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Assignment", this::dealWithVarDecl);
        addVisit("Integer", this::dealWithInteger);
        addVisit("Identifier",this::dealWithIdentifier);
        setDefaultValue(() -> new ExprCodeResult("", ""));
    }

    private ExprCodeResult dealWithIdentifier(JmmNode jmmNode, Void unused) {
        return new ExprCodeResult("", jmmNode.get("value"));
    }

    private ExprCodeResult dealWithInteger(JmmNode jmmNode, Void unused) {
        return new ExprCodeResult("", jmmNode.get("value"));
    }

    public String nextTempVar() {
        var tempVar = "temp_" + counter;
        counter++;
        return tempVar;
    }

    public ExprCodeResult dealWithVarDecl(JmmNode jmmNode, Void unused){
        return new ExprCodeResult("",jmmNode.get("name"));
    }

    public ExprCodeResult dealWithBinaryOp(JmmNode jmmNode, Void unused) {

        var lhsres = visit(jmmNode.getJmmChild(0));
        var rhsres = visit(jmmNode.getJmmChild(1));


        var code = new StringBuilder();
        code.append(lhsres.prefixCode());
        code.append(rhsres.prefixCode());


        var value = nextTempVar();
        //var type = getOllirStringType(getType(jmmNode, jmmNode.get("name")));
        //System.out.println("value: " + getOllirStringType(value));
        var op = OllirUtils.getReturnType(jmmNode.get("op"));
        code.append("\t\t").append(value).append(op).append(":=").append(op)
                .append(lhsres.value()).append(op).append(jmmNode.get("op")).append(op).append(rhsres.value()).append(op).append(";\n");

        return new ExprCodeResult(code.toString(), value);
    }

}
