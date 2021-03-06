package org.neo4j.graphalgo.core.neo4jview;

import org.neo4j.collection.primitive.PrimitiveIntIterator;
import org.neo4j.collection.primitive.PrimitiveLongIterator;
import org.neo4j.cursor.Cursor;
import org.neo4j.graphalgo.api.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.api.ReadOperations;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.StatementConstants;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.impl.api.store.RelationshipIterator;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.storageengine.api.NodeItem;
import org.neo4j.storageengine.api.RelationshipItem;

import java.util.Iterator;
import java.util.function.*;

/**
 * A Graph implemented as View on Neo4j Kernel API
 *
 * @author mknobloch
 */
public class GraphView implements Graph {

    private final ThreadToStatementContextBridge contextBridge;
    private final GraphDatabaseAPI db;

    private final double propertyDefaultWeight;
    private int relationTypeId;
    private int nodeCount;
    private int propertyKey;
    private int labelId;

    public GraphView(GraphDatabaseAPI db, String label, String relation, String propertyName, double propertyDefaultWeight) {
        this.db = db;
        contextBridge = db.getDependencyResolver()
                .resolveDependency(ThreadToStatementContextBridge.class);
        this.propertyDefaultWeight = propertyDefaultWeight;

        withinTransaction(read -> {
            labelId = read.labelGetForName(label);
            nodeCount = Math.toIntExact(read.countsForNode(labelId));
            relationTypeId = read.relationshipTypeGetForName(relation);
            propertyKey = read.propertyKeyGetForName(propertyName);
        });
    }

    @Override
    public void forEachRelationship(int nodeId, Direction direction, RelationshipConsumer consumer) {
        final long originalNodeId = toOriginalNodeId(nodeId);
        withinTransaction(read -> {
            try (Cursor<NodeItem> nodeItemCursor = read.nodeCursor(originalNodeId)) {
                nodeItemCursor.forAll(nodeItem -> {
                    try (Cursor<RelationshipItem> relationships = nodeItem.relationships(mediate(direction), relationTypeId)) {
                        relationships.forAll(item -> {
                            consumer.accept(nodeId, toMappedNodeId(item.otherNode(originalNodeId)), item.id());
                        });
                    }
                });
            }
        });
    }

    @Override
    public void forEachRelationship(int nodeId, Direction direction, WeightedRelationshipConsumer consumer) {
        final long originalNodeId = toOriginalNodeId(nodeId);
        withinTransactionTyped(read -> {
            try {
                final RelationshipIterator iterator = read.nodeGetRelationships(originalNodeId, direction, relationTypeId);
                while (iterator.hasNext()) {
                    final long relationId = iterator.next();
                    final Cursor<RelationshipItem> relationshipItemCursor = read.relationshipCursor(relationId);
                    relationshipItemCursor.next();
                    final RelationshipItem item = relationshipItemCursor.get();
                    consumer.accept(
                            nodeId,
                            toMappedNodeId(item.otherNode(originalNodeId)),
                            relationId,
                            ((Number) read.relationshipGetProperty(relationId, propertyKey)).doubleValue()
                    );
                }
            } catch (EntityNotFoundException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    @Override
    public Iterator<WeightedRelationshipCursor> weightedRelationshipIterator(int nodeId, Direction direction) {
        try {
            return new WeightedRelationIteratorImpl(this, db, nodeId, direction, relationTypeId, propertyKey, propertyDefaultWeight);
        } catch (EntityNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int nodeCount() {
        return nodeCount;
    }

    @Override
    public void forEachNode(IntConsumer consumer) {
        withinTransaction(read -> {
            if (labelId == StatementConstants.NO_SUCH_LABEL) {
                try (Cursor<NodeItem> nodeItemCursor = read.nodeCursorGetAll()) {
                    while (nodeItemCursor.next()) {
                        consumer.accept(toMappedNodeId(nodeItemCursor.get().id()));
                    }
                }
            } else {
                try (Cursor<NodeItem> nodeItemCursor = read.nodeCursorGetForLabel(labelId)) {
                    while (nodeItemCursor.next()) {
                        consumer.accept(toMappedNodeId(nodeItemCursor.get().id()));
                    }
                }
            }
        });
    }

    @Override
    public PrimitiveIntIterator nodeIterator() {
        return withinTransactionTyped(read -> {
            if (labelId == StatementConstants.NO_SUCH_LABEL) {
                return new NodeIterator(this, read.nodesGetAll());
            }
            return new NodeIterator(this, read.nodesGetForLabel(labelId));
        });
    }

    @Override
    public int degree(int nodeId, Direction direction) {
        return withinTransactionInt(read -> {
            try {
                return read.nodeGetDegree(toOriginalNodeId(nodeId), direction, relationTypeId);
            } catch (EntityNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Iterator<RelationshipCursor> relationshipIterator(int nodeId, Direction direction) {
        try {
            return new RelationIteratorImpl(this, db, nodeId, direction, relationTypeId);
        } catch (EntityNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int toMappedNodeId(long nodeId) {
        return Math.toIntExact(nodeId);
    }

    @Override
    public long toOriginalNodeId(int nodeId) {
        return nodeId;
    }

    private int withinTransactionInt(ToIntFunction<ReadOperations> block) {
        try (final Transaction tx = db.beginTx();
             Statement statement = contextBridge.get()) {
            final int result = block.applyAsInt(statement.readOperations());
            tx.success();
            return result;
        }
    }

    private <T> T withinTransactionTyped(Function<ReadOperations, T> block) {
        try (final Transaction tx = db.beginTx();
             Statement statement = contextBridge.get()) {
            final T result = block.apply(statement.readOperations());
            tx.success();
            return result;
        }
    }

    private void withinTransaction(Consumer<ReadOperations> block) {
        try (final Transaction tx = db.beginTx();
             Statement statement = contextBridge.get()) {
            block.accept(statement.readOperations());
            tx.success();
        }
    }

    private static org.neo4j.storageengine.api.Direction mediate(Direction direction) {
        switch (direction) {
            case INCOMING:
                return org.neo4j.storageengine.api.Direction.INCOMING;
            case OUTGOING:
                return org.neo4j.storageengine.api.Direction.OUTGOING;
            case BOTH:
                return org.neo4j.storageengine.api.Direction.BOTH;
        }
        throw new IllegalArgumentException("Direction " + direction + " is unknown");
    }


    private static class NodeIterator implements PrimitiveIntIterator {

        private final Graph graph;

        private final PrimitiveLongIterator iterator;

        private NodeIterator(Graph graph, PrimitiveLongIterator iterator) {
            this.graph = graph;
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public int next() {
            return graph.toMappedNodeId(iterator.next());
        }
    }
}
