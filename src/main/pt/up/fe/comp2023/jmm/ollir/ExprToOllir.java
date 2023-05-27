package pt.up.fe.comp2023.jmm.ollir;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable;
import static pt.up.fe.comp2023.jmm.ollir.OllirUtils.getOllirStringType;
import static pt.up.fe.comp2023.jmm.ollir.OllirUtils.getOllirType;

public class ExprToOllir extends PreorderJmmVisitor<Void, ExprCodeResult> {
    private int elifCounter;
    private int whileCounter;
    private int counter;
    private SymbolTable symbolTable;

    public ExprToOllir(SymbolTable symbolTable){
        this.symbolTable = symbolTable;
        this.elifCounter = 0;
        this.whileCounter = 0;
        this.counter = 0;
    }

    private String nextElif(){
        int res = elifCounter;
        elifCounter++;
        return Integer.toString(res);
    }

    private String nextWhile(){
        int res = whileCounter;
        whileCounter++;
        return Integer.toString(res);
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
        addVisit("ArrayAccessChain",this::dealWithArray);
        addVisit("Length", this::dealWithArrayLen);
        addVisit("NewArray", this::dealWithArrayDecl);
        addVisit("IfElseStatement", this::dealWithIfElse);
        addVisit("While", this::dealWithWhile);
        addVisit("Boolean",this::dealWithBoolean);
        setDefaultValue(() -> new ExprCodeResult("", ""));
    }

    private ExprCodeResult dealWithBoolean(JmmNode jmmNode, Void unused) {
        return new ExprCodeResult("",jmmNode.get("value") + ".bool");
    }

    private ExprCodeResult dealWithWhile(JmmNode jmmNode, Void unused) {
        String prefixCode = "";
        var whileCondition = visit(jmmNode.getJmmChild(0));
        String nextWhile = nextWhile();

        prefixCode += "\t\t"+ "goto while_cond_" + nextWhile + ";\nwhile_body_" + nextWhile + ":\n";

        for(JmmNode child: jmmNode.getJmmChild(1).getChildren()){
            var whileExpr = visit(child);
            prefixCode += whileExpr.prefixCode();
        }
        prefixCode += "\t\t"+"while_cond_" + nextWhile + ":\n";

        prefixCode += whileCondition.prefixCode();
        prefixCode += "\t\t" + "if(" + whileCondition.value() + ") goto while_body_" + nextWhile + ";\n";

        return new ExprCodeResult(prefixCode,"");
    }

    private ExprCodeResult dealWithIfElse(JmmNode jmmNode, Void unused) {
        String prefixCode = ""; String value = "";
        var boolCond = visit(jmmNode.getJmmChild(0));

        prefixCode += boolCond.prefixCode();
        String nextIf = nextElif();
        // Print the else stat first and the then main if stat
        prefixCode += "\t\t" + "if(" + boolCond.value() + ") goto if_then_" + nextIf + ";\n";
        // Get the else expression
        for (JmmNode child: jmmNode.getJmmChild(2).getChildren()){
            var ifExpr = visit(child);
            prefixCode += "\t\t" + ifExpr.prefixCode();
        }
        prefixCode += "\t\t"+"goto if_end_" + nextIf + ";\n";
        prefixCode += "\t\t"+"if_then_" + nextIf + ":\n";
        // Get the if expression
        for (JmmNode child: jmmNode.getJmmChild(1).getChildren()){
            var ifExpr = visit(child);
            prefixCode += "\t\t" + ifExpr.prefixCode();
        }
        prefixCode += "\t\t" + "if_end_" + nextIf + ":\n";

        return new ExprCodeResult(prefixCode, value);
    }

    private ExprCodeResult dealWithArrayDecl(JmmNode jmmNode, Void unused) {
        String value = ""; String prefixCode = "";
        var type = getOllirType(OllirUtils.getType(jmmNode));

        //System.out.println("type: "+ type);

        // Assign array indice
        ExprCodeResult rhs = visit(jmmNode.getJmmChild(1));
        String indTemp = nextTempVar();
        prefixCode = rhs.prefixCode();
        prefixCode +=  indTemp + type + " :=.i32 " + rhs.value() + ";\n";

        // Make new array line
        String newTemp = nextTempVar();
        value = newTemp + type;
        System.out.println(value);
        //hard code type
        prefixCode += "\t\t" + value + " :=" + ".array.i32" + " new(array, " + indTemp + type + ")" + ".array.i32" + ";\n";

        return new ExprCodeResult(prefixCode, value);
    }

    private ExprCodeResult dealWithArrayLen(JmmNode jmmNode, Void unused) {
        String value = nextTempVar() + ".i32";
        String prefixCode = "";

        //System.out.println("val" + value);
        prefixCode += value + " :=.i32 " + "arraylength(" + jmmNode.getJmmChild(0).get("value") + ".array.i32).i32;\n";
        System.out.println("prefix "+ prefixCode);

        return new ExprCodeResult(prefixCode, value);
    }

    private String getMethod(JmmNode jmmNode, String var){
        JmmNode node = jmmNode;
        while (!(node.getKind().equals("MetDeclaration"))){
            if (node.getKind().equals("MainDeclaration")) {
                return "main";
            }
            node = node.getJmmParent();
        }

        return node.get("methodName");
    }

    private ExprCodeResult dealWithArray(JmmNode jmmNode, Void unused) {
        String value = nextTempVar() + ".i32"; String prefixCode = "";

        var rhsCode = visit(jmmNode.getJmmChild(1));
        prefixCode += rhsCode.prefixCode();
        System.out.println(prefixCode);
        prefixCode += value + " :=.i32 " + jmmNode.getJmmChild(0).get("value") + ".array.i32[" + rhsCode.value() + "].i32;\n";

        return new ExprCodeResult(prefixCode, value);
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
                    prefixCode += value + varTy + " :=" + varTy + " getfield(this, " + jmmNode.get("value") + varTy + ")" + varTy + ";\n";
                    val = value + varTy;
                } else {
                    val = jmmNode.get("value");
                }
            }
        }

        return new ExprCodeResult(prefixCode, val);
    }


    private ExprCodeResult dealWithInteger(JmmNode jmmNode, Void unused) {
        return new ExprCodeResult("", jmmNode.get("value") + ".i32");
    }

    public String nextTempVar() {
        var tempVar = "temp_" + counter;
        counter++;
        return tempVar;
    }

    public ExprCodeResult dealWithVarDecl(JmmNode jmmNode, Void unused){
        String prefixCode = "";
        String lhsVar; String method = getMethod(jmmNode, jmmNode.get("assignmentName"));
        Type lhsVarType;

        if (((ImplementedSymbolTable) symbolTable).isLocalVar(jmmNode.get("assignmentName"),method)){
            lhsVarType = ((ImplementedSymbolTable) symbolTable).getLocalVarType(jmmNode.get("assignmentName"),method);
            lhsVar = jmmNode.get("assignmentName") + getOllirStringType(lhsVarType.getName());
        } else if (((ImplementedSymbolTable) symbolTable).isParam(jmmNode.get("value"),method)) {
            Pair<Type,Integer> pairType = ((ImplementedSymbolTable) symbolTable).getParameterType(jmmNode.get("value"),method);
            lhsVarType = pairType.a;
            lhsVar = "$" + pairType.b + "." + jmmNode.get("value") + getOllirStringType(lhsVarType.getName());
        } else {
            lhsVarType = ((ImplementedSymbolTable) symbolTable).getFieldType(jmmNode.get("value"));
            lhsVar = jmmNode.get("value") + getOllirStringType(lhsVarType.getName());
        }

        var lhsCode = new ExprCodeResult("", lhsVar);
        var rhsCode = visit(jmmNode.getJmmChild(0));

        String code = "";

        code += rhsCode.prefixCode();

        if (((ImplementedSymbolTable) symbolTable).isField(jmmNode.get("assignmentName")) && !((ImplementedSymbolTable) symbolTable).isParam(jmmNode.get("assignmentName"),method) && !((ImplementedSymbolTable) symbolTable).isLocalVar(jmmNode.get("assignmentName"),method)){
            code += "putfield(this, " + lhsCode.value() + ", " + rhsCode.value() + ").V;\n";
        } else {
            code += lhsCode.value() + " :=" + getOllirStringType(lhsVarType.getName()) + " " + rhsCode.value() + ";\n";
        }

        prefixCode += code;

        return new ExprCodeResult(prefixCode, lhsVar);
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
                .append(lhsres.value()).append(" ").append(jmmNode.get("op")).append(op).append(" ").append(rhsres.value()).append(";\n");

        return new ExprCodeResult(code.toString(), value + op);
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

        var identifierCode = visit(jmmNode.getJmmChild(0));
        String identifierName = identifierCode.value();
        // Check if virtual or static with identifier

        if(symbolTable.getImports().contains(identifierName)){
            code.append(identifierCode.prefixCode()).append("\t\tinvokestatic(").append(identifierName).append(", \"").append(jmmNode.get("methodCallName")).append("\"");
        } else {
            String type = "";
            if (!identifierName.equals("this")) type = getType(jmmNode,identifierName);
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

            code.append(", ").append(param.value());
        }

        code.append(")").append(".i32;\n");


        return new ExprCodeResult(code.toString(),value);
    }

    public ExprCodeResult dealWithThis(JmmNode jmmNode, Void unused){
        return new ExprCodeResult("", "this");
    }

}
