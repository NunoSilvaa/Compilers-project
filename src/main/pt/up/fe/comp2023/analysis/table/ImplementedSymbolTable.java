package pt.up.fe.comp2023.analysis.table;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImplementedSymbolTable implements SymbolTable {

    private final List<String> imports = new ArrayList<>();
    private String className, superClassName;
    private Map<Symbol, Boolean> fields = new HashMap<>();
    //private List<Method> methods = new ArrayList<>();
    private final HashMap<String, Method> methods = new HashMap<>();
    private Method current;

    @Override
    public List<String> getImports() {
        return this.imports;
    }

    /*public void setImports(List<String> imports) {
        this.imports = imports;
    }*/

    public void setImports(String importN) {
        imports.add(importN);
    }

    @Override
    public String getClassName() {
        return this.className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String getSuper() {
        return this.superClassName;
    }

    public void setSuper(String superClassName) {
        this.superClassName = superClassName;
    }

    @Override
    public List<Symbol> getFields() {
        return new ArrayList<>(this.fields.keySet());
    }

    public void setField(Symbol field, boolean isStatic) {
        this.fields.put(field, isStatic);
    }

    public static Type getType(JmmNode jmmNode, String attribute) {
        String strType = jmmNode.get(attribute);
        Type type = new Type(strType, jmmNode.hasAttribute("isArray"));
        return type;
    }

    @Override
    public List<String> getMethods() {
        return List.copyOf(this.methods.keySet());
    }

    public void addMethod(String name, Type returnType, List<Symbol> localVariables, List<Symbol> parameters, List<Symbol> assignments){
        this.current = new Method(name);
        for(Symbol localVariable : localVariables){
            current.setLocalVariable(localVariable);
        }
        for(Symbol parameter: parameters){
            current.setParameters(parameter);
        }
        for(Symbol assignment: assignments){
            current.setAssignment(assignment);
        }
        current.setReturnType(returnType);
        this.methods.put(name, current);
    }

    public Method getCurrentMethod() {
        return this.current;
    }

    @Override
    public Type getReturnType(String s) {
        return this.methods.get(s).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String s) {
        return this.methods.get(s).getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        return this.methods.get(s).getLocalVariables();
    }

    public List<Symbol> getAssignments(String s) {
        return this.methods.get(s).getAssignments();
    }

    public Type getVariableType(String method, String variable){
        if(getLocalVariables(method).isEmpty()) {
            return new Type("impossible", false);
        }
        for(Symbol symbol : getLocalVariables(method)){
            if(symbol.getName().equals(variable)){
                return symbol.getType();
            }
        }
        return new Type("impossible",false);
    }

}
