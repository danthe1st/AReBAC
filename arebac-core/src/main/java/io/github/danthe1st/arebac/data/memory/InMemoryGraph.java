package io.github.danthe1st.arebac.data.memory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedGraph;
import io.github.danthe1st.arebac.data.commongraph.memory.CommonInMemoryGraph;

/**
 * Implementation of an attributed in-memory graph adhering to {@link CommonInMemoryGraph} and {@link AttributedGraph}.
 * @see InMemoryGraphNode
 * @see InMemoryGraphEdge
 */
public record InMemoryGraph(Map<String, InMemoryGraphNode> nodes,
		Map<InMemoryGraphNode, List<InMemoryGraphEdge>> outgoingEdges,
		Map<InMemoryGraphNode, List<InMemoryGraphEdge>> incomingEdges) implements CommonInMemoryGraph<InMemoryGraphNode, InMemoryGraphEdge>, AttributedGraph<InMemoryGraphNode, InMemoryGraphEdge> {

	public InMemoryGraph(List<InMemoryGraphNode> nodes, List<InMemoryGraphEdge> edges) {
		this(
				nodes.stream().collect(Collectors.toMap(InMemoryGraphNode::id, Function.identity())),
				edges.stream().collect(Collectors.groupingBy(InMemoryGraphEdge::source)),
				edges.stream().collect(Collectors.groupingBy(InMemoryGraphEdge::target))
		);
	}

	public InMemoryGraph {
		Objects.requireNonNull(nodes);
		Objects.requireNonNull(outgoingEdges);
		Objects.requireNonNull(incomingEdges);

		nodes = Map.copyOf(nodes);
		outgoingEdges = copy(outgoingEdges);
		incomingEdges = copy(incomingEdges);
		
		CommonInMemoryGraph.validate(nodes, outgoingEdges, incomingEdges);
	}
	
	private static Map<InMemoryGraphNode, List<InMemoryGraphEdge>> copy(Map<InMemoryGraphNode, List<InMemoryGraphEdge>> data) {
		Map<InMemoryGraphNode, List<InMemoryGraphEdge>> copy = data
			.entrySet()
			.stream()
			.map(e -> Map.entry(e.getKey(), List.copyOf(e.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		return Map.copyOf(copy);
	}
	
	@Override
	public InMemoryGraphNode findNodeById(String id) {
		return nodes().get(id);
	}
	
	@Override
	public Collection<InMemoryGraphEdge> findOutgoingEdges(InMemoryGraphNode node) {
		return Objects.requireNonNullElse(outgoingEdges().get(node), Set.of());
	}
	
	@Override
	public Collection<InMemoryGraphEdge> findIncomingEdges(InMemoryGraphNode node) {
		return Objects.requireNonNullElse(incomingEdges().get(node), Set.of());
	}
}
