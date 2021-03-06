package org.neo4j.graphalgo.core;

import com.carrotsearch.hppc.LongIntHashMap;
import com.carrotsearch.hppc.LongIntMap;
import com.carrotsearch.hppc.cursors.LongIntCursor;
import org.neo4j.collection.primitive.PrimitiveIntIterator;
import org.neo4j.graphalgo.api.IdMapping;
import org.neo4j.graphalgo.api.NodeIterator;

import java.util.function.IntConsumer;

/**
 * This is basically a long to int mapper. It sorts the id's in ascending order so its
 * guaranteed that there is no ID greater then nextGraphId / capacity
 */
public final class IdMap implements IdMapping, NodeIterator{

    private final IdIterator iter;
    private int nextGraphId;
    private long[] graphIds;
    private LongIntMap nodeToGraphIds;

    /**
     * initialize the map with maximum node capacity
     */
    public IdMap(final int capacity) {
        nodeToGraphIds = new LongIntHashMap((int) Math.ceil(capacity / 0.99), 0.99);
        iter = new IdIterator();
    }

    /**
     * CTor used by deserializing logic
     */
    public IdMap(
            long[] graphIds,
            LongIntMap nodeToGraphIds) {
        this.nextGraphId = graphIds.length;
        this.graphIds = graphIds;
        this.nodeToGraphIds = nodeToGraphIds;
        iter = new IdIterator();
    }

    public PrimitiveIntIterator iterator() {
        return iter.reset(nextGraphId);
    }

    public int mapOrGet(long longValue) {
        int intValue = nodeToGraphIds.getOrDefault(longValue, -1);
        if (intValue == -1) {
            intValue = nextGraphId++;
            nodeToGraphIds.put(longValue, intValue);
        }
        return intValue;
    }

    public void add(long longValue) {
        int intValue = nextGraphId++;
        nodeToGraphIds.put(longValue, intValue);
    }

    public int get(long longValue) {
        return nodeToGraphIds.getOrDefault(longValue, -1);
    }

    public long unmap(int intValue) {
        return graphIds[intValue];
    }

    public void buildMappedIds() {
        graphIds = new long[nodeToGraphIds.size()];
        for (final LongIntCursor cursor : nodeToGraphIds) {
            graphIds[cursor.value] = cursor.key;
        }
    }

    public int size() {
        return nextGraphId;
    }

    public long[] mappedIds() {
        return graphIds;
    }

    public void forEach(IntConsumer consumer) {
        int limit = this.nextGraphId;
        for (int i = 0; i < limit; i++) {
            consumer.accept(i);
        }
    }

    @Override
    public int toMappedNodeId(long nodeId) {
        return mapOrGet(nodeId);
    }

    @Override
    public long toOriginalNodeId(int nodeId) {
        return graphIds[nodeId];
    }

    @Override
    public int nodeCount() {
        return graphIds.length;
    }

    @Override
    public void forEachNode(IntConsumer consumer) {
        final int count = nodeCount();
        for (int i = 0; i < count; i++) {
            consumer.accept(i);
        }
    }

    @Override
    public PrimitiveIntIterator nodeIterator() {
        return new IdIterator().reset(nodeCount());
    }

    private static final class IdIterator implements PrimitiveIntIterator {

        private int current;
        private int limit;

        private PrimitiveIntIterator reset(int limit) {
            current = 0;
            this.limit = limit;
            return this;
        }

        @Override
        public boolean hasNext() {
            return current < limit;
        }

        @Override
        public int next() {
            return current++;
        }
    }
}
