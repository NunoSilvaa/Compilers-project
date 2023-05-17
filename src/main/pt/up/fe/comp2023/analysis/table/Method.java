package pt.up.fe.comp2023.analysis.table;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Method {

    private String name;
    private final Map<String, Symbol> parameters;
    private Type returnType;
    private final Map<String, Symbol> localVariables;

    private final Map<String, Symbol> assignments;

    //private final List<String> assignmentNames;

    private final Map<String, String> assignmentValues;

    public Method(String name) {
        this.name = name;
        this.parameters = new HashMap<String, Symbol>();
        this.localVariables = new HashMap<String, Symbol>();
        this.assignments = new HashMap<String, Symbol>();
        this.assignmentValues = new HashMap<String, String>();
    }

    public List<String> getParameterTypes() {
        List<String> params = new ArrayList<>();

        for (Map.Entry<String, Symbol> parameter : parameters.entrySet()) {
            params.add(parameter.getKey());
        }

        return params;
    }

    public List<Symbol> getParameters() {
        return new ArrayList<>(this.parameters.values());
    }

    public void setParameters(Symbol parameter) {
        this.parameters.put(parameter.getName(),parameter);
    }

    public Type getReturnType() {
        return this.returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public List<Symbol> getLocalVariables() {
        return new ArrayList<>(this.localVariables.values());
    }

    public List<String> getLocalVariablesNames(){
        List<String> localVariablesNames = new ArrayList<>();
        for(Symbol localVariable : this.localVariables.values()){
            localVariablesNames.add(localVariable.getName());
        }
        return localVariablesNames;
    }

    public List<Symbol> getAssignments() {
        return new ArrayList<>(this.assignments.values());
    }

    public List<String> getAssignmentValues() {
        return new ArrayList<>(this.assignmentValues.values());
    }


    public void setLocalVariable(Symbol variable) {
        this.localVariables.put(variable.getName(), variable);
    }

    public void setAssignment(Symbol symbol){
        this.assignments.put(symbol.getName(), symbol);
    }

    /*public void setUsedVariable(String name){
        this.usedVariables.add(name);
    }*/

    public void setAssignmentValues(String name, String value){
        this.assignmentValues.put(name, value);
    }

    public String getName(){
        return this.name;
    }
}
