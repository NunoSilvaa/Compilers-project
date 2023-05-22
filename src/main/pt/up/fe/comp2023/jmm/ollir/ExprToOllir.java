package pt.up.fe.comp2023.jmm.ollir;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2023.JavammBaseListener;
import pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable;
import static pt.up.fe.comp2023.jmm.ollir.OllirUtils.getOllirStringType;
import static pt.up.fe.comp2023.jmm.ollir.OllirUtils.getOllirType;

public class ExprToOllir extends PreorderJmmVisitor<Void, ExprCodeResult> {
    private int counter;
    private SymbolTable symbolTable;

    public ExprToOllir(SymbolTable symbolTable){
        this.symbolTable = symbolTable;
        this.counter = 0;
    }

    @Override
    protected void buildVisitor() {
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Assignment", this::dealWithVarDecl);
        addVisit("Integer", this::dealWithInteger);
        addVisit("Identifier",this::dealWithIdentifier);
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("This", this::dealWithThis);
        addVisit("MethodCall", this::dealWithCallMethod);
        setDefaultValue(() -> new ExprCodeResult("", ""));
    }

    private String getMethod(JmmNode jmmNode, String var){
        JmmNode node = jmmNode;
        while (!(node.getKind().equals("MetDeclaration"))){
            if (node.getKind().equals("MainDeclaration")) {
                return node.get("methodName");
            }
            node = node.getJmmParent();
        }

        return node.get("methodName");
    }


    private ExprCodeResult dealWithIdentifier(JmmNode jmmNode, Void unused) {
        String prefixCode = "";
        String value;
        Type varType = ((ImplementedSymbolTable) symbolTable).getLocalVarType(jmmNode.get("value"), getMethod(jmmNode, jmmNode.get("value")));
        String varTy, val;
        if (varType != null){
            varTy = getOllirStringType(varType.getName());
            val = jmmNode.get("value") + varTy;
        } else {
            String methodName = getMethod(jmmNode, jmmNode.get("value"));
            Pair<Type, Integer> paramPair= ((ImplementedSymbolTable) symbolTable).getParameterType(jmmNode.get("value"), methodName);
            if (paramPair != null) {
                varType = paramPair.a;
                varTy = getOllirStringType(varType.getName());
                val = "$" + paramPair.b + "." + jmmNode.get("value") + varTy;
            } else {
                varType = ((ImplementedSymbolTable) symbolTable).getFieldType(jmmNode.get("value"));
                if (varType != null) {
                    varTy = getOllirStringType(varType.getName());
                    value = nextTempVar();
                    prefixCode += value.substring(0, value.length() - 4) + varTy + " :=" + varTy + " getfield(this, " + jmmNode.get("value") + varTy + ")" + varTy + ";\n";
                    val = value.substring(0, value.length() - 4) + varTy;
                } else {
                    val = jmmNode.get("value");
                }
            }
        }

        return new ExprCodeResult(prefixCode, val);
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
        return new ExprCodeResult("",jmmNode.get("assignmentName"));
    }

    public ExprCodeResult dealWithBinaryOp(JmmNode jmmNode, Void unused) {

        var lhsres = visit(jmmNode.getJmmChild(0));
        var rhsres = visit(jmmNode.getJmmChild(1));


        var code = new StringBuilder();
        code.append(lhsres.prefixCode());
        code.append(rhsres.prefixCode());


        var value = nextTempVar();
        //var type = getOllirStringType(getType(jmmNode, jmmNode.get("name")));
        var op = OllirUtils.getReturnType(jmmNode.get("op"));
        code.append("\t\t").append(value).append(op).append(" ").append(":=").append(op).append(" ")
                .append(lhsres.value()).append(op).append(" ").append(jmmNode.get("op")).append(op).append(" ").append(rhsres.value()).append(op).append(";\n");

        return new ExprCodeResult(code.toString(), value);
    }

    public ExprCodeResult dealWithNewObject(JmmNode jmmNode, Void unused) {
        String value = nextTempVar();
        value += "." + jmmNode.get("value");

        String code = value + " :=." + jmmNode.get("value") + " new(" + jmmNode.get("value") + ")." + jmmNode.get("value") + ";\n";

        code += "\t\tinvokespecial(" + value + ",\"<init>\").V;\n";

        return new ExprCodeResult(code, value);
    }

    public String getType(JmmNode jmmNode, String var) {
        JmmNode node = jmmNode;

        while (!node.getKind().equals("MetDeclaration")){
            if (node.getKind().equals("MainDeclaration")) {
                for(Symbol varType : symbolTable.getLocalVariables("main")){
                    if (varType.getName().equals(var)) return varType.getType().getName();
                }
                return "";
            }
            node = node.getJmmParent();
        }
        String methodName = node.get("methodName");

        for(Symbol varType : symbolTable.getLocalVariables(methodName)){
            if (varType.getName().equals(var))
                return varType.getType().getName();
        }
        return "";
    }

    public ExprCodeResult dealWithCallMethod(JmmNode jmmNode, Void unused){
        var code = new StringBuilder();
        String value = nextTempVar();
        // Get identifier --> this, import ou outro qql
        var identifierCode = visit(jmmNode.getJmmChild(0));
        String identifierName = identifierCode.value();
        // Check if virtual or static with identifier
        // If import or superclass -> static / else virtual
        // Get parameters

        if(symbolTable.getImports().contains(identifierName)){
            code.append(identifierCode.prefixCode()).append("\t\tinvokestatic(").append(identifierName).append(", \"").append(jmmNode.get("methodCallName")).append("\"");
        } else {
            // Because it's virtual, he have to get the type of the identifier
            String type = "";
            if (!identifierName.equals("this")) type = "." + getType(jmmNode,identifierName);
            //if (jmmNode.getJmmChild(0).equals("this")) type = " ";
            code.append(identifierCode.prefixCode()).append("\t\t"+ value).append(".i32").append(" :=.i32 ").append("invokevirtual(").append(identifierName).append(type).append(",\"").append(jmmNode.get("methodCallName")).append("\"");
        }
        int i = 0;
        for (var child : jmmNode.getChildren()) {
            if(i == 0) {
                i++;
                continue;
            }
            var param = visit(child);
            //change type later;
            var type =  getType(jmmNode,param.value());
            type = "int";
            code.append(", ").append(param.value()).append(getOllirStringType(type));
        }

        code.append(")").append(".i32;\n");


        return new ExprCodeResult(code.toString(),value);
    }

    public ExprCodeResult dealWithThis(JmmNode jmmNode, Void unused){
        return new ExprCodeResult("", "this");
    }

}
