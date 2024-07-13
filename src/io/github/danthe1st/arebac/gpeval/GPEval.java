package io.github.danthe1st.arebac.gpeval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.danthe1st.arebac.data.graph.Graph;
import io.github.danthe1st.arebac.data.graph.GraphEdge;
import io.github.danthe1st.arebac.data.graph.GraphNode;
import io.github.danthe1st.arebac.data.graph_pattern.GPEdge;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.MutualExclusionConstraint;

public class GPEval {

	private final Graph graph;
	private final GraphPattern pattern;

	private final Map<GPNode, Set<GPNode>> mutualExclusionConstraints;

	// node in pattern -> list of nodes in graph
	private Map<GPNode, Set<GraphNode>> candidates = new HashMap<>();

	// node in pattern -> node in graph
	private Map<GPNode, GraphNode> assignments = new HashMap<>();

	private Set<List<GraphNode>> results = new HashSet<>();

	public static Set<List<GraphNode>> evaluate(Graph graph, GraphPattern pattern) {

		GPEval eval = new GPEval(graph, pattern);
		try{
			eval.init();
		}catch(NoResultException e){
			return Set.of();
		}

		eval.run(new HashMap<>());

		return eval.results;// returns nodes corresponding to returned nodes in graph pattern
	}

	private GPEval(Graph graph, GraphPattern pattern) {
		Objects.requireNonNull(graph);
		Objects.requireNonNull(pattern);
		this.graph = graph;
		this.pattern = pattern;
		Map<GPNode, Set<GPNode>> mutualExclusionConstraints = new HashMap<>();
		for(MutualExclusionConstraint constraint : pattern.mutualExclusionConstraints()){
			GPNode first = constraint.first();
			GPNode second = constraint.second();
			addToMultimap(mutualExclusionConstraints, first, second);
			addToMultimap(mutualExclusionConstraints, second, first);
		}
		this.mutualExclusionConstraints = Map.copyOf(mutualExclusionConstraints);
	}

	public GPEval(Graph graph, GraphPattern pattern, Map<GPNode, Set<GPNode>> mutualExclusionConstraints, Map<GPNode, Set<GraphNode>> candidates, Map<GPNode, GraphNode> assignments, Set<List<GraphNode>> results) {
		this.graph = graph;
		this.pattern = pattern;
		this.mutualExclusionConstraints = mutualExclusionConstraints;
		this.candidates = candidates;
		this.assignments = assignments;
		this.results = results;
	}

	private void addToMultimap(Map<GPNode, Set<GPNode>> mutualExclusionConstraints, GPNode key, GPNode value) {
		mutualExclusionConstraints.merge(key, new HashSet<>(Set.of(value)), (a, b) -> {
			a.addAll(b);
			return a;
		});
	}

	private void init() throws NoResultException {
		setupFixedVertices();
		checkRequirementsForFixedVertices();
	}

	private void setupFixedVertices() throws NoResultException {
		// TODO: fixed vertices other than fixed ID?
		for(Map.Entry<GPNode, List<AttributeRequirement>> attributeRequirementEntry : pattern.nodeRequirements().entrySet()){
			GPNode patternNode = attributeRequirementEntry.getKey();
			List<AttributeRequirement> requirements = attributeRequirementEntry.getValue();
			for(AttributeRequirement requirement : requirements){
				if(AttributeRequirement.ID_KEY.equals(requirement.key())){
					GraphNode graphNode = graph.nodes().get(requirement.key());
					if(graphNode == null){
						throw new NoResultException("Fixed node cannot be found");
					}
					candidates.put(patternNode, new HashSet<>(Set.of(graphNode)));
				}
			}
		}
	}

	private void checkRequirementsForFixedVertices() throws NoResultException {
		for(Map.Entry<GPNode, Set<GraphNode>> assignedNodeEntry : candidates.entrySet()){
			GPNode patternNode = assignedNodeEntry.getKey();
			Set<GraphNode> graphNodes = assignedNodeEntry.getValue();
			for(Iterator<GraphNode> graphNodeIterator = graphNodes.iterator(); graphNodeIterator.hasNext();){
				GraphNode graphNode = graphNodeIterator.next();
				if(!checkRequirementsForNode(patternNode, graphNode)){
					graphNodeIterator.remove();
				}
			}
			if(graphNodes.isEmpty()){
				throw new NoResultException("node cannot be assigned without violating constraints: " + patternNode);
			}

			// TODO check edge requirements
			// TODO what about mutual exclusion constraints?
		}
	}

	private boolean checkRequirementsForNode(GPNode patternNode, GraphNode graphNode) {
		for(AttributeRequirement requirement : pattern.nodeRequirements().get(patternNode)){
			if(!requirement.evaluate(graphNode.attributes())){
				return false;
			}
		}
		return true;
	}

	private Set<GPNode> run(Map<GPNode, Set<GPNode>> incomingConflicts) {// returns nodes for backjumping
		if(assignments.size() == pattern.graph().nodes().size()){

			List<GraphNode> result = new ArrayList<>();
			for(GPNode nodeToReturn : pattern.returnedNodes()){
				result.add(Objects.requireNonNull(assignments.get(nodeToReturn)));
			}

			results.add(List.copyOf(result));
			return Set.copyOf(pattern.returnedNodes());
		}

		boolean deadEnd = true;
		Set<GPNode> conflicts = new HashSet<>();
		Set<GPNode> outgoingConflicts = new HashSet<>();

		GPNode currentNode = pickNextNode();
		Set<GraphNode> currentNodeCandidates = candidates.get(currentNode);
		filterMutualExclusionConstraints(currentNode, currentNodeCandidates, mutualExclusionConstraints.get(currentNode), incomingConflicts.get(currentNode));
		for(GraphNode candidateNode : currentNodeCandidates){
			Map<GPNode, Set<GraphNode>> newCandidates = new HashMap<>(candidates);
			newCandidates.remove(currentNode);
			Map<GPNode, GraphNode> newAssignments = new HashMap<>(assignments);
			newAssignments.put(currentNode, candidateNode);
			Map<GPNode, Set<GPNode>> newIncomingConflicts = incomingConflicts// deep copy
				.entrySet()
				.stream()
				.collect(
						Collectors.toMap(
								Map.Entry::getKey,
								e -> new HashSet<>(e.getValue())
						)
				);
			GPEval child = new GPEval(graph, pattern, mutualExclusionConstraints, newCandidates, newAssignments, results);
			boolean valid = child.forwardChecking(currentNode, newIncomingConflicts, outgoingConflicts);
			if(valid){
				deadEnd = false;
				Set<GPNode> jump = child.run(incomingConflicts);
				if(!jump.isEmpty() && !jump.contains(currentNode)){
					return jump;
				}
				conflicts.addAll(jump);
			}
		}

		if(deadEnd){
			addAllIncomingConflictsForNode(incomingConflicts, conflicts, currentNode);
			for(GPNode node : outgoingConflicts){
				addAllIncomingConflictsForNode(incomingConflicts, conflicts, node);
			}
			return conflicts;
		}
		conflicts.addAll(pattern.returnedNodes());
		for(GPNode node : pattern.returnedNodes()){
			addAllIncomingConflictsForNode(incomingConflicts, conflicts, node);
		}
		if(!outgoingConflicts.isEmpty()){
			addAllIncomingConflictsForNode(incomingConflicts, conflicts, currentNode);
			for(GPNode node : outgoingConflicts){
				addAllIncomingConflictsForNode(incomingConflicts, conflicts, node);
			}
		}
		return conflicts;
	}

	private void addAllIncomingConflictsForNode(Map<GPNode, Set<GPNode>> incomingConflicts, Set<GPNode> conflicts, GPNode currentNode) {
		Set<GPNode> nodeIncomingConflicts = incomingConflicts.get(currentNode);
		if(nodeIncomingConflicts != null){
			conflicts.addAll(nodeIncomingConflicts);
		}
	}

	private GPNode pickNextNode() {
		GPNode candidate = null;
		int numberOfPossibilities = Integer.MAX_VALUE;
		for(Entry<GPNode, Set<GraphNode>> candidateEntry : candidates.entrySet()){
			int possibilities = candidateEntry.getValue().size();
			GPNode potentialCandidate = candidateEntry.getKey();
			if(assignments.containsKey(potentialCandidate)){
				System.err.println("WARNING: element of candidate set already assigned");
			}else if(possibilities < numberOfPossibilities){
				candidate = potentialCandidate;
				numberOfPossibilities = possibilities;
			}
		}
		Objects.requireNonNull(candidate);
		return candidate;
	}

	private void filterMutualExclusionConstraints(GPNode currentCandidateNode, Set<GraphNode> candidatesForNode, Set<GPNode> exclusionConstraints, Set<GPNode> incomingConflicts) {
		for(Iterator<GraphNode> it = candidatesForNode.iterator(); it.hasNext();){
			GraphNode graphCandidate = it.next();
			for(GPNode exclusionConstraint : exclusionConstraints){
				if(graphCandidate.equals(assignments.get(exclusionConstraint))){
					incomingConflicts.add(exclusionConstraint);
					it.remove();
				}
			}
		}
	}

	private boolean forwardChecking(GPNode currentNode, Map<GPNode, Set<GPNode>> incomingConflicts, Set<GPNode> outgoingConflicts) {

		List<RelevantEdge> relevantEdges = getRelevantEdges(currentNode);
		for(RelevantEdge relevantEdge : relevantEdges){
			GPNode otherNode = relevantEdge.otherNode();
			if(!assignments.containsKey(otherNode)){
				List<GraphNode> neighbors = getNeighborsSatisfyingEdgeAndAttributeRequirements(currentNode, relevantEdge);
				Set<GraphNode> otherNodeCandidates = candidates.get(otherNode);
				assert otherNodeCandidates == null || !otherNodeCandidates.isEmpty();// I think this shouldn't happen, null is written as empty in the paper
				Set<GPNode> otherNodeIncomingConflicts = incomingConflicts.computeIfAbsent(otherNode, n -> new HashSet<>());
				Set<GPNode> currentNodeIncomingConflicts = incomingConflicts.computeIfAbsent(currentNode, n -> new HashSet<>());
				if(otherNodeCandidates == null || !neighbors.containsAll(otherNodeCandidates)){
					otherNodeIncomingConflicts.addAll(currentNodeIncomingConflicts);
					otherNodeIncomingConflicts.add(currentNode);
				}
				if(otherNodeCandidates == null){
					otherNodeCandidates = new HashSet<>(neighbors);
					candidates.put(otherNode, otherNodeCandidates);
				}else{
					otherNodeCandidates.addAll(neighbors);
				}
				if(otherNodeCandidates.isEmpty()){
					outgoingConflicts.add(otherNode);
					return false;
				}
			}
		}

		return true;
	}

	private List<GraphNode> getNeighborsSatisfyingEdgeAndAttributeRequirements(GPNode currentNode, RelevantEdge relevantEdge) {
		GraphNode currentNodeInDB = assignments.get(currentNode);
		List<GraphEdge> graphEdges;
		Function<GraphEdge, GraphNode> neighborFinder;
		if(relevantEdge.isOutgoing()){
			graphEdges = graph.outgoingEdges().get(currentNodeInDB);
			neighborFinder = GraphEdge::target;
		}else{
			graphEdges = graph.incomingEdges().get(currentNodeInDB);
			neighborFinder = GraphEdge::source;
		}
		if(graphEdges == null){
			graphEdges = List.of();
		}
		List<GraphNode> neighborsSatisfyingRequirements = new ArrayList<>();
		for(GraphEdge graphEdge : graphEdges){
			GraphNode neighbor = neighborFinder.apply(graphEdge);
			if(satisfiesRequirements(relevantEdge, graphEdge, neighbor)){
				neighborsSatisfyingRequirements.add(neighbor);
			}
		}
		return neighborsSatisfyingRequirements;
	}

	private boolean satisfiesRequirements(RelevantEdge currentEdge, GraphEdge graphEdge, GraphNode neighbor) {
		return currentEdge.edge.edgeType().equals(graphEdge.edgeType()) &&
				currentEdge.otherNode.nodeType().equals(neighbor.nodeType()) &&
				checkEdgeAttributeRequirements(currentEdge, graphEdge) &&
				checkNodeAttributeRequirements(currentEdge.otherNode, neighbor);
	}

	// TODO use interface to reduce duplicate code between checkNodeAttributeRequirements and checkEdgeAttributeRequirements
	private boolean checkNodeAttributeRequirements(GPNode otherNode, GraphNode neighbor) {
		List<AttributeRequirement> requirements = pattern.nodeRequirements().get(otherNode);
		if(requirements == null){
			return true;
		}
		for(AttributeRequirement attributeRequirement : requirements){
			if(!attributeRequirement.evaluate(neighbor.attributes())){
				return false;
			}
		}
		return true;
	}
	
	private boolean checkEdgeAttributeRequirements(RelevantEdge currentEdge, GraphEdge graphEdge) {
		List<AttributeRequirement> requirements = pattern.edgeRequirements().get(currentEdge.edge);
		if(requirements == null){
			return true;
		}
		for(AttributeRequirement attributeRequirement : requirements){
			if(!attributeRequirement.evaluate(graphEdge.attributes())){
				return false;
			}
		}
		return true;
	}
	
	private List<RelevantEdge> getRelevantEdges(GPNode currentNode) {
		List<GPEdge> outgoingEdges = pattern.graph().outgoingEdges().get(currentNode);
		List<GPEdge> incomingEdges = pattern.graph().incomingEdges().get(currentNode);

//		Map<GPEdge, GPNode> relevantEdges = new HashMap<>();
		List<RelevantEdge> relevantEdges = new ArrayList<>();
		for(GPEdge edge : incomingEdges){
//			relevantEdges.put(edge, edge.source());
			relevantEdges.add(new RelevantEdge(edge, edge.source(), false));
		}
		for(GPEdge edge : outgoingEdges){
//			relevantEdges.put(edge, edge.target());
			relevantEdges.add(new RelevantEdge(edge, edge.target(), true));
		}
		return relevantEdges;
	}

	record RelevantEdge(GPEdge edge, GPNode otherNode, boolean isOutgoing) {
	}

}
