package org.ballerinalang.langserver.foldingrange;

import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
//import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.eclipse.lsp4j.FoldingRange;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FoldingRangeProvider {
    private static final String REGION = "region";
    public static List<FoldingRange> getFoldingRange(SyntaxTree syntaxTree) {


        List<FoldingRange> foldingRangeList = new ArrayList<>();
        ModulePartNode modulePartNode = syntaxTree.rootNode();
        FoldingRangeFinder foldingRangeFinder = new FoldingRangeFinder();

        Node rootNode = syntaxTree.rootNode();

//        int bucketCount = syntaxTree.rootNode().internalNode().bucketCount();

//        if (bucketCount > 0) {
//            for(int i =0 ; i< bucketCount ; i ++) {
////                syntaxTree.rootNode().internalNode().childInBucket(i).
//            }
//        }

//        NodeList<ModuleMemberDeclarationNode>  memberz = modulePartNode.members();

        List<ModuleMemberDeclarationNode> members = modulePartNode.members().stream().collect(Collectors.toList());
        for (ModuleMemberDeclarationNode member : members) {

            foldingRangeList.addAll(foldingRangeFinder.getFoldingRange(member));
//            if (member.kind() == SyntaxKind.SERVICE_DECLARATION) {
//
//            } else if (member.kind() == SyntaxKind.FUNCTION_DEFINITION) {
//
//            }
        }

        List<ImportDeclarationNode> imports =  modulePartNode.imports().stream().collect(Collectors.toList());
        int length = imports.size();
        if (length > 1) {
            ImportDeclarationNode firstImport = imports.get(0);
            ImportDeclarationNode lastImport = imports.get(length - 1);
            foldingRangeList.add(foldingRangeFinder.createFoldingRange(firstImport.lineRange().startLine().line(),
                    lastImport.lineRange().endLine().line(), firstImport.importKeyword().textRange().length(),
                    0, REGION));
        }
//        FoldingRange foldingRange1 = new FoldingRange(2, 3);
//        foldingRange1.setKind("comment");
//        foldingRange1.setStartCharacter(1);
//        foldingRange1.setEndCharacter(3);
//        foldingRangeList.add(foldingRange1);
//
//        FoldingRange foldingRange2 = new FoldingRange(4, 7);
//        foldingRange2.setKind("region");
//        foldingRange2.setStartCharacter(26);
//        foldingRange2.setEndCharacter(1);
//        foldingRangeList.add(foldingRange2);
//
//        FoldingRange foldingRange3 = new FoldingRange(11, 12);
//        foldingRange3.setKind("region");
//        foldingRange3.setStartCharacter(12);
//        foldingRange3.setEndCharacter(15);
//        foldingRangeList.add(foldingRange3);
//
//        FoldingRange foldingRange4 = new FoldingRange(9, 10);
//        foldingRange4.setKind("region");
//        foldingRange4.setStartCharacter(0);
//        foldingRange4.setEndCharacter(20);
//        foldingRangeList.add(foldingRange4);

        return foldingRangeList;

    }

}
