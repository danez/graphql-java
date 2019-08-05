package graphql.util;

import graphql.PublicApi;

import java.util.List;
import java.util.Queue;

import static graphql.Assert.assertTrue;

@PublicApi
public class TreeTransformerUtil {

    /**
     * Can be called multiple times to change the current node of the context. The latest call wins
     *
     * @param context
     * @param changedNode
     * @param <T>
     *
     * @return
     */
    public static <T> TraversalControl changeNode(TraverserContext<T> context, T changedNode) {
        NodeZipper<T> zipperWithChangedNode = context.getVar(NodeZipper.class).withNewNode(changedNode);
        boolean changed = context.isChanged();
        if (context.isParallel()) {
            Queue<NodeZipper<T>> zippers = context.getSharedContextData();
            if (changed) {
                throw new RuntimeException("multiple changes per node is not supported for parallel traversing");
            } else {
                zippers.add(zipperWithChangedNode);
                context.changeNode(changedNode);
            }
            return TraversalControl.CONTINUE;
        } else {
            List<NodeZipper<T>> zippers = context.getSharedContextData();
            if (changed) {
                // this is potentially expensive
                replaceZipperForNode(zippers, context.thisNode(), changedNode);
                context.changeNode(changedNode);
            } else {
                zippers.add(zipperWithChangedNode);
                context.changeNode(changedNode);
            }
            return TraversalControl.CONTINUE;
        }
    }

    private static <T> void replaceZipperForNode(List<NodeZipper<T>> zippers, T currentNode, T newNode) {
        int index = FpKit.findIndex(zippers, zipper -> zipper.getCurNode() == currentNode);
        assertTrue(index >= 0, "No current zipper found for provided node");
        NodeZipper<T> newZipper = zippers.get(index).withNewNode(newNode);
        zippers.set(index, newZipper);
    }

    public static <T> TraversalControl deleteNode(TraverserContext<T> context) {
        NodeZipper<T> deleteNodeZipper = context.getVar(NodeZipper.class).deleteNode();
        Queue<NodeZipper<T>> zippers = context.getSharedContextData();
        zippers.add(deleteNodeZipper);
        context.deleteNode();
        return TraversalControl.CONTINUE;
    }

    public static <T> TraversalControl insertAfter(TraverserContext<T> context, T toInsertAfter) {
        NodeZipper<T> insertNodeZipper = context.getVar(NodeZipper.class).insertAfter(toInsertAfter);
        Queue<NodeZipper<T>> zippers = context.getSharedContextData();
        zippers.add(insertNodeZipper);
        return TraversalControl.CONTINUE;
    }

    public static <T> TraversalControl insertBefore(TraverserContext<T> context, T toInsertBefore) {
        NodeZipper<T> insertNodeZipper = context.getVar(NodeZipper.class).insertBefore(toInsertBefore);
        Queue<NodeZipper<T>> zippers = context.getSharedContextData();
        zippers.add(insertNodeZipper);
        return TraversalControl.CONTINUE;
    }


}
