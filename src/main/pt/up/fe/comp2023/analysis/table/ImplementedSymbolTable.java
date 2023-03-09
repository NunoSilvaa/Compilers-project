package pt.up.fe.comp2023.analysis.table;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2023.analysis.table.Method;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImplementedSymbolTable implements SymbolTable {

    private List<String> imports;
    private String className, superClassName;
    private List<Symbol> fields;
    private Map<String, Method> methods;

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClassName;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    @Override
    public List<String> getMethods() {
        return new ArrayList<>(this.methods.keySet());
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
}
