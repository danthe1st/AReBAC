package io.github.danthe1st.arebac.data.graph;

import java.util.Map;
import java.util.Objects;

import io.github.danthe1st.arebac.data.commongraph.CommonNode;
import io.github.danthe1st.arebac.data.graph_pattern.AttributeValue;

public record GraphNode(
		String id,
		String nodeType,
		Map<String, AttributeValue<?>> attributes) implements CommonNode, AttributeAware {
	public GraphNode {
		Objects.requireNonNull(id);
		Objects.requireNonNull(nodeType);
		Objects.requireNonNull(attributes);
		
		attributes = Map.copyOf(attributes);
	}
}
