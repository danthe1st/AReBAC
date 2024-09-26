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
	/**
	 * @see Weaving#findEquivalences()
	 * @see Weaving#convertNode(int, GPNode)
	 */
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
		LinkedHashMap<EdgeInGraphPattern, GPEdge> edgeMapping = createEdges();
		GPGraph graph = new GPGraph(new ArrayList<>(new HashSet<>(vertexToNodes.values())), new ArrayList<>(new HashSet<>(edgeMapping.values())));
		List<MutualExclusionConstraint> mutualExclusionConstraints = createMutualExclusionConstraints();
		Map<GPNode, List<AttributeRequirement>> nodeRequirements = createNodeRequirements();
		Map<GPEdge, List<AttributeRequirement>> edgeRequirements = createEdgeRequirements(edgeMapping);
		List<GPNode> returnedNodes = createReturnedNodes();
		return new GraphPattern(graph, mutualExclusionConstraints, nodeRequirements, edgeRequirements, returnedNodes, actorsToCombinedNodes);
    }

	/**
	 * Populates {@link Weaving#vertexToNodes} such that each vertex in each of the input graph patterns are associated with a {@link GPNode}.
	 * If vertices of different graph patterns are associated with the same actor, these point to the same {@link GPNode}.
	 * This requires both vertices having the same label.
	 */
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

	/**
	 * Creates a mapping from edges in the old graph pattern to edges in the new graph pattern.
	 * If an edge is present in the old graph pattern, it must also be present in the new graph pattern.
	 * The vertices of the new edges are found in {@link Weaving#vertexToNodes} i.e. edges use the vertices in the resulting graph pattern
	 * @return A mapping from edges in the old graph pattern to edges in the new graph pattern
	 */
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

	/**
	 * Creates all vertex attribute requirements in the new graph pattern.
	 * Each vertex attribute requirement in an input pattern corresponds to a vertex attribute requirement in the resulting pattern but using the vertices of the new pattern in {@link Weaving#vertexToNodes}.
	 * @return The node attribute requirements of the new pattern
	 */
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

	/**
	 * Creates all edge attribute requirements in the new graph pattern.
	 * Each edge attribute requirement in an input pattern corresponds to an edge attribute requirement in the resulting pattern but vertices in the resulting pattern are looked up from {@link Weaving#vertexToNodes}.
	 * @param edgeMapping The mapping from edges in all input graph patterns to edges in the resulting graph pattern
	 * @return All edge attribute requirements of the new graph pattern
	 */
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

	/**
	 * Creates all mutual exclusion constraints in the new graph pattern.
	 * All mutual exclusion constraints in any of the input patterns must be present in the resulting pattern but vertices are transformed/looked up using {@link Weaving#vertexToNodes}.
	 * @return A {@link List} with all mutual exclusion constraints of the new graph pattern
	 */
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

	/**
	 * Computes the returned vertices of the resulting graph pattern.
	 * This method transforms vertices in the input patterns using {@link Weaving#vertexToNodes}.
	 * @return a {@link List} containing all returned vertices of the resulting graph pattern
	 */
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

	/**
	 * converts an edge in an input pattern to an edge in the result pattern
	 * @param edgeInPattern information about the edge in the input pattern
	 * @return The new {@link GPEdge} in the resulting pattern
	 */
	private GPEdge convertEdge(EdgeInGraphPattern edgeInPattern) {
		GPNode newSource = convertNode(edgeInPattern.patternId(), edgeInPattern.edge().source());
		GPNode newTarget = convertNode(edgeInPattern.patternId(), edgeInPattern.edge().target());
		return new GPEdge(newSource, newTarget, generateId(edgeInPattern.patternId(), edgeInPattern.edge().id()), edgeInPattern.edge().edgeType());
	}

	/**
	 * Looks up a vertex in an input pattern using {@link Weaving#vertexToNodes} and returns the corresponding edge in the resulting pattern.
	 * @param patternId The index of the input pattern
	 * @param requiredNode the {@link GPNode vertex} in the input pattern
	 * @return The {@link GPNode vertex} in the resulting pattern
	 */
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
