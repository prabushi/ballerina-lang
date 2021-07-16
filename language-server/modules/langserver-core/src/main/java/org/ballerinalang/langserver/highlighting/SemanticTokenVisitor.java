package org.ballerinalang.langserver.highlighting;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.DistinctTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.EnumDeclarationNode;
import io.ballerina.compiler.syntax.tree.EnumMemberNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ImportPrefixNode;
import io.ballerina.compiler.syntax.tree.IntersectionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.KeySpecifierNode;
import io.ballerina.compiler.syntax.tree.MapTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.MarkdownParameterDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.NilTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.ObjectTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ParenthesisedTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SingletonTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.StreamTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TableTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TupleTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;
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
        this.cache.forEach(semanticToken -> {
            calculateRelativeTokenValues(semanticToken.getLine(), semanticToken.getStartChar());
            this.data.add(semanticToken.getLength());
            this.data.add(semanticToken.getType());
            this.data.add(semanticToken.getModifiers());
        });
    }

    public void visit(ImportDeclarationNode importDeclarationNode) {
        Optional<ImportPrefixNode> importPrefixNode = importDeclarationNode.prefix();
        if (importPrefixNode.isPresent()) {
           Token token =  importPrefixNode.get().prefix();
            LinePosition startLine = token.lineRange().startLine();
            SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
            if (!cache.contains(semanticToken)) {
                int length = token.text().length();
                semanticToken.setLength(length);
                semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Namespace));
                semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
                cache.add(semanticToken);
                handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.Namespace), 0);
            }
        }
        visitSyntaxNode(importDeclarationNode);
    }

    public void visit(FunctionDefinitionNode functionDefinitionNode) {

        LinePosition startLine = functionDefinitionNode.functionName().lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
            int length = functionDefinitionNode.functionName().text().length();
            semanticToken.setLength(length);
            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Function));
            semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
            cache.add(semanticToken);
            if (functionDefinitionNode.kind() != SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.Function), 0);
            }
            if (functionDefinitionNode.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                NodeList<Node> resourcePaths =  functionDefinitionNode.relativeResourcePath();
                resourcePaths.forEach(resourcePath -> {
                    SemanticToken resourcePathToken = new SemanticToken(resourcePath.lineRange().startLine().line(),
                            resourcePath.lineRange().startLine().offset(), resourcePath.toString().trim().length(),
                            TOKEN_TYPES.indexOf(SemanticTokenTypes.Function),
                            1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
                    cache.add(resourcePathToken);
                });
            }
        }
        visitSyntaxNode(functionDefinitionNode);
    }

//    public void visit(ExpressionStatementNode expressionStatementNode) {
//
//        visitSyntaxNode(expressionStatementNode);
//    }

//    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
//
//        LinePosition startLine = functionCallExpressionNode.functionName().lineRange().startLine();
//        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
//        if (!cache.contains(semanticToken)) {
//            semanticToken.setLength(functionCallExpressionNode.functionName().toString().trim().length());
//            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Function));
//            semanticToken.setModifiers(0);
//            cache.add(semanticToken);
//        }
//
//        visitSyntaxNode(functionCallExpressionNode);
//    }

    public void visit(RequiredParameterNode requiredParameterNode) {

        if (requiredParameterNode.paramName().isPresent()) {
            Token token = requiredParameterNode.paramName().get();
            LinePosition startLine = token.lineRange().startLine();
            SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
            if (!cache.contains(semanticToken)) {
                int length = token.toString().trim().length();
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
        processWithSymbolsAPI(simpleNameReferenceNode, startLine);
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

    public void visit(KeySpecifierNode keySpecifierNode) {
        visitSyntaxNode(keySpecifierNode);
    }

    public void visit(ObjectTypeDescriptorNode objectTypeDescriptorNode) {
        visitSyntaxNode(objectTypeDescriptorNode);
    }

    public void visit(RecordTypeDescriptorNode recordTypeDescriptorNode) {
        visitSyntaxNode(recordTypeDescriptorNode);
    }

//    public void visit(ReturnTypeDescriptorNode returnTypeDescriptorNode) {
//        visitSyntaxNode(returnTypeDescriptorNode);
//    }

    public void visit(NilTypeDescriptorNode nilTypeDescriptorNode) {
        visitSyntaxNode(nilTypeDescriptorNode);
    }

    public void visit(OptionalTypeDescriptorNode optionalTypeDescriptorNode) {
        visitSyntaxNode(optionalTypeDescriptorNode);
    }

    public void visit(MapTypeDescriptorNode mapTypeDescriptorNode) {
        visitSyntaxNode(mapTypeDescriptorNode);
    }

    public void visit(UnionTypeDescriptorNode unionTypeDescriptorNode) {
        visitSyntaxNode(unionTypeDescriptorNode);
    }

    public void visit(StreamTypeDescriptorNode streamTypeDescriptorNode) {
        visitSyntaxNode(streamTypeDescriptorNode);
    }

    public void visit(TableTypeDescriptorNode tableTypeDescriptorNode) {
//        tableTypeDescriptorNode.keyConstraintNode().get().
//        ((KeySpecifierNode)tableTypeDescriptorNode.keyConstraintNode().get()).toString() // key(age)
        visitSyntaxNode(tableTypeDescriptorNode);
    }

    public void visit(FunctionTypeDescriptorNode functionTypeDescriptorNode) {
        visitSyntaxNode(functionTypeDescriptorNode);
    }

    public void visit(TupleTypeDescriptorNode tupleTypeDescriptorNode) {
        visitSyntaxNode(tupleTypeDescriptorNode);
    }

    public void visit(ParenthesisedTypeDescriptorNode parenthesisedTypeDescriptorNode) {
        visitSyntaxNode(parenthesisedTypeDescriptorNode);
    }

    public void visit(IntersectionTypeDescriptorNode intersectionTypeDescriptorNode) {
        visitSyntaxNode(intersectionTypeDescriptorNode);
    }

    public void visit(SingletonTypeDescriptorNode singletonTypeDescriptorNode) {
        visitSyntaxNode(singletonTypeDescriptorNode);
    }

    public void visit(ArrayTypeDescriptorNode arrayTypeDescriptorNode) {
        visitSyntaxNode(arrayTypeDescriptorNode);
    }

    public void visit(DistinctTypeDescriptorNode distinctTypeDescriptorNode) {
        visitSyntaxNode(distinctTypeDescriptorNode);
    }

//    public void visit(ParameterizedTypeDescriptorNode parameterizedTypeDescriptorNode) {
//        visitSyntaxNode(parameterizedTypeDescriptorNode);
//    }

//    public void visit(TypedBindingPatternNode typedBindingPatternNode) {
//        visitSyntaxNode(typedBindingPatternNode);
//    }

    public void visit(QualifiedNameReferenceNode qualifiedNameReferenceNode) {
        Token token = qualifiedNameReferenceNode.modulePrefix();
        LinePosition linePosition = token.lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(linePosition.line(), linePosition.offset());
        if (!cache.contains(semanticToken)) {
            int length = token.text().length();
            semanticToken.setLength(length);
            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Namespace));
            semanticToken.setModifiers(0);
            this.cache.add(semanticToken);
        }
        Token identifier = qualifiedNameReferenceNode.identifier();
        LinePosition identifierPosition = identifier.lineRange().startLine();
        SemanticToken identifierSemanticToken = new SemanticToken(identifierPosition.line(),
                identifierPosition.offset());
        if (!cache.contains(identifierSemanticToken)) {
            processWithSymbolsAPI(identifier, identifierPosition);
        }
        visitSyntaxNode(qualifiedNameReferenceNode);
    }

    public void visit(ConstantDeclarationNode constantDeclarationNode) {

        Token token = constantDeclarationNode.variableName();
        LinePosition startLine = token.lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
            int length = token.toString().trim().length();
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
            int length = token.text().trim().length();
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
            SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset(),
                    serviceName.toString().trim().length(), TOKEN_TYPES.indexOf(SemanticTokenTypes.Type),
                    1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
            cache.add(semanticToken);
        });
        visitSyntaxNode(serviceDeclarationNode);
    }

    public void visit(EnumDeclarationNode enumDeclarationNode) {

        Node token = enumDeclarationNode.identifier();
        LinePosition startLine = token.lineRange().startLine();
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
            int length = token.text().trim().length();
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
            int length = token.toString().trim().length();
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
            int length = token.toString().length();
            semanticToken.setLength(length);
            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.TypeParameter));
            if (recordFieldNode.readonlyKeyword().isPresent()) {
                semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration)
                        | 1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
                cache.add(semanticToken);
                handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.TypeParameter),
                        1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
            } else {
                semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
                cache.add(semanticToken);
                handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.TypeParameter), 0);
            }
        }
        visitSyntaxNode(recordFieldNode);
    }

    public void visit(AnnotationNode annotationNode) {
        visitSyntaxNode(annotationNode);
    }

    public void visit(ObjectFieldNode objectFieldNode) {
        //public int age;
        Token token = objectFieldNode.fieldName();
        LinePosition startLine = token.lineRange().startLine();
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
            int length = token.toString().trim().length();
            semanticToken.setLength(length);
            semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Property));
            semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Declaration));
            cache.add(semanticToken);
            handleReferences(startLine, length, TOKEN_TYPES.indexOf(SemanticTokenTypes.Property), 0);
        }
        visitSyntaxNode(objectFieldNode);
    }

    private void processWithSymbolsAPI(Node node, LinePosition startLine) {
        SemanticToken semanticToken = new SemanticToken(startLine.line(), startLine.offset());
        if (!cache.contains(semanticToken)) {
            Optional<Symbol> symbol = this.semanticModel.symbol(node);
            if (symbol.isPresent()) {
                SymbolKind kind = symbol.get().kind();
                String nodeName = node.toString().trim();
                semanticToken.setLength(nodeName.length());
                if (kind == SymbolKind.CLASS) {
                    if (nodeName.equals("self")) {
                        semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Keyword));
                    } else {
                        semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Class));
                    }
                    semanticToken.setModifiers(0);
                } else if (kind == SymbolKind.CLASS_FIELD) {
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Property));
                    semanticToken.setModifiers(0);
                } else if (kind == SymbolKind.CONSTANT) {
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Variable));
                    semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
                } else if (kind == SymbolKind.VARIABLE) {
                    if (nodeName.equals("self")) {
                        semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Keyword));
                        semanticToken.setModifiers(0);
                    } else {
                        semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Variable));
                        semanticToken.setModifiers(0);
                    }
                } else if (kind == SymbolKind.TYPE || kind == SymbolKind.RECORD_FIELD) {
//                        || symbol.get() instanceof TypeReferenceTypeSymbol
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Type));
                    semanticToken.setModifiers(0);
                } else if (kind == SymbolKind.ENUM_MEMBER) {
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.EnumMember));
                    semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Readonly));
                } else if (kind == SymbolKind.PARAMETER) {
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Parameter));
                    semanticToken.setModifiers(0);
                } else if (kind == SymbolKind.FUNCTION) {
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Function));
                    semanticToken.setModifiers(0);
                } else if (kind == SymbolKind.ANNOTATION) {
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Keyword));
//                    if (nodeName.equals("deprecated")) {
//                        semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.Deprecated));
//                    } else {
                        semanticToken.setModifiers(0);
//                    }
                } else if (kind == SymbolKind.METHOD) {
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Method));
                    semanticToken.setModifiers(0);
                } else {
                    semanticToken.setType(TOKEN_TYPES.indexOf(SemanticTokenTypes.Type)); // TODO
                    semanticToken.setModifiers(1 << TOKEN_MODIFIERS.indexOf(SemanticTokenModifiers.DefaultLibrary));
                }
                cache.add(semanticToken);
            }
        }
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
