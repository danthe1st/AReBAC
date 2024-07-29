package io.github.danthe1st.arebac.data.graph_pattern;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.MutualExclusionConstraint;
import io.github.danthe1st.arebac.gpeval.GPEval;

/**
 * A graph pattern.
 *
 * A graph pattern consists of a graph, nodes of interest (which are returned by {@link GPEval GP-Eval}) and any number of the following constraints:
 * <ul>
 * 	<li>Mutual exclusion constraints specifying two nodes must not be the same</li>
 * 	<li>Node attribute requirements specifying specific nodes must have attributes matching a criterion</li>
 *  <li>Edge attribute requirements specifying specific edges must have attributes matching a criterion</li>
 * </ul>
 */
public record GraphPattern(
		GPGraph graph,
		List<MutualExclusionConstraint> mutualExclusionConstraints,
		Map<GPNode, List<AttributeRequirement>> nodeRequirements,
		Map<GPEdge, List<AttributeRequirement>> edgeRequirements,
		List<GPNode> returnedNodes,
		Map<String, GPNode> actorsToNodes) {
	
	// TODO figure out whether categories are necessary and (if so) add them
	
	public GraphPattern {
		Objects.requireNonNull(graph);
		Objects.requireNonNull(mutualExclusionConstraints);
		Objects.requireNonNull(nodeRequirements);
		Objects.requireNonNull(edgeRequirements);
		Objects.requireNonNull(returnedNodes);
		Objects.requireNonNull(actorsToNodes);
		
		mutualExclusionConstraints = List.copyOf(mutualExclusionConstraints);
		nodeRequirements = Map.copyOf(nodeRequirements);
		edgeRequirements = Map.copyOf(edgeRequirements);
		returnedNodes = List.copyOf(returnedNodes);
		actorsToNodes = Map.copyOf(actorsToNodes);
		
		for(GPNode node : returnedNodes){
			checkGraphHasNode(graph, node);
		}
		for(GPNode node : nodeRequirements.keySet()){
			checkGraphHasNode(graph, node);
		}
		for(GPEdge edge : edgeRequirements.keySet()){
			checkGraphHasEdge(graph, edge);
		}
		for(MutualExclusionConstraint mutualExclusionConstraint : mutualExclusionConstraints){
			checkGraphHasNode(graph, mutualExclusionConstraint.first());
			checkGraphHasNode(graph, mutualExclusionConstraint.second());
		}
		for(GPNode node : actorsToNodes.values()){
			checkGraphHasNode(graph, node);
		}
	}
	
	private static void checkGraphHasNode(GPGraph graph, GPNode node) {
		GPNode graphNode = graph.nodes().get(node.id());
		if(!node.equals(graphNode)){
			throw new IllegalArgumentException("node missing in graph: " + node);
		}
	}
	
	private static void checkGraphHasEdge(GPGraph graph, GPEdge edge) {
		Collection<GPEdge> edges = graph.outgoingEdges().get(edge.source());
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
