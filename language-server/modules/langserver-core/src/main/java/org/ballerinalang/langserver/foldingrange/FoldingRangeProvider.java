package org.ballerinalang.langserver.foldingrange;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.eclipse.lsp4j.FoldingRange;

import java.util.ArrayList;
import java.util.List;

public class FoldingRangeProvider {

    public static List<FoldingRange> getFoldingRange(SyntaxTree syntaxTree) {

        List<FoldingRange> foldingRangeList = new ArrayList<>();
        Node rootNode = syntaxTree.rootNode();
        FoldingRange foldingRange1 = new FoldingRange(2, 3);
        foldingRange1.setKind("comment");
        foldingRange1.setStartCharacter(1);
        foldingRange1.setEndCharacter(3);
        foldingRangeList.add(foldingRange1);

        FoldingRange foldingRange2 = new FoldingRange(4, 7);
        foldingRange2.setKind("region");
        foldingRange2.setStartCharacter(26);
        foldingRange2.setEndCharacter(1);
        foldingRangeList.add(foldingRange2);

        FoldingRange foldingRange3 = new FoldingRange(11, 12);
        foldingRange3.setKind("region");
        foldingRange3.setStartCharacter(12);
        foldingRange3.setEndCharacter(15);
        foldingRangeList.add(foldingRange3);

        FoldingRange foldingRange4 = new FoldingRange(9, 10);
        foldingRange4.setKind("region");
        foldingRange4.setStartCharacter(0);
        foldingRange4.setEndCharacter(20);
        foldingRangeList.add(foldingRange4);

        return foldingRangeList;

    }

}
