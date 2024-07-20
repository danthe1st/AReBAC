package io.github.danthe1st.arebac.data.graph_pattern;

import java.util.Objects;

import io.github.danthe1st.arebac.data.commongraph.CommonEdge;

/**
 * An edge in a {@link GraphPattern graph pattern}.
 * @see GPGraph
 * @see GraphPattern
 */
public record GPEdge(
		GPNode source, GPNode target,
		String id, String edgeType) implements CommonEdge<GPNode> {
	public GPEdge {
		Objects.requireNonNull(source);
		Objects.requireNonNull(target);
		Objects.requireNonNull(id);
		Objects.requireNonNull(edgeType);
	}
}
