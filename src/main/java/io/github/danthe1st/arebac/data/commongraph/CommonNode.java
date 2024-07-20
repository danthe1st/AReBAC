package io.github.danthe1st.arebac.data.commongraph;

/**
 * Interface for nodes in a graph.
 *
 * Nodes have an ID and a type (label).
 * The ID should be unique in the graph while there can be multiple nodes of the same type.
 */
public interface CommonNode {
	String id();
	String nodeType();
}
