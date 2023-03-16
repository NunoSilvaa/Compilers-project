package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Method {

    private String name;
    private List<Map.Entry<Symbol, String>> parameters = new ArrayList<>();
    private Type returnType;
    private List<Symbol> localVariables;

    public Method(String name, List<Symbol> parameters, Type returnType) {
        this.name = name;
        for (Symbol parameter : parameters){
            this.parameters.add(Map.entry(parameter,parameter.getName()));
        }
        this.returnType = returnType;
    }

    public List<Symbol> getParameters() {

        List<Symbol> parameters = new ArrayList<>();

        for(Map.Entry<Symbol, String> parameter : this.parameters){
            parameters.add(parameter.getKey());
        }
        return parameters;
    }


    /*public void setParameters(List<Symbol> parameters) {
        this.parameters = parameters;
    }*/

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

    public String getName(){
        return name;
    }
}
