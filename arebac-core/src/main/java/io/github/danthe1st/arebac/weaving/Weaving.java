package io.github.danthe1st.arebac.weaving;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import io.github.danthe1st.arebac.data.graph_pattern.GPEdge;
import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.MutualExclusionConstraint;

public final class Weaving {
    private final List<GraphPattern> inputPatterns;
    private final Map<String, GPNode> actorsToCombinedNodes = new HashMap<>();
    private final Map<VertexInGraphPattern, GPNode> vertexToNodes = new HashMap<>();

	public static GraphPattern combinePatterns(List<GraphPattern> inputPatterns) {
		return new Weaving(inputPatterns).run();
	}
	
    private Weaving(List<GraphPattern> inputPatterns) {
        Objects.requireNonNull(inputPatterns);
        this.inputPatterns = List.copyOf(inputPatterns);
    }

	private GraphPattern run() {
        findEquivalences();
		// TODO if needed: combined pattern has all categories of each single pattern (currently there are no categories)
		LinkedHashMap<EdgeInGraphPattern, GPEdge> edgeMapping = createEdges();
		GPGraph graph = new GPGraph(new ArrayList<>(new HashSet<>(vertexToNodes.values())), new ArrayList<>(edgeMapping.values()));
		List<MutualExclusionConstraint> mutualExclusionConstraints = createMutualExclusionConstraints();
		Map<GPNode, List<AttributeRequirement>> nodeRequirements = createNodeRequirements();
		Map<GPEdge, List<AttributeRequirement>> edgeRequirements = createEdgeRequirements(edgeMapping);
		List<GPNode> returnedNodes = createReturnedNodes();
		return new GraphPattern(graph, mutualExclusionConstraints, nodeRequirements, edgeRequirements, returnedNodes, actorsToCombinedNodes);
    }

	private void findEquivalences() {
		for(int patternIndex = 0; patternIndex < inputPatterns.size(); patternIndex++){
			GraphPattern graphPattern = inputPatterns.get(patternIndex);
			Map<String, GPNode> actorsToNodes = graphPattern.actorsToNodes();
			for(Entry<String, GPNode> actorToNode : actorsToNodes.entrySet()){
				String actor = actorToNode.getKey();
				GPNode node = actorToNode.getValue();
				VertexInGraphPattern nodeInPattern = new VertexInGraphPattern(patternIndex, node);
				
				if(actorsToCombinedNodes.containsKey(actor)){
					GPNode foundNode = actorsToCombinedNodes.get(actor);
					if(!foundNode.nodeType().equals(node.nodeType())){
						throw new IllegalArgumentException("node type doesn't match");
					}
					vertexToNodes.put(nodeInPattern, foundNode);
				}else{
					GPNode newNode = new GPNode(generateId(patternIndex, node.id()), node.nodeType());
					actorsToCombinedNodes.put(actor, newNode);
					vertexToNodes.put(nodeInPattern, newNode);
				}
			}
			for(GPNode node : graphPattern.graph().nodes().values()){
				VertexInGraphPattern nodeInPattern = new VertexInGraphPattern(patternIndex, node);
				GPNode newNode = new GPNode(generateId(patternIndex, node.id()), node.nodeType());
				if(!vertexToNodes.containsKey(nodeInPattern)){
					vertexToNodes.put(nodeInPattern, newNode);
				}
			}
		}
	}
	
	private LinkedHashMap<EdgeInGraphPattern, GPEdge> createEdges() {
		LinkedHashMap<EdgeInGraphPattern, GPEdge> oldToNewEdges = new LinkedHashMap<>();
		for(int patternId = 0; patternId < inputPatterns.size(); patternId++){
			GraphPattern graphPattern = inputPatterns.get(patternId);
			for(Collection<GPEdge> edgesOfNode : graphPattern.graph().incomingEdges().values()){
				for(GPEdge oldEdgeInPattern : edgesOfNode){
					EdgeInGraphPattern edgeInPattern = new EdgeInGraphPattern(patternId, oldEdgeInPattern);
					GPEdge newEdge = convertEdge(edgeInPattern);
					oldToNewEdges.put(edgeInPattern, newEdge);
				}
			}
		}
		return oldToNewEdges;
	}
	
	private Map<GPNode, List<AttributeRequirement>> createNodeRequirements() {
		Map<GPNode, List<AttributeRequirement>> nodeRequirements = new HashMap<>();
		for(int patternId = 0; patternId < inputPatterns.size(); patternId++){
			GraphPattern graphPattern = inputPatterns.get(patternId);
			Map<GPNode, List<AttributeRequirement>> oldRequirements = graphPattern.nodeRequirements();
			for(Map.Entry<GPNode, List<AttributeRequirement>> oldRequirementEntry : oldRequirements.entrySet()){
				GPNode requiredNode = oldRequirementEntry.getKey();
				List<AttributeRequirement> attributeConstraints = oldRequirementEntry.getValue();
				GPNode newNode = convertNode(patternId, requiredNode);
				nodeRequirements.merge(newNode, attributeConstraints, (a,b) -> {
					List<AttributeRequirement> merged = new ArrayList<>(a);
					merged.addAll(b);
					return merged;
				});
			}
		}
		return nodeRequirements;
	}

	private Map<GPEdge, List<AttributeRequirement>> createEdgeRequirements(LinkedHashMap<EdgeInGraphPattern, GPEdge> edgeMapping) {
		Map<GPEdge, List<AttributeRequirement>> edgeRequirements = new HashMap<>();
		for(int patternId = 0; patternId < inputPatterns.size(); patternId++){
			GraphPattern graphPattern = inputPatterns.get(patternId);
			Map<GPEdge, List<AttributeRequirement>> oldRequirements = graphPattern.edgeRequirements();
			for(Map.Entry<GPEdge, List<AttributeRequirement>> oldRequirementEntry : oldRequirements.entrySet()){
				GPEdge requiredEdge = oldRequirementEntry.getKey();
				List<AttributeRequirement> attributeConstraints = oldRequirementEntry.getValue();
				edgeRequirements.put(edgeMapping.get(new EdgeInGraphPattern(patternId, requiredEdge)), attributeConstraints);
			}
		}
		return edgeRequirements;
	}

	private List<MutualExclusionConstraint> createMutualExclusionConstraints() {
		List<MutualExclusionConstraint> mutualExclusionConstraints = new ArrayList<>();
		for(int patternId = 0; patternId < inputPatterns.size(); patternId++){
			GraphPattern graphPattern = inputPatterns.get(patternId);
			for(MutualExclusionConstraint mutualExclusionConstraint : graphPattern.mutualExclusionConstraints()){
				VertexInGraphPattern first = new VertexInGraphPattern(patternId, mutualExclusionConstraint.first());
				VertexInGraphPattern second = new VertexInGraphPattern(patternId, mutualExclusionConstraint.second());
				mutualExclusionConstraints.add(new MutualExclusionConstraint(vertexToNodes.get(first), vertexToNodes.get(second)));
			}
		}
		return mutualExclusionConstraints;
	}

	private List<GPNode> createReturnedNodes() {
		List<GPNode> returnedNodes = new ArrayList<>();
		for(int patternId = 0; patternId < inputPatterns.size(); patternId++){
			GraphPattern graphPattern = inputPatterns.get(patternId);
			for(GPNode oldNode : graphPattern.returnedNodes()){
				returnedNodes.add(convertNode(patternId, oldNode));
			}
		}
		return returnedNodes;
	}
	
	private GPEdge convertEdge(EdgeInGraphPattern edgeInPattern) {
		GPNode newSource = convertNode(edgeInPattern.patternId(), edgeInPattern.edge().source());
		GPNode newTarget = convertNode(edgeInPattern.patternId(), edgeInPattern.edge().target());
		return new GPEdge(newSource, newTarget, generateId(edgeInPattern.patternId(), edgeInPattern.edge().id()), edgeInPattern.edge().edgeType());
	}

	private GPNode convertNode(int patternId, GPNode requiredNode) {
		VertexInGraphPattern nodeInPattern = new VertexInGraphPattern(patternId, requiredNode);
		return Objects.requireNonNull(vertexToNodes.get(nodeInPattern));
		
	}
	private String generateId(int patternIndex, String id) {
		return "p" + patternIndex + "," + id;
	}
	
	private record VertexInGraphPattern(int patternId, GPNode vertex) {
		
	}
	
	private record EdgeInGraphPattern(int patternId, GPEdge edge) {
		
	}
}
