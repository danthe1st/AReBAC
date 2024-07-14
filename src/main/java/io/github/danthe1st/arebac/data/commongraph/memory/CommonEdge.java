package io.github.danthe1st.arebac.data.commongraph.memory;

/**
 * An edge in a (directed) graph.
 *
 * Edges have a source and target node as well as an id and an edge type.
 * The id should be unique in the graph.
 * @param <N> The type of the nodes.
 */
public interface CommonEdge<N extends CommonNode> {
	N source();
	N target();
	
	String id();
	String edgeType();
}
