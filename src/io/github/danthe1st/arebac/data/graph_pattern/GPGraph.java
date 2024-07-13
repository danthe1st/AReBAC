package io.github.danthe1st.arebac.data.graph_pattern;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.danthe1st.arebac.data.commongraph.CommonGraph;

public record GPGraph(Map<String, GPNode> nodes,
		Map<GPNode, List<GPEdge>> outgoingEdges,
		Map<GPNode, List<GPEdge>> incomingEdges) implements CommonGraph<GPNode, GPEdge> {

	public GPGraph(List<GPNode> nodes, List<GPEdge> edges) {
		this(
				nodes.stream().collect(Collectors.toMap(GPNode::id, Function.identity())),
				edges.stream().collect(Collectors.groupingBy(GPEdge::source)),
				edges.stream().collect(Collectors.groupingBy(GPEdge::target))
		);
	}

	public GPGraph {
		Objects.requireNonNull(nodes);
		Objects.requireNonNull(outgoingEdges);
		Objects.requireNonNull(incomingEdges);

		nodes = Map.copyOf(nodes);
		outgoingEdges = Map.copyOf(outgoingEdges);
		incomingEdges = Map.copyOf(outgoingEdges);
		validate();
	}
}
