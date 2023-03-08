package pt.up.fe.comp2023.analysis.table;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;

public class Method {
    private List<Symbol> parameters;
    private Type returnType;
    private List<Symbol> localVariables;

    public Method(List<Symbol> parameters, Type returnType, List<Symbol> localVariables) {
        this.parameters = parameters;
        this.returnType = returnType;
        this.localVariables = localVariables;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public void setParameters(List<Symbol> parameters) {
        this.parameters = parameters;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public List<Symbol> getLocalVariables() {
        return localVariables;
    }

    public void setLocalVariables(List<Symbol> localVariables) {
        this.localVariables = localVariables;
    }
}
