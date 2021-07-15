package org.ballerinalang.langserver.highlighting;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassFieldSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.EnumSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.EnumDeclarationNode;
import io.ballerina.compiler.syntax.tree.EnumMemberNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MarkdownParameterDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.projects.Document;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import org.eclipse.lsp4j.SemanticTokenModifiers;
import org.eclipse.lsp4j.SemanticTokenTypes;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Visitor class for semantic tokens.
 */
public class SemanticTokenVisitor extends NodeVisitor {

    private static final List<String> TOKEN_TYPES = Arrays.asList(
            SemanticTokenTypes.Namespace, SemanticTokenTypes.Type, SemanticTokenTypes.Class, SemanticTokenTypes.Enum,
            SemanticTokenTypes.Interface, SemanticTokenTypes.Struct, SemanticTokenTypes.TypeParameter,
            SemanticTokenTypes.Parameter, SemanticTokenTypes.Variable, SemanticTokenTypes.Property,
            SemanticTokenTypes.EnumMember, SemanticTokenTypes.Event, SemanticTokenTypes.Function,
            SemanticTokenTypes.Method, SemanticTokenTypes.Macro, SemanticTokenTypes.Keyword,
            SemanticTokenTypes.Modifier, SemanticTokenTypes.Comment, SemanticTokenTypes.String,
            SemanticTokenTypes.Number, SemanticTokenTypes.Regexp, SemanticTokenTypes.Operator
    );

    private static final List<String> TOKEN_MODIFIERS = Arrays.asList(
            SemanticTokenModifiers.Declaration, SemanticTokenModifiers.Definition, SemanticTokenModifiers.Readonly,
            SemanticTokenModifiers.Static, SemanticTokenModifiers.Deprecated, SemanticTokenModifiers.Abstract,
            SemanticTokenModifiers.Async, SemanticTokenModifiers.Modification, SemanticTokenModifiers.Documentation,
            SemanticTokenModifiers.DefaultLibrary
    );

    private final List<Integer> data;
    private SemanticToken previousToken;
    private final Document document;
    private final SemanticModel semanticModel;
    private final String fileName;
    private final Set<SemanticToken> cache;

    public SemanticTokenVisitor(List<Integer> data, SemanticModel semanticModel, Document document, String fileName) {

        this.data = data;
        this.semanticModel = semanticModel;
        this.document = document;
        this.fileName = fileName;
        this.cache = new TreeSet<>(SemanticToken.semanticTokenComparator);
    }

    public void visitSemanticTokens(Node node) {

        visitSyntaxNode(node);
        // TODO processing
        this.cache.forEach(semanticToken -> {
            calculateRelativeTokenValues(semanticToken.getLine(), semanticToken.getStartChar());
            this.data.add(semanticToken.getLength());
            this.data.add(semanticToken.getType());
            this.data.add(semanticToken.getModifiers());
        });
    }

    public void visit(FunctionDefinitionNode functionDefinitionNode) {

        LinePosition startLine = functionDefinitionNode.functionName().lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
//            calculateRelativeTokenValues(startLine.line(), startLine.offset());
            int length = functionDefinitionNode.functionName().text().length();
            semanticToken.setLength(length);
            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Function));
            semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
//            this.data.add(length);
//            this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Function));
//            this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
            cache.add(semanticToken);
            handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.Function), 0);
        }
        visitSyntaxNode(functionDefinitionNode);
    }

//    public void visit(FunctionBodyBlockNode functionBodyBlockNode) {
//        this.localVariables = new ArrayList<>();
//        visitSyntaxNode(functionBodyBlockNode);
//    }

    public void visit(ExpressionStatementNode expressionStatementNode) {

        visitSyntaxNode(expressionStatementNode);
    }

    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {

        LinePosition startLine = functionCallExpressionNode.functionName().lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
//            calculateRelativeTokenValues(startLine.line(), startLine.offset());
//            this.data.add(functionCallExpressionNode.functionName().toString().trim().length());
//            this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Function));
//            this.data.add(0);
            semanticToken.setLength(functionCallExpressionNode.functionName().toString().trim().length());
            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Function));
            semanticToken.setModifiers(0);
            cache.add(semanticToken);
        }

        visitSyntaxNode(functionCallExpressionNode);
    }

    public void visit(RequiredParameterNode requiredParameterNode) {

        if (requiredParameterNode.paramName().isPresent()) {
            Token token = requiredParameterNode.paramName().get();
            LinePosition startLine = token.lineRange().startLine();
            SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
            if (!cache.contains(semanticToken)) {
                int length = token.toString().trim().length();
//                calculateRelativeTokenValues(startLine.line(), startLine.offset());
//                this.data.add(length);
//                this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Parameter));
//                this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
                semanticToken.setLength(length);
                semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Parameter));
                semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
                cache.add(semanticToken);
                handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.Parameter), 0);
            }
        }
        visitSyntaxNode(requiredParameterNode);
    }

    public void visit(CaptureBindingPatternNode captureBindingPatternNode) {

        LinePosition startLine = captureBindingPatternNode.lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
            int length = captureBindingPatternNode.toString().trim().length();
//            calculateRelativeTokenValues(startLine.line(), startLine.offset());
//            this.data.add(length);
//            this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Variable));
//            this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
            semanticToken.setLength(length);
            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Variable));
            semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
            cache.add(semanticToken);
            handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.Variable), 0);
        }
        visitSyntaxNode(captureBindingPatternNode);
    }

    public void visit(SimpleNameReferenceNode simpleNameReferenceNode) {

        LinePosition startLine = simpleNameReferenceNode.lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
            Optional<Symbol> symbol = this.semanticModel.symbol(simpleNameReferenceNode);
//        this.semanticModel.references()
            if (symbol.isPresent()) {
//                calculateRelativeTokenValues(startLine.line(), startLine.offset());
                String nodeName = simpleNameReferenceNode.toString().trim();
//                this.data.add(nodeName.length());
                semanticToken.setLength(nodeName.length());
                if (symbol.get() instanceof ClassSymbol) {
//                    this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Class));
//                    this.data.add(0);
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Class));
                    semanticToken.setModifiers(0);
                } else if (symbol.get() instanceof ClassFieldSymbol) {
//                    this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Property));
//                    this.data.add(0);
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Property));
                    semanticToken.setModifiers(0);
                } else if (symbol.get() instanceof ConstantSymbol) {
//                    this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Variable));
//                    this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Variable));
                    semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
                } else if (symbol.get() instanceof TypeDescTypeSymbol || symbol.get() instanceof RecordTypeSymbol
                        || symbol.get() instanceof TypeReferenceTypeSymbol) {
//                    this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Type));
//                    this.data.add(0);
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Type));
                    semanticToken.setModifiers(0);
                } else if (symbol.get() instanceof EnumSymbol) {
//                    this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.EnumMember));
//                    this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.EnumMember));
                    semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
//            } else if (localVariables.contains(nodeName)) {
//                this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Variable));
//                this.data.add(0);
                } else if (symbol.get() instanceof ParameterSymbol) {
//                    this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Parameter));
//                    this.data.add(0);
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Parameter));
                    semanticToken.setModifiers(0);
                } else if (symbol.get() instanceof FunctionSymbol) {
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Function));
                    semanticToken.setModifiers(0);
                } else {
//                    this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Struct)); // TODO
//                    this.data.add(0);
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Struct)); // TODO
                    semanticToken.setModifiers(0);
                }
            }
            cache.add(semanticToken);
        }
        visitSyntaxNode(simpleNameReferenceNode);
    }

//    public void visit(BuiltinSimpleNameReferenceNode builtinSimpleNameReferenceNode) {
//
//        LinePosition startLine = builtinSimpleNameReferenceNode.lineRange().startLine();
//        calculateRelativeTokenValues(startLine.line(), startLine.offset());
//        this.data.add(builtinSimpleNameReferenceNode.toString().trim().length());
//        this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Keyword));
//        this.data.add(0);
//        visitSyntaxNode(builtinSimpleNameReferenceNode);
//    }

    public void visit(ConstantDeclarationNode constantDeclarationNode) {

        Token token = constantDeclarationNode.variableName();
        LinePosition startLine = token.lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
            int length = token.toString().trim().length();
//            calculateRelativeTokenValues(startLine.line(), startLine.offset());
//            this.data.add(length);
//            this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Variable));
//            this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration)
//                    | 1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
            semanticToken.setLength(length);
            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Variable));
            semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration)
                    | 1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
            cache.add(semanticToken);
            handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.Variable),
                    1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
        }

        visitSyntaxNode(constantDeclarationNode);
    }

    public void visit(ClassDefinitionNode classDefinitionNode) {

        Token token = classDefinitionNode.className();
        LinePosition startLine = token.lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
//            calculateRelativeTokenValues(startLine.line(), startLine.offset());
            int length = token.text().trim().length();
//            this.data.add(length);
//            this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Class));
//            this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
            semanticToken.setLength(length);
            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Class));
            semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
            cache.add(semanticToken);
            handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.Class), 0);
        }
        visitSyntaxNode(classDefinitionNode);
    }

    public void visit(ServiceDeclarationNode serviceDeclarationNode) {

        serviceDeclarationNode.absoluteResourcePath().forEach(serviceName -> {
            LinePosition startLine = serviceName.lineRange().startLine();
//            calculateRelativeTokenValues(startLine.line(), startLine.offset());
//            this.data.add(serviceName.toString().trim().length());
//            this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Struct));
//            this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
            SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset(),
                    serviceName.toString().trim().length(), TOKEN_TYPES.indexOf(SemanticTokenTypes.Struct),
                    1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
            cache.add(semanticToken);
        });
        visitSyntaxNode(serviceDeclarationNode);
    }

    public void visit(EnumDeclarationNode enumDeclarationNode) {

        Node token = enumDeclarationNode.identifier();
        LinePosition startLine = token.lineRange().startLine();
//        calculateRelativeTokenValues(startLine.line(), startLine.offset());
//        this.data.add(token.toString().trim().length());
//        this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Enum));
//        this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset(),
                token.toString().trim().length(), TOKEN_TYPES.indexOf(SemanticTokenTypes.Enum),
                1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
        cache.add(semanticToken);
        visitSyntaxNode(enumDeclarationNode);
    }

    public void visit(EnumMemberNode enumMemberNode) {

        IdentifierToken token = enumMemberNode.identifier();
        LinePosition startLine = token.lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
//            calculateRelativeTokenValues(startLine.line(), startLine.offset());
            int length = token.text().trim().length();
//            this.data.add(length);
//            this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.EnumMember));
//            this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration)
//                    | 1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
            semanticToken.setLength(length);
            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.EnumMember));
            semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration)
                    | 1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
            cache.add(semanticToken);
            handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.EnumMember),
                    1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
        }
        visitSyntaxNode(enumMemberNode);
    }

    public void visit(MarkdownParameterDocumentationLineNode markdownParameterDocumentationLineNode) {

        Node token = markdownParameterDocumentationLineNode.parameterName();
        LinePosition startLine = token.lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
//            calculateRelativeTokenValues(startLine.line(), startLine.offset());
//            this.data.add(token.toString().length());
//            this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Parameter));
//            this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Documentation));
            semanticToken.setLength(token.toString().length());
            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Parameter));
            semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Documentation));
            cache.add(semanticToken);
        }
        visitSyntaxNode(markdownParameterDocumentationLineNode);
    }

    public void visit(TypeDefinitionNode typeDefinitionNode) {

        Token token = typeDefinitionNode.typeName();
        LinePosition startLine = token.lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
//            calculateRelativeTokenValues(startLine.line(), startLine.offset());
            int length = token.toString().trim().length();
//            this.data.add(length);
//            this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Type));
//            this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
            semanticToken.setLength(length);
            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Type));
            semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
            cache.add(semanticToken);
            handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.Type), 0);
        }
        visitSyntaxNode(typeDefinitionNode);
    }

    public void visit(RecordFieldNode recordFieldNode) {

        Token token = recordFieldNode.fieldName();
        LinePosition startLine = token.lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
//            calculateRelativeTokenValues(startLine.line(), startLine.offset());
            int length = token.toString().length();
//            this.data.add(length);
//            this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.TypeParameter));
            semanticToken.setLength(length);
            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.TypeParameter));
            if (recordFieldNode.readonlyKeyword().isPresent()) {
//                this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration)
//                        | 1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
                semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration)
                        | 1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
                cache.add(semanticToken);
                handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.TypeParameter),
                        1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
            } else {
//                this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
                semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
                cache.add(semanticToken);
                handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.TypeParameter), 0);
            }
        }
        visitSyntaxNode(recordFieldNode);
    }

    public void visit(ObjectFieldNode objectFieldNode) {
        //public int age;
        Token token = objectFieldNode.fieldName();
        LinePosition startLine = token.lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
//            calculateRelativeTokenValues(startLine.line(), startLine.offset());
            int length = token.toString().trim().length();
//            this.data.add(length);
//            this.data.add(TOKEN_TYPES.indexOf(SemanticTokenTypes.Property));
//            this.data.add(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
            semanticToken.setLength(length);
            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Property));
            semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
            cache.add(semanticToken);
            handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.Property), 0);
        }
        visitSyntaxNode(objectFieldNode);
    }

    private void calculateRelativeTokenValues(int line, int startChar) {

        int actualLine = line;
        int actualStartChar = startChar;
        if (this.previousToken != null) {
            if (line == this.previousToken.getLine()) {
                startChar -= this.previousToken.getStartChar();
            }
            line -= this.previousToken.getLine();
        }
        this.data.add(line);
        this.data.add(startChar);
        this.previousToken = new SemanticToken(actualLine, actualStartChar);
    }

    private void handleReferences(LinePosition linePosition, int length, int type, int modifiers) {

        List<Location> locations = this.semanticModel.references(this.document, linePosition);
        locations.stream().filter(location -> location.lineRange().filePath().equals(this.fileName))
                .forEach(location -> {
                    LinePosition position = location.lineRange().startLine();
                    SemanticToken semanticToken = new SemanticToken(position.line(), position.offset());
                    if (!cache.contains(semanticToken)) {
                        semanticToken.setLength(length);
                        semanticToken.setType(type);
                        semanticToken.setModifiers(modifiers);
                        cache.add(semanticToken);
//                        calculateRelativeTokenValues(position.line(), position.offset());
//                        this.data.add(length);
//                        this.data.add(type);
//                        this.data.add(modifiers);
                    }
                });
    }

    static class SemanticToken implements Comparable<SemanticToken> {

        private final int line;
        private final int startChar;
        private int length;
        private int type;
        private int modifiers;

        SemanticToken(int line, int startChar) {

            this.line = line;
            this.startChar = startChar;
//            this.length = length;
//            this.type = type;
//            this.modifiers = modifiers;
        }

        public SemanticToken(int line, int startChar, int length, int type, int modifiers) {

            this.line = line;
            this.startChar = startChar;
            this.length = length;
            this.type = type;
            this.modifiers = modifiers;
        }

        public int getLine() {

            return line;
        }

        public int getStartChar() {

            return startChar;
        }

        public int getLength() {

            return length;
        }

        public int getType() {

            return type;
        }

        public int getModifiers() {

            return modifiers;
        }

        public void setLength(int length) {

            this.length = length;
        }

        public void setType(int type) {

            this.type = type;
        }

        public void setModifiers(int modifiers) {

            this.modifiers = modifiers;
        }

        @Override
        public boolean equals(Object o) {

            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SemanticToken that = (SemanticToken) o;
            return line == that.line &&
                    startChar == that.startChar;
        }

        @Override
        public int hashCode() {

            return Objects.hash(line, startChar);
        }

        @Override
        public int compareTo(SemanticTokenVisitor.SemanticToken o) {
            if (this.line == o.line) {
                return this.startChar - o.startChar;
            }
            return this.line - o.line;
        }

        public static Comparator<SemanticToken> semanticTokenComparator = new Comparator<SemanticToken>() {

            public int compare(SemanticToken o1, SemanticToken o2) {

                //ascending order
                return o1.compareTo(o2);

                //descending order
                //return fruitName2.compareTo(fruitName1);
            }

        };
    }
}
