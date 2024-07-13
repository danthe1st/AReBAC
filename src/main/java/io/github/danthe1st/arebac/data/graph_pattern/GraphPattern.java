package io.github.danthe1st.arebac.data.graph_pattern;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.MutualExclusionConstraint;

public record GraphPattern(
		GPGraph graph,
		List<MutualExclusionConstraint> mutualExclusionConstraints,
		Map<GPNode, List<AttributeRequirement>> nodeRequirements,
		Map<GPEdge, List<AttributeRequirement>> edgeRequirements,
		List<GPNode> returnedNodes) {
	
	// TODO category
	
	public GraphPattern {
		Objects.requireNonNull(graph);
		Objects.requireNonNull(mutualExclusionConstraints);
		Objects.requireNonNull(nodeRequirements);
		Objects.requireNonNull(edgeRequirements);
		
		mutualExclusionConstraints = List.copyOf(mutualExclusionConstraints);
		nodeRequirements = Map.copyOf(nodeRequirements);
		edgeRequirements = Map.copyOf(edgeRequirements);
		returnedNodes = List.copyOf(returnedNodes);
		
		for(GPNode node : returnedNodes){
			checkGraphHasNode(node);
		}
		for(GPNode node : nodeRequirements.keySet()){
			checkGraphHasNode(node);
		}
		for(GPEdge edge : edgeRequirements.keySet()){
			checkGraphHasEdge(edge);
		}
		for(MutualExclusionConstraint mutualExclusionConstraint : mutualExclusionConstraints){
			checkGraphHasNode(mutualExclusionConstraint.first());
			checkGraphHasNode(mutualExclusionConstraint.second());
		}
	}
	
	private void checkGraphHasNode(GPNode node) {
		GPNode graphNode = graph.nodes().get(node.id());
		if(!node.equals(graphNode)){
			throw new IllegalArgumentException("node missing in graph: " + node);
		}
	}
	
	private void checkGraphHasEdge(GPEdge edge) {
		List<GPEdge> edges = graph.outgoingEdges().get(edge.source());
		if(edges == null){
			throw new IllegalArgumentException("edge missing in graph: " + edge);
		}
		for(GPEdge e : edges){
			if(e.equals(edge)){
				return;
			}
		}
		throw new IllegalArgumentException("edge missing in graph: " + edge);
	}
}