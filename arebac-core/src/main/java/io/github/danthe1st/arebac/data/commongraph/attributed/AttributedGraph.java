package io.github.danthe1st.arebac.data.commongraph.attributed;

import java.util.Collection;

/**
 * Interface for attributed graphs.
 *
 * Both nodes and edges can have attributes.
 * @param <N> The type of the nodes
 * @param <E> The type of the edges
 */
public interface AttributedGraph<N extends AttributedNode, E extends AttributedEdge<N>> {
	N findNodeById(String id);

	Collection<E> findOutgoingEdges(N node, String edgeType);
	Collection<E> findIncomingEdges(N node, String edgeType);
	
	/**
	 * Checks whether an attribute is unique for a specified node type.
	 *
	 * Returning {@code false} is also allowed if there is no concept of uniqueness, the uniqueness is uncertain or if {@link AttributedGraph#getNodeByUniqueAttribute(String, String, AttributeValue)} is not implemented
	 * @param key the key of the attribute to check
	 * @param nodeType the node type
	 * @return {@code true} if the attribute is unique, else {@code false}
	 * @see AttributedGraph#getNodeByUniqueAttribute(String, String, AttributeValue)
	 */
	default boolean isAttributeUniqueForNodeType(String key, String nodeType) {
		return false;
	}

	/**
	 * Get a node by a unique attribute value as specified by {@link AttributedGraph#isAttributeUniqueForNodeType(String, String)}.
	 * @param nodeType the node type where the attribute is unique
	 * @param key the key of the unique attribute
	 * @param value the value of the unique attribute
	 * @return the node associated with the unique attribute value
	 * @throws UnsupportedOperationException if the attribute is not actually unique
	 * @see AttributedGraph#isAttributeUniqueForNodeType(String, String)
	 */
	default N getNodeByUniqueAttribute(String nodeType, String key, AttributeValue<?> value) {
		throw new UnsupportedOperationException();
	}
}
