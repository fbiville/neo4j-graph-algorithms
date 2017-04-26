package org.neo4j.graphalgo.core.utils.container;

import org.apache.lucene.util.ArrayUtil;

import java.util.Arrays;
import java.util.function.IntPredicate;

/**
 * @author mknblch
 */
public class Path {

    private int[] nodes;
    private int offset = 0;

    public Path() {
        this(1); // TODO test other initial sizes
    }

    public Path(int initialSize) {
        nodes = new int[initialSize];
    }

    public void append(int nodeId) {
        if (offset >= nodes.length) {
            nodes = ArrayUtil.grow(nodes, offset);
        }
        nodes[offset++] = nodeId;
    }

    public int size() {
        return offset;
    }

    public void forEch(IntPredicate consumer) {
        for (int i = 0; i < offset; i++) {
            if (!consumer.test(nodes[i])) {
                return;
            }
        }
    }

    public void clear() {
        offset = 0;
    }
}
