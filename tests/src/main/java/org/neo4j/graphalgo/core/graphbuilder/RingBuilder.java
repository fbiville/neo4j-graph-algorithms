package org.neo4j.graphalgo.core.graphbuilder;

import org.neo4j.graphdb.Node;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import java.util.HashSet;
import java.util.function.Consumer;

/**
 * @author mknblch
 */
public class RingBuilder extends GraphBuilder<RingBuilder> {

    private final HashSet<Node> nodes;

    protected RingBuilder(GraphDatabaseAPI api) {
        super(api);
        nodes = new HashSet<>();
    }

    public RingBuilder createRing(int size) {
        if (size < 2) {
            throw new IllegalArgumentException("size must be >= 2");
        }
        withinTransaction(() -> {
            final Node head = createNode();
            Node temp = head;
            for (int i = 0; i < size; i++) {
                Node node = createNode();
                nodes.add(node);
                createRelationship(temp, node);
                temp = node;
            }
            createRelationship(temp, head);
        });
        return this;
    }

    public HashSet<Node> getNodes() {
        return nodes;
    }

    public void forEachNode(Consumer<Node> consumer) {
        withinTransaction(() -> nodes.forEach(consumer));
    }

    @Override
    protected RingBuilder me() {
        return this;
    }
}
