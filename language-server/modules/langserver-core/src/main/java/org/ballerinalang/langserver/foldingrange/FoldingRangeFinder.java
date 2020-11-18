package org.ballerinalang.langserver.foldingrange;

import io.ballerina.compiler.syntax.tree.ExternalFunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.eclipse.lsp4j.FoldingRange;

//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FoldingRangeFinder extends NodeVisitor {

//    private static final Map<Class<?>, Method> SCOPED_NODE_TO_VISIT_METHOD = Arrays.stream(
//            FoldingRangeFinder.class.getDeclaredMethods())
//            .filter(s -> "visit".equals(s.getName()) && s.getParameterTypes().length > 0)
//            .collect(Collectors.toMap(k -> k.getParameterTypes()[0], v -> v));
    private List<FoldingRange> foldingRanges;
    private static final  String COMMENT = "comment";
    private static final String REGION = "region";
    private int startLine;
    private int startCharacter;

    FoldingRangeFinder() {
        foldingRanges = new ArrayList<>();
    }

//    public void visit(FunctionDefinitionNode functionDefinitionNode) {
//        FunctionBodyNode functionBodyNode = functionDefinitionNode.functionBody();
//        visitSyntaxNode(functionDefinitionNode);
//    }

    public void visit(FunctionBodyBlockNode functionBodyBlockNode) {
        visitSyntaxNode(functionBodyBlockNode);
        foldingRanges.add(createFoldingRange(functionBodyBlockNode.lineRange().startLine().line(),
                functionBodyBlockNode.lineRange().endLine().line(),
                functionBodyBlockNode.lineRange().startLine().offset(),
                1, COMMENT));
    }

//    public void visit(FunctionBodyNode functionBodyNode) {
//        foldingRanges.add(createFoldingRange(functionBodyNode.lineRange().startLine().line(),
//                functionBodyNode.lineRange().endLine().line(), functionBodyNode.lineRange().startLine().offset(),
//                1, COMMENT));
//        visitSyntaxNode(functionBodyNode);
//    }

    public void visit(ExternalFunctionBodyNode externalFunctionBodyNode) {

        System.out.println("ExternalFunctionBodyNode");
        foldingRanges.add(createFoldingRange(6, 8, 3, 5, REGION));
        visitSyntaxNode(externalFunctionBodyNode);
    }

    public void visit(MetadataNode metadataNode) {

        System.out.println("MetadataNode");
        foldingRanges.add(createFoldingRange(metadataNode.lineRange().startLine().line(),
                metadataNode.lineRange().endLine().line(), -1,
                metadataNode.lineRange().endLine().offset(), REGION));
//        visitSyntaxNode(metadataNode);
    }

//    public void visit(Node node) {
//        if (node == null) {
//            return;
//        }
//
//        // If it is a supported scope node, visit it
//        visitScopedNodeMethod(node);
//
//        // Visit from bottom-up in tree until we find a supported node or loose the range
//        boolean isRangeWithinNode = CommonUtil.isWithinLineRange(this.range.getStart(), node.lineRange()) &&
//                CommonUtil.isWithinLineRange(this.range.getEnd(), node.lineRange());
//        if (!isRangeWithinNode) {
//            visit(node.parent());
//        }
//    }

//    private void visitScopedNodeMethod(Node node) {
//        Method visitMethod = SCOPED_NODE_TO_VISIT_METHOD.get(node.getClass());
//        if (visitMethod != null) {
//            try {
//                visitMethod.invoke(this, node);
//            } catch (IllegalAccessException | InvocationTargetException e) {
//                // ignore;
//            }
//        }
//    }

    public List<FoldingRange> getFoldingRange(Node node) {
        visitSyntaxNode(node);
        return this.foldingRanges;
    }

    public FoldingRange createFoldingRange(int startLine, int endLine, int startCharacter, int endCharacter, String kind) {
        FoldingRange foldingRange =  new FoldingRange(startLine, endLine);
        if (startCharacter > -1) {
            foldingRange.setStartCharacter(startCharacter);
        }
        foldingRange.setEndCharacter(endCharacter);
        foldingRange.setKind(kind);
        return foldingRange;
    }
}
