package io.github.danthe1st.arebac.data.graph;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.danthe1st.arebac.data.commongraph.CommonInMemoryGraph;
import io.github.danthe1st.arebac.data.genericdb.GeneralDBGraph;

public record Graph(Map<String, GraphNode> nodes,
		Map<GraphNode, List<GraphEdge>> outgoingEdges,
		Map<GraphNode, List<GraphEdge>> incomingEdges) implements CommonInMemoryGraph<GraphNode, GraphEdge>, GeneralDBGraph<GraphNode, GraphEdge> {

	public Graph(List<GraphNode> nodes, List<GraphEdge> edges) {
		this(
				nodes.stream().collect(Collectors.toMap(GraphNode::id, Function.identity())),
				edges.stream().collect(Collectors.groupingBy(GraphEdge::source)),
				edges.stream().collect(Collectors.groupingBy(GraphEdge::target))
		);
	}

	public Graph {
		Objects.requireNonNull(nodes);
		Objects.requireNonNull(outgoingEdges);
		Objects.requireNonNull(incomingEdges);

		nodes = Map.copyOf(nodes);
		outgoingEdges = Map.copyOf(outgoingEdges);
		incomingEdges = Map.copyOf(incomingEdges);
		
		CommonInMemoryGraph.validate(nodes, outgoingEdges, incomingEdges);
	}
	
	@Override
	public GraphNode findNodeById(String id) {
		return nodes().get(id);
	}
	
	@Override
	public List<GraphEdge> findOutgoingEdges(GraphNode node) {
		return Objects.requireNonNullElse(outgoingEdges().get(node), List.of());
	}
	
	@Override
	public List<GraphEdge> findIncomingEdges(GraphNode node) {
		return Objects.requireNonNullElse(incomingEdges().get(node), List.of());
	}
}
