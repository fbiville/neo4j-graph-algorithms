package org.neo4j.graphalgo.api;

import java.util.concurrent.ExecutorService;

/**
 * DTO to ease the use of the GraphFactory-CTor. Should contain
 * setup options for loading the graph from neo4j.
 *
 * @author mknblch
 */
public class GraphSetup {

    // start label type. null means any label.
    public final String startLabel;
    // end label type (not yet implemented).
    public final String endLabel;
    // relationtype name. null means any relation.
    public final String relationshipType;
    // property of relationship weights. null means NO property (the default value will be used instead).
    public final String relationWeightPropertyName;
    // default property is used for weighted relationships if property is not set.
    public final double relationDefaultWeight;

    // the executor service for parallel execution. null means single threaded evaluation.
    @Deprecated
    public final ExecutorService executor;

    /**
     * main ctor
     * @param startLabel the start label. null means any label.
     * @param endLabel not implemented yet
     * @param relationshipType the relation type identifier. null for any relationship
     * @param relationWeightPropertyName property name which holds the weights / costs of a relation.
     *                                   null means the default value is used for each weight.
     * @param relationDefaultWeight the default relationship weight if property is not given.
     * @param executor the executor. null means single threaded evaluation
     */
    public GraphSetup(
            String startLabel,
            String endLabel,
            String relationshipType,
            String relationWeightPropertyName,
            double relationDefaultWeight,
            ExecutorService executor) {

        this.startLabel = startLabel;
        this.endLabel = endLabel;
        this.relationshipType = relationshipType;
        this.relationWeightPropertyName = relationWeightPropertyName;
        this.relationDefaultWeight = relationDefaultWeight;
        this.executor = executor;
    }

    /**
     * Setup Graph to load any label, any relationship, no property in single threaded mode
     */
    public GraphSetup() {
        this.startLabel = null;
        this.endLabel = null;
        this.relationshipType = null;
        this.relationWeightPropertyName = null;
        this.relationDefaultWeight = 1.0;
        this.executor = null;
    }

    /**
     * Setup graph to load any label, any relationship, no property but
     * in multithreaded mode (depends on the actual executor)
     *
     * @param executor executor service
     */
    public GraphSetup(ExecutorService executor) {
        this.startLabel = null;
        this.endLabel = null;
        this.relationshipType = null;
        this.relationWeightPropertyName = null;
        this.relationDefaultWeight = 1.0;
        this.executor = executor;
    }

    public boolean loadConcurrent() {
        return executor != null;
    }

    public boolean loadDefaultRelationshipWeight() {
        return relationWeightPropertyName == null;
    }

    public boolean loadAnyLabel() {
        return startLabel == null;
    }

    public boolean loadAnyRelationshipType() {
        return relationshipType == null;
    }
}
