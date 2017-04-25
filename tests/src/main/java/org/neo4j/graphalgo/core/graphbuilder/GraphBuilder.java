package org.neo4j.graphalgo.core.graphbuilder;

import com.carrotsearch.hppc.IntScatterSet;
import com.carrotsearch.hppc.IntSet;
import org.neo4j.graphalgo.api.IdMapping;
import org.neo4j.graphalgo.core.sources.LazyIdMapper;
import org.neo4j.graphdb.*;
import org.neo4j.kernel.api.DataWriteOperations;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.InvalidTransactionTypeKernelException;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import java.util.function.Consumer;

/**
 * The GraphBuilder intends to ease the creation
 * of test graphs with well known properties
 *
 * @author mknblch
 */
public abstract class GraphBuilder<ME extends GraphBuilder<ME>> {

    protected final GraphDatabaseAPI api;
    protected final IntSet nodeSet;
    protected final ThreadToStatementContextBridge bridge;

    protected String label;
    protected String relationship;

    protected GraphBuilder(GraphDatabaseAPI api) {
        this.api = api;
        bridge = api.getDependencyResolver().resolveDependency(ThreadToStatementContextBridge.class);
        nodeSet = new IntScatterSet();
    }

    public IntSet getNodeSet() {
        return nodeSet;
    }

    public ME setLabel(String label) {
        this.label = label;
        return me();
    }

    public ME setRelationship(String relationship) {
        this.relationship = relationship;
        return me();
    }

    protected Node createNode(String label, String property, Object value) {
        Node node = api.createNode();
        if (null != label) {
            node.addLabel(Label.label(label));
        }
        return node;
    }

    protected Relationship createRelationship(Node p, Node q) {
        return p.createRelationshipTo(q, RelationshipType.withName(relationship));
    }

    protected Node createNode() {
        Node node = api.createNode();
        if (null != label) {
            node.addLabel(Label.label(label));
        }
        return node;
    }

    protected void writeInTransaction(Consumer<DataWriteOperations> consumer) {
        try(Transaction tx = api.beginTx();
            Statement statement = bridge.get()) {
            consumer.accept(statement.dataWriteOperations());
            tx.success();
        } catch (InvalidTransactionTypeKernelException e) {
            throw new RuntimeException(e);
        }
    }

    protected void withinTransaction(Runnable runnable) {
        try(Transaction tx = api.beginTx()) {
            runnable.run();
            tx.success();
        }
    }

    protected abstract ME me();
}
