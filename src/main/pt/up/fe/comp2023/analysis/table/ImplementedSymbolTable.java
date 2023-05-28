package pt.up.fe.comp2023.analysis.table;

import org.antlr.v4.runtime.misc.Pair;
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
        boolean isArray = Boolean.parseBoolean(jmmNode.get("isArray"));
        Type type = new Type(strType, isArray);

        return type;
    }

    @Override
    public List<String> getMethods() {
        return List.copyOf(this.methods.keySet());
    }

    public List<Method> getMethodsList() {
        return new ArrayList<>(this.methods.values());
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
        if(this.methods.get(s) == null) return new ArrayList<>();
        return this.methods.get(s).getParameters();
    }

    public List<String> getParametersNames(String s) {
        List<String> parametersNames = new ArrayList<>();
        for(Symbol parameter : this.methods.get(s).getParameters()){
            parametersNames.add(parameter.getName());
        }
        return parametersNames;
    }

    public List<String> getAssignmentNames(String s) {
        List<String> assignmentNames = new ArrayList<>();
        for(Symbol parameter : this.methods.get(s).getAssignments()){
            assignmentNames.add(parameter.getName());
        }
        return assignmentNames;
    }

    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        for(Symbol field : this.fields.keySet()){
            fieldNames.add(field.getName());
        }
        return fieldNames;
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        if(this.methods.get(s) == null) return new ArrayList<>();
        return this.methods.get(s).getLocalVariables();
    }

    public List<Symbol> getAssignments(String s) {
        return this.methods.get(s).getAssignments();
    }

    public boolean isParameter(String var, String method){
        for (Symbol param: this.getParameters(method)) {
            if (param.getName().equals(var)) return true;
        }
        return false;
    }

    public Type getFieldType(String var){
        for (Symbol f: fields.keySet()){
            if(f.getName().equals(var)) return f.getType();
        }
        return null;
    }

    public Pair<Type, Integer> getParameterType(String var, String method){
        int i=1;
        for (Symbol p: getParameters(method)){
            if(p.getName().equals(var)) return new Pair<>(p.getType(), i);
            i++;
        }
        return null;
    }

    public Type getLocalVarType(String var, String method){
        for (Symbol v: getLocalVariables(method)){
            if (v.getName().equals(var)) return v.getType();
        }
        return null;
    }

    public boolean isLocalVar(String s, String method){
        return getLocalVarType(s, method) != null;
    }

    public boolean isParam(String s, String method){
        return getParameterType(s, method) != null;
    }

    public boolean isField(String s){
        return getFieldType(s) != null;
    }
}
