package io.github.danthe1st.arebac.data.memory;

import java.util.Map;
import java.util.Objects;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedNode;

/**
 * Node of an in in-memory attributed graph.
 * @see InMemoryGraph
 */
public record InMemoryGraphNode(
		String id,
		String nodeType,
		Map<String, AttributeValue<?>> attributes) implements AttributedNode {
	public InMemoryGraphNode {
		Objects.requireNonNull(id);
		Objects.requireNonNull(nodeType);
		Objects.requireNonNull(attributes);
		
		attributes = Map.copyOf(attributes);
	}
	
	@Override
	public AttributeValue<?> getAttribute(String key) {
		return attributes().get(key);
	}
	
	@Override
	public boolean hasNodeType(String nodeType) {
		return this.nodeType().equals(nodeType);
	}
}
