package io.github.danthe1st.arebac.data.graph;

import java.util.Map;
import java.util.Objects;

import io.github.danthe1st.arebac.data.genericdb.GeneralDBEdge;
import io.github.danthe1st.arebac.data.graph_pattern.AttributeValue;

public record GraphEdge(
		GraphNode source, GraphNode target,
		String id, String edgeType,
		Map<String, AttributeValue<?>> attributes) implements GeneralDBEdge<GraphNode> {
	public GraphEdge {
		Objects.requireNonNull(source);
		Objects.requireNonNull(target);
		Objects.requireNonNull(id);
		Objects.requireNonNull(attributes);
		
		attributes = Map.copyOf(attributes);
	}
}
