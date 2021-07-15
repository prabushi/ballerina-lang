package org.ballerinalang.langserver.highlighting;

import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Package;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic token provider class.
 */
public class SemanticTokenProvider {

    private final List<String> classes;
    private final List<String> types;
    private final List<String> constants;
    private final List<String> enumMembers;

    public SemanticTokenProvider() {

        classes = new ArrayList<>();
        types = new ArrayList<>();
        constants = new ArrayList<>();
        enumMembers = new ArrayList<>();
    }

    public void getSemanticTokens(Package currentPackage, List<Integer> data, SyntaxTree syntaxTree) {

        currentPackage.moduleIds().forEach(moduleId -> {
            List<Symbol> symbolList = currentPackage.getCompilation().getSemanticModel(moduleId).moduleSymbols();
//            currentPackage.getCompilation().getSemanticModel(moduleId).symbol()
            symbolList.stream().filter(symbol -> symbol.kind() == SymbolKind.CLASS).forEach(classSymbol -> {
                if (classSymbol.getName().isPresent()) {
                    classes.add(classSymbol.getName().get());
                }
            });

            symbolList.stream().filter(symbol -> symbol.kind() == SymbolKind.TYPE).forEach(typeSymbol -> {
                if (typeSymbol.getName().isPresent()) {
                    types.add(typeSymbol.getName().get());
                }
            });

            symbolList.stream().filter(symbol -> symbol.kind() == SymbolKind.CONSTANT).forEach(constSymbol -> {
                if (constSymbol.getName().isPresent()) {
                    constants.add(constSymbol.getName().get());
                }
            });

            symbolList.stream().filter(symbol -> symbol.kind() == SymbolKind.ENUM_MEMBER).forEach(enumSymbol -> {
                if (enumSymbol.getName().isPresent()) {
                    enumMembers.add(enumSymbol.getName().get());
                }
            });
        });
//        SemanticTokenVisitor semanticTokenVisitor = new SemanticTokenVisitor(data, this);
//        semanticTokenVisitor.visitSemanticTokens(syntaxTree.rootNode());
    }

    public List<String> getClasses() {

        return classes;
    }

    public List<String> getTypes() {

        return types;
    }

    public List<String> getConstants() {

        return constants;
    }

    public List<String> getEnumMembers() {

        return enumMembers;
    }
}
