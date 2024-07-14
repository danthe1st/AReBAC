package io.github.danthe1st.arebac.data.memory;

import java.util.Map;
import java.util.Objects;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedGraphEdge;

/**
 * Edge of an in-memory attributed graph.
 * @see InMemoryGraph
 */
public record InMemoryGraphEdge(
		InMemoryGraphNode source, InMemoryGraphNode target,
		String id, String edgeType,
		Map<String, AttributeValue<?>> attributes) implements AttributedGraphEdge<InMemoryGraphNode> {
	public InMemoryGraphEdge {
		Objects.requireNonNull(source);
		Objects.requireNonNull(target);
		Objects.requireNonNull(id);
		Objects.requireNonNull(attributes);
		
		attributes = Map.copyOf(attributes);
	}
	
	@Override
	public AttributeValue<?> getAttribute(String key) {
		return attributes().get(key);
	}
}
