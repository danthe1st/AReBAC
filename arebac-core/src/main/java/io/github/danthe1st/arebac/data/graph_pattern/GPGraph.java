package io.github.danthe1st.arebac.data.graph_pattern;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.danthe1st.arebac.data.commongraph.memory.CommonInMemoryGraph;

/**
 * The graph of a {@link GraphPattern graph pattern}.
 * This is a non-attributed directed graph.
 * @see GPNode
 * @see GPEdge
 * @see GraphPattern
 */
public record GPGraph(Map<String, GPNode> nodes,
		Map<GPNode, Collection<GPEdge>> outgoingEdges,
		Map<GPNode, Collection<GPEdge>> incomingEdges) implements CommonInMemoryGraph<GPNode, GPEdge> {

	public GPGraph(Collection<GPNode> nodes, Collection<GPEdge> edges) {
		this(
				nodes.stream().collect(Collectors.toMap(GPNode::id, Function.identity())),
				edges.stream().collect(Collectors.groupingBy(GPEdge::source, Collectors.toCollection(HashSet::new))),
				edges.stream().collect(Collectors.groupingBy(GPEdge::target, Collectors.toCollection(HashSet::new)))
		);
	}

	public GPGraph {
		Objects.requireNonNull(nodes);
		Objects.requireNonNull(outgoingEdges);
		Objects.requireNonNull(incomingEdges);

		nodes = Map.copyOf(nodes);
		outgoingEdges = copy(outgoingEdges);
		incomingEdges = copy(incomingEdges);

		CommonInMemoryGraph.validate(nodes, outgoingEdges, incomingEdges);
	}
	
	private static Map<GPNode, Collection<GPEdge>> copy(Map<GPNode, Collection<GPEdge>> data) {
		Map<GPNode, Collection<GPEdge>> copy = data
			.entrySet()
			.stream()
			.map(e -> Map.entry(e.getKey(), List.copyOf(e.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		return Map.copyOf(copy);
	}
}
