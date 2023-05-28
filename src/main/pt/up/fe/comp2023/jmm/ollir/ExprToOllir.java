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
        addVisit("BracketsAssignment", this::dealWithArrayAssign);
        addVisit("IfElseStatement", this::dealWithIfElse);
        addVisit("While", this::dealWithWhile);
        addVisit("Boolean",this::dealWithBoolean);
        addVisit("Negation", this::dealWithDenial);
        setDefaultValue(() -> new ExprCodeResult("", ""));
    }

    private ExprCodeResult dealWithDenial(JmmNode jmmNode, Void unused) {
        var value = nextTempVar() + ".bool";
        var prefixCode = new StringBuilder();
        var rhsCode = visit(jmmNode.getJmmChild(0));
        prefixCode.append(rhsCode.prefixCode());
        prefixCode.append("\t\t").append(value).append(" :=.bool ").append("!.bool ").append(rhsCode.value()).append(";\n");

        return new ExprCodeResult(prefixCode.toString(),value);
    }

    private ExprCodeResult dealWithBoolean(JmmNode jmmNode, Void unused) {
        return new ExprCodeResult("",jmmNode.get("value") + ".bool");
    }

    private ExprCodeResult dealWithWhile(JmmNode jmmNode, Void unused) {
        var  prefixCode = new StringBuilder();
        var whileCondition = visit(jmmNode.getJmmChild(0));
        String nextWhile = nextWhile();

        prefixCode.append("\t\t" + "goto while_cond_").append(nextWhile).append(";\nwhile_body_").append(nextWhile).append(":\n");

        for(JmmNode child: jmmNode.getJmmChild(1).getChildren()){
            var whileExpr = visit(child);
            prefixCode.append(whileExpr.prefixCode());
        }
        prefixCode.append("\t\t" + "while_cond_").append(nextWhile).append(":\n");

        prefixCode.append(whileCondition.prefixCode());
        prefixCode.append("\t\t" + "if(").append(whileCondition.value()).append(") goto while_body_").append(nextWhile).append(";\n");

        return new ExprCodeResult(prefixCode.toString(),"");
    }

    private ExprCodeResult dealWithIfElse(JmmNode jmmNode, Void unused) {
        String value = "";
        var prefixCode = new StringBuilder();
        var boolCond = visit(jmmNode.getJmmChild(0));

        prefixCode.append(boolCond.prefixCode());
        String nextIf = nextElif();
        // Print the else stat first and the then main if stat
        prefixCode.append("\t\t" + "if(").append(boolCond.value()).append(") goto if_then_").append(nextIf).append(";\n");
        // Get the else expression
        for (JmmNode child: jmmNode.getJmmChild(2).getChildren()){
            var ifExpr = visit(child);
            prefixCode.append("\t\t").append(ifExpr.prefixCode());
        }
        prefixCode.append("\t\t" + "goto if_end_").append(nextIf).append(";\n");
        prefixCode.append("\t\t" + "if_then_").append(nextIf).append(":\n");
        // Get the if expression
        for (JmmNode child: jmmNode.getJmmChild(1).getChildren()){
            var ifExpr = visit(child);
            prefixCode.append("\t\t").append(ifExpr.prefixCode());
        }
        prefixCode.append("\t\t" + "if_end_").append(nextIf).append(":\n");

        return new ExprCodeResult(prefixCode.toString(), value);
    }

    private ExprCodeResult dealWithArrayDecl(JmmNode jmmNode, Void unused) {
        String value = "";
        var prefixCode = new StringBuilder();
        var type = getOllirType(OllirUtils.getType(jmmNode));

        ExprCodeResult rhs = visit(jmmNode.getJmmChild(1));
        String indTemp = nextTempVar();
        prefixCode.append(rhs.prefixCode());
        prefixCode.append(indTemp).append(type).append(" :=.i32 ").append(rhs.value()).append(";\n");

        // Make new array line
        String newTemp = nextTempVar();
        value = newTemp + ".array.i32";

        //hard code type
        prefixCode.append("\t\t").append(value).append(" :=").append(".array.i32").append(" new(array, ").append(indTemp).append(type).append(")").append(".array.i32").append(";\n");

        return new ExprCodeResult(prefixCode.toString(), value);
    }

    private ExprCodeResult dealWithArrayLen(JmmNode jmmNode, Void unused) {
        String value = nextTempVar() + ".i32";
        var prefixCode = new StringBuilder();

        prefixCode.append(value).append(" :=.i32 ").append("arraylength(").append(jmmNode.getJmmChild(0).get("value")).append(".array.i32).i32;\n");

        return new ExprCodeResult(prefixCode.toString(), value);
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
        String value = nextTempVar() + ".i32";
        var prefixCode = new StringBuilder();

        var rhsCode = visit(jmmNode.getJmmChild(1));
        prefixCode.append(rhsCode.prefixCode());

        prefixCode.append(value).append(" :=.i32 ").append(jmmNode.getJmmChild(0).get("value")).append(".array.i32[").append(rhsCode.value()).append("].i32;\n");

        return new ExprCodeResult(prefixCode.toString(), value);
    }


    private ExprCodeResult dealWithIdentifier(JmmNode jmmNode, Void unused) {
        var prefixCode = new StringBuilder();
        String value;
        Type varType = ((ImplementedSymbolTable) symbolTable).getLocalVarType(jmmNode.get("value"), getMethod(jmmNode, jmmNode.get("value")));
        String varTy, val;
        if (varType != null){
            varTy = getOllirStringType(varType.getName());
            val = jmmNode.get("value") + varTy;
        } else {
            String methodName = getMethod(jmmNode, jmmNode.get("value"));
            Pair<Type, Integer> paramPair = ((ImplementedSymbolTable) symbolTable).getParameterType(jmmNode.get("value"), methodName);
            if (paramPair != null) {
                varType = paramPair.a;
                varTy = getOllirStringType(varType.getName());
                val = "$" + paramPair.b + "." + jmmNode.get("value") + varTy;
            } else {
                varType = ((ImplementedSymbolTable) symbolTable).getFieldType(jmmNode.get("value"));
                if (varType != null) {
                    varTy = getOllirStringType(varType.getName());
                    value = nextTempVar();
                    prefixCode.append(value).append(varTy).append(" :=").append(varTy).append(" getfield(this, ").append(jmmNode.get("value")).append(varTy).append(")").append(varTy).append(";\n");
                    val = value + varTy;
                } else {
                    val = jmmNode.get("value");
                }
            }
        }

        return new ExprCodeResult(prefixCode.toString(), val);
    }


    private ExprCodeResult dealWithInteger(JmmNode jmmNode, Void unused) {
        var value = jmmNode.get("value") + ".i32";

        return new ExprCodeResult("", value);
    }

    public String nextTempVar() {
        var tempVar = "temp_" + counter;
        counter++;
        return tempVar;
    }

    public ExprCodeResult dealWithVarDecl(JmmNode jmmNode, Void unused){
        var prefixCode = new StringBuilder();
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

        prefixCode.append(code);

        return new ExprCodeResult(prefixCode.toString(), lhsVar);
    }

    public ExprCodeResult dealWithBinaryOp(JmmNode jmmNode, Void unused) {

        var lhsres = visit(jmmNode.getJmmChild(0));
        var rhsres = visit(jmmNode.getJmmChild(1));


        var code = new StringBuilder();
        code.append(lhsres.prefixCode());
        code.append(rhsres.prefixCode());


        var value = nextTempVar();
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
            code.append(identifierCode.prefixCode()).append("invokestatic(").append(identifierName).append(", \"").append(jmmNode.get("methodCallName")).append("\"");
        } else {
            String type = ""; value += ".i32";
            if (!identifierName.equals("this")) type = getType(jmmNode,identifierName);
            code.append(identifierCode.prefixCode()).append("\t\t"+ value).append(" :=.i32 ").append("invokevirtual(").append(identifierName).append(type).append(",\"").append(jmmNode.get("methodCallName")).append("\"");
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
    private ExprCodeResult dealWithArrayAssign(JmmNode jmmNode, Void unused) {
        var code = new StringBuilder();

        var rhsCode = visit(jmmNode.getJmmChild(1));
        var lhsCode = visit(jmmNode.getJmmChild(0));

        code.append(lhsCode.prefixCode());
        code.append(rhsCode.prefixCode());

        var value = nextTempVar();

        code.append(value).append(".i32 :=.i32 ").append(rhsCode.value()).append(";\n");
        code.append(jmmNode.get("bracketsAssignmentName")).append("[").append(lhsCode.value()).append("].i32 :=.i32 ").append(rhsCode.value()).append(";\n");

        return new ExprCodeResult(code.toString(),value);
    }

    public ExprCodeResult dealWithThis(JmmNode jmmNode, Void unused){
        var value = "." + symbolTable.getClassName();

        return new ExprCodeResult("", "this" + value);
    }

}

