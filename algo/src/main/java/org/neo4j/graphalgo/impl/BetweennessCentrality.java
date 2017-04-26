package org.neo4j.graphalgo.impl;

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntStack;
import org.neo4j.graphalgo.api.Graph;
import org.neo4j.graphalgo.core.utils.container.Path;
import org.neo4j.graphdb.Direction;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implements Betweenness Centrality for unweighted graphs
 * as specified in <a href=http://www.algo.uni-konstanz.de/publications/b-fabc-01.pdf">this paper</a>
 *
 * @author mknblch
 */
public class BetweennessCentrality {

    private final Graph graph;

    private final double[] centrality;
    private final IntStack stack;
    private final int[] sigma;
    private final double[] delta;
    private final int[] d;
    private final IntArrayDeque queue;
    private final Path[] paths;

    public BetweennessCentrality(Graph graph) {
        this.graph = graph;
        this.centrality = new double[graph.nodeCount()];
        this.stack = new IntStack();
        this.sigma = new int[graph.nodeCount()];
        this.d = new int[graph.nodeCount()];
        queue = new IntArrayDeque();
        paths = new Path[graph.nodeCount()];
        delta = new double[graph.nodeCount()];
    }

    public BetweennessCentrality compute() {
        graph.forEachNode(this::compute);
        return this;
    }

    /**
     * iterate over each result until every node has visited or
     * the consumer returns false;
     *
     * @param consumer the result consumer
     */
    public void forEach(ResultConsumer consumer) {
        for (int i = graph.nodeCount() - 1; i >= 0; i--) {
            if (!consumer.consume(graph.toOriginalNodeId(i), centrality[i])) {
                return;
            }
        }
    }

    public Stream<Result> resultStream() {
        return IntStream.range(0, graph.nodeCount())
                .mapToObj(nodeId ->
                        new Result(
                                graph.toOriginalNodeId(nodeId),
                                centrality[nodeId]));
    }

    private void compute(int node) {
        clearPaths();
        stack.clear();
        queue.clear();
        Arrays.fill(sigma, 0);
        Arrays.fill(delta, 0);
        Arrays.fill(d, -1);
        sigma[node] = 1;
        d[node] = 0;
        queue.addLast(node);
        while (!queue.isEmpty()) {
            int v = queue.removeLast();
            stack.push(v);
            graph.forEachRelationship(v, Direction.OUTGOING, (source, target, relationId) -> {
                if (d[target] < 0) {
                    queue.addLast(target);
                    d[target] = d[v] + 1;
                } else if (d[target] == d[v] + 1) {
                    sigma[target] += sigma[v];
                    append(target, v);
                }
                return true;
            });
        }
        while (!stack.isEmpty()) {
            final int w = stack.pop();
            paths[w].forEch(v -> {
                delta[v] += (double) sigma[v] / ((double) sigma[w]) * (delta[w] + 1.0);
                if (w != node) {
                    centrality[w] += delta[w];
                }
                return true;
            });
        }
    }

    /**
     * append nodeId to path
     *
     * @param path the selected path
     * @param nodeId the node id
     */
    private void append(int path, int nodeId) {
        if (null == paths[path]) {
            paths[path] = new Path();
        }
        paths[path].append(nodeId);

    }

    private void clearPaths() {
        for (Path path : paths) {
            if (null == path) {
                continue;
            }
            path.clear();
        }
    }

    public interface ResultConsumer {
        boolean consume(long originalNodeId, double value);
    }

    public static final class Result {

        public final Long nodeId;

        public final Double value;

        public Result(Long nodeId, Double value) {
            this.nodeId = nodeId;
            this.value = value;
        }
    }
}
