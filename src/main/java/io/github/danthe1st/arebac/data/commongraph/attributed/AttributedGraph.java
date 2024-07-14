package io.github.danthe1st.arebac.data.commongraph.attributed;

import java.util.List;

/**
 * Interface for attributed graphs.
 *
 * Both nodes and edges can have attributes.
 * @param <N> The type of the nodes
 * @param <E> The type of the edges
 */
public interface AttributedGraph<N extends AttributedNode, E extends AttributedGraphEdge<N>> {
	N findNodeById(String id);
	
	List<E> findOutgoingEdges(N node);
	List<E> findIncomingEdges(N node);
}
