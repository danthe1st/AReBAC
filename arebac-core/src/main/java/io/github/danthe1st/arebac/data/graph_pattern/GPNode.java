package io.github.danthe1st.arebac.data.graph_pattern;

import java.util.Objects;

import io.github.danthe1st.arebac.data.commongraph.CommonNode;

/**
 * A node in a {@link GraphPattern graph pattern}.
 * @see GPGraph
 * @see GraphPattern
 */
public record GPNode(
		String id,
		String nodeType) implements CommonNode {
	public GPNode {
		Objects.requireNonNull(id);
		Objects.requireNonNull(nodeType);
	}
	
	@Override
	public boolean hasNodeType(String nodeType) {
		return this.nodeType().equals(nodeType);
	}
}
