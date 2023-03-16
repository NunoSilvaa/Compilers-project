package pt.up.fe.comp2023.analysis.table;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImplementedSymbolTable implements SymbolTable {

    private final List<String> imports = new ArrayList<>();
    private String className, superClassName;
    private Map<Symbol, Boolean> fields = new HashMap<>();
    private List<Method> methods = new ArrayList<>();
    private Method current;

    @Override
    public List<String> getImports() {
        return this.imports;
    }

    /*public void setImports(List<String> imports) {
        this.imports = imports;
    }*/

    public void addImports(String importN) {
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

    public void addField(Symbol field, boolean isStatic) {
        this.fields.put(field, isStatic);
    }

    @Override
    public List<String> getMethods() {
        List<String> methodName = new ArrayList<>();
        for (Method m : methods){
            methodName.add(m.getName());
        }
        return methodName;
    }

    /*public void addMethod(String name, List<Symbol> parameters, Type returnType, List<Symbol> localVariables) {
        this.methods.put(name, new Method(parameters, returnType, localVariables));
    }*/

    @Override
    public Type getReturnType(String s) {

        for (Method m : methods){
            if(m.getName().equals(s)){
                return m.getReturnType();
            }
        }
        return null;
    }

    @Override
    public List<Symbol> getParameters(String s) {
        for (Method m : methods){
            if(m.getName().equals(s)){
                return m.getParameters();
            }
        }
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        return null;
    }
}
