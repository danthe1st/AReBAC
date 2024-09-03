package io.github.danthe1st.arebac.data.graph_pattern;

import java.util.Objects;
import java.util.UUID;

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
		if(id == null){
			id = UUID.randomUUID().toString();
		}
		Objects.requireNonNull(edgeType);
	}
	
	@Override
	public boolean hasEdgeType(String edgeType) {
		return this.edgeType().equals(edgeType);
	}
}
