package pt.up.fe.comp2023.jmm.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.analysis.table.ImplementedSymbolTable;

import java.util.Arrays;
import java.util.List;

public class OllirUtils {

    public static String getCode(Symbol symbol) {
        return symbol.getName() + getOllirType(symbol.getType());
    }

        public static String getOllirType(Type jmmType) {
        switch (jmmType.getName()) {
            case "void" -> {
                return ".V";
            }
            case "boolean" -> {
                return ".bool";
            }
            case "int" -> {
                if (jmmType.isArray()) {
                    return ".array.i32";
                } else {
                    return ".i32";
                }
            }
            case "String" -> {
                return ".array.String";
            }
            default -> {
                return "." + jmmType.getName();
            }
        }
    }

    public static String getOllirStringType(String type) {
        return switch (type) {
            case "static void" -> ".V";
            case "boolean" -> ".bool";
            case "int" -> ".i32";
            case "Integer"  -> ".i32";
            default -> type;
        };
    }

    public static String getBooleanValue(String value) {
        return switch (value) {
            case "true" -> "1";
            case "false" -> "0";
            default -> "// invalid bool value\n";
        };
    }

}