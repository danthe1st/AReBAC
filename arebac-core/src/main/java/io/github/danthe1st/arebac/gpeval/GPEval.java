package io.github.danthe1st.arebac.gpeval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeAware;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.StringAttribute;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedGraph;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedGraphEdge;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedNode;
import io.github.danthe1st.arebac.data.graph_pattern.GPEdge;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.MutualExclusionConstraint;
import io.github.danthe1st.arebac.gpeval.events.FilterMutualExclusionConstraintsEvent;
import io.github.danthe1st.arebac.gpeval.events.ForwardCheckingEvent;

/**
 * Implementation of the GP-eval algorithm.
 *
 * This algorithm finds all assignments in a {@link AttributedGraph graph} that match a specified {@link GraphPattern}
 *
 * @param <N> The type of nodes in the graph
 * @param <E> The type of edges in the graph
 */
public final class GPEval<N extends AttributedNode, E extends AttributedGraphEdge<N>> {

	private final AttributedGraph<N, E> graph;
	private final GraphPattern pattern;

	private final Map<GPNode, Set<GPNode>> mutualExclusionConstraints;

	// node in pattern -> list of nodes in graph
	private Map<GPNode, List<N>> candidates = new HashMap<>();

	// node in pattern -> node in graph
	private Map<GPNode, N> assignments = new HashMap<>();

	private Set<List<N>> results = new HashSet<>();

	public static <N extends AttributedNode, E extends AttributedGraphEdge<N>> Set<List<N>> evaluate(AttributedGraph<N, E> graph, GraphPattern pattern) {
		GPEval<N, E> eval = new GPEval<>(graph, pattern);
		try{
			eval.init();
		}catch(NoResultException e){
			return Set.of();
		}

		eval.run(new HashMap<>());

		return eval.results;// returns nodes corresponding to returned nodes in graph pattern
	}

	private GPEval(AttributedGraph<N, E> graph, GraphPattern pattern) {
		Objects.requireNonNull(graph);
		Objects.requireNonNull(pattern);
		this.graph = graph;
		this.pattern = pattern;
		Map<GPNode, Set<GPNode>> exclusionConstraints = new HashMap<>();
		for(MutualExclusionConstraint constraint : pattern.mutualExclusionConstraints()){
			GPNode first = constraint.first();
			GPNode second = constraint.second();
			addToMultimap(exclusionConstraints, first, second);
			addToMultimap(exclusionConstraints, second, first);
		}
		this.mutualExclusionConstraints = Map.copyOf(exclusionConstraints);
	}

	private GPEval(AttributedGraph<N, E> graph, GraphPattern pattern, Map<GPNode, Set<GPNode>> mutualExclusionConstraints, Map<GPNode, List<N>> candidates, Map<GPNode, N> assignments, Set<List<N>> results) {
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
		for(Map.Entry<GPNode, List<AttributeRequirement>> attributeRequirementEntry : pattern.nodeRequirements().entrySet()){
			GPNode patternNode = attributeRequirementEntry.getKey();
			List<AttributeRequirement> requirements = attributeRequirementEntry.getValue();
			for(AttributeRequirement requirement : requirements){
				if(AttributeRequirement.ID_KEY.equals(requirement.key())){
					AttributeValue<?> requirementValue = requirement.value();
					if(!(requirementValue instanceof StringAttribute(String value))){
						throw new IllegalStateException("ID requirements must be strings");
					}
					N graphNode = graph.findNodeById(value);

					if(graphNode == null){
						throw new NoResultException("Fixed node cannot be found");
					}
					candidates.put(patternNode, new ArrayList<>(List.of(graphNode)));
				}else if(graph.isAttributeUniqueForNodeType(requirement.key(), patternNode.nodeType()) && requirement.operator() == AttributeRequirementOperator.EQUAL){
					N graphNode = graph.getNodeByUniqueAttribute(patternNode.nodeType(), requirement.key(), requirement.value());
					if(graphNode == null){
						throw new NoResultException("Fixed node cannot be found");
					}
					candidates.put(patternNode, new ArrayList<>(List.of(graphNode)));
				}
			}
		}
	}

	private void checkRequirementsForFixedVertices() throws NoResultException {
		for(Map.Entry<GPNode, List<N>> assignedNodeEntry : candidates.entrySet()){
			GPNode patternNode = assignedNodeEntry.getKey();
			List<N> graphNodes = assignedNodeEntry.getValue();
			for(Iterator<N> graphNodeIterator = graphNodes.iterator(); graphNodeIterator.hasNext();){
				N graphNode = graphNodeIterator.next();
				if(!checkRequirementsForNode(patternNode, graphNode)){
					graphNodeIterator.remove();
				}
			}
			if(graphNodes.isEmpty()){
				throw new NoResultException("node cannot be assigned without violating constraints: " + patternNode);
			}
		}
	}

	private boolean checkRequirementsForNode(GPNode patternNode, N graphNode) {
		for(AttributeRequirement requirement : pattern.nodeRequirements().get(patternNode)){
			if(!requirement.evaluate(graphNode)){
				return false;
			}
		}
		return true;
	}

	private Set<GPNode> run(Map<GPNode, List<GPNode>> incomingConflicts) {// returns nodes for backjumping
		if(assignments.size() == pattern.graph().nodes().size()){

			List<N> result = new ArrayList<>();
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
		List<N> currentNodeCandidates = candidates.get(currentNode);
		Set<GPNode> exclusionConstraints = mutualExclusionConstraints.get(currentNode);
		if(exclusionConstraints != null){
			filterMutualExclusionConstraints(currentNodeCandidates, exclusionConstraints, Objects.requireNonNullElse(incomingConflicts.get(currentNode), new ArrayList<>()));
		}
		for(N candidateNode : currentNodeCandidates){
			Map<GPNode, List<N>> newCandidates = deepCopyExceptKey(candidates, currentNode);
			Map<GPNode, N> newAssignments = new HashMap<>(assignments);
			newAssignments.put(currentNode, candidateNode);
			Map<GPNode, List<GPNode>> newIncomingConflicts = deepCopy(incomingConflicts);
			GPEval<N, E> child = new GPEval<>(graph, pattern, mutualExclusionConstraints, newCandidates, newAssignments, results);
			boolean valid = child.forwardChecking(currentNode, newIncomingConflicts, outgoingConflicts);
			if(valid){
				deadEnd = false;
				Set<GPNode> jump = child.run(newIncomingConflicts);// the paper uses incomingConflicts (confIn) here but I think that's just a missing single quote
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

	private <K, V> Map<K, List<V>> deepCopyExceptKey(Map<K, List<V>> multimap, K keyToSkip) {
		Map<K, List<V>> result = HashMap.newHashMap(multimap.size());
		for(Entry<K, List<V>> entry : multimap.entrySet()){
			if(!entry.getKey().equals(keyToSkip)){
				List<V> newInnerList = new ArrayList<>();
				for(V innerListItem : entry.getValue()){
					newInnerList.add(innerListItem);
				}
				result.put(entry.getKey(), newInnerList);
			}
		}
		return result;
	}

	private <K, V> Map<K, List<V>> deepCopy(Map<K, List<V>> multimap) {
		Map<K, List<V>> result = HashMap.newHashMap(multimap.size());
		for(Entry<K, List<V>> entry : multimap.entrySet()){
			List<V> newInnerList = new ArrayList<>();
			for(V innerListItem : entry.getValue()){
				newInnerList.add(innerListItem);
			}
			result.put(entry.getKey(), newInnerList);
		}
		return result;
	}

	private void addAllIncomingConflictsForNode(Map<GPNode, List<GPNode>> incomingConflicts, Set<GPNode> conflicts, GPNode currentNode) {
		List<GPNode> nodeIncomingConflicts = incomingConflicts.get(currentNode);
		if(nodeIncomingConflicts != null){
			conflicts.addAll(nodeIncomingConflicts);
		}
	}

	private GPNode pickNextNode() {
		GPNode candidate = null;
		int numberOfPossibilities = Integer.MAX_VALUE;
		for(Entry<GPNode, List<N>> candidateEntry : candidates.entrySet()){
			int possibilities = candidateEntry.getValue().size();
			GPNode potentialCandidate = candidateEntry.getKey();
			if(assignments.containsKey(potentialCandidate)){
				throw new IllegalStateException("sanity check failed: element of candidate set already assigned");
			}
			if(possibilities < numberOfPossibilities){
				candidate = potentialCandidate;
				numberOfPossibilities = possibilities;
			}
		}
		Objects.requireNonNull(candidate);
		return candidate;
	}

	private void filterMutualExclusionConstraints(List<N> candidatesForNode, Set<GPNode> exclusionConstraints, List<GPNode> incomingConflicts) {
		FilterMutualExclusionConstraintsEvent event = new FilterMutualExclusionConstraintsEvent();
		event.begin();
		for(Iterator<N> it = candidatesForNode.iterator(); it.hasNext();){
			filterMutualExclusionConstraintWithSpecificCandidate(exclusionConstraints, incomingConflicts, it);
		}
		event.commit();
	}

	private void filterMutualExclusionConstraintWithSpecificCandidate(Set<GPNode> exclusionConstraints, List<GPNode> incomingConflicts, Iterator<N> it) {
		N graphCandidate = it.next();
		for(GPNode exclusionConstraint : exclusionConstraints){
			if(graphCandidate.equals(assignments.get(exclusionConstraint))){
				incomingConflicts.add(exclusionConstraint);
				it.remove();
				return;
			}
		}
	}

	private boolean forwardChecking(GPNode currentNode, Map<GPNode, List<GPNode>> incomingConflicts, Set<GPNode> outgoingConflicts) {
		ForwardCheckingEvent forwardCheckingEvent = new ForwardCheckingEvent();
		forwardCheckingEvent.begin();

		List<RelevantEdge> relevantEdges = getRelevantEdges(currentNode);
		forwardCheckingEvent.setRelevantEdges(relevantEdges.size());
		for(RelevantEdge relevantEdge : relevantEdges){
			GPNode otherNode = relevantEdge.otherNode();
			if(!assignments.containsKey(otherNode)){
				forwardCheckingEvent.addUnknownEdge();
				Collection<N> neighbors = getNeighborsSatisfyingEdgeAndAttributeRequirements(currentNode, relevantEdge, forwardCheckingEvent);
				forwardCheckingEvent.addNeighborsProcessed(neighbors.size());
				List<N> otherNodeCandidates = candidates.get(otherNode);
				assert otherNodeCandidates == null || !otherNodeCandidates.isEmpty();// I think this shouldn't happen, null is written as empty in the paper
				List<GPNode> otherNodeIncomingConflicts = incomingConflicts.computeIfAbsent(otherNode, n -> new ArrayList<>());
				List<GPNode> currentNodeIncomingConflicts = incomingConflicts.computeIfAbsent(currentNode, n -> new ArrayList<>());
				if(otherNodeCandidates == null || !neighbors.containsAll(Objects.requireNonNullElse(otherNodeCandidates, List.of()))){
					otherNodeIncomingConflicts.addAll(currentNodeIncomingConflicts);
					otherNodeIncomingConflicts.add(currentNode);
				}
				if(otherNodeCandidates == null){
					otherNodeCandidates = new ArrayList<>(neighbors);
					candidates.put(otherNode, otherNodeCandidates);
				}else{
					otherNodeCandidates.retainAll(neighbors);
				}
				if(otherNodeCandidates.isEmpty()){
					outgoingConflicts.add(otherNode);
					return false;
				}
			}
		}
		forwardCheckingEvent.commit();

		return true;
	}

	private Collection<N> getNeighborsSatisfyingEdgeAndAttributeRequirements(GPNode currentNode, RelevantEdge relevantEdge, ForwardCheckingEvent forwardCheckingEvent) {
		N currentNodeInDB = assignments.get(currentNode);
		Collection<E> graphEdges;
		Function<E, N> neighborFinder;
		if(relevantEdge.isOutgoing()){
			graphEdges = graph.findOutgoingEdges(currentNodeInDB);
			neighborFinder = AttributedGraphEdge::target;
		}else{
			graphEdges = graph.findIncomingEdges(currentNodeInDB);
			neighborFinder = AttributedGraphEdge::source;
		}
		graphEdges = Objects.requireNonNullElse(graphEdges, List.of());
		Collection<N> neighborsSatisfyingRequirements = new HashSet<>();
		for(E graphEdge : graphEdges){
			N neighbor = neighborFinder.apply(graphEdge);
			if(satisfiesRequirements(relevantEdge, graphEdge, neighbor)){
				neighborsSatisfyingRequirements.add(neighbor);
			}
		}

		forwardCheckingEvent.addNeighborsTotal(graphEdges.size());

		return neighborsSatisfyingRequirements;
	}

	private boolean satisfiesRequirements(RelevantEdge currentEdge, E graphEdge, N neighbor) {
		return graphEdge.hasEdgeType(currentEdge.edge.edgeType()) &&
				neighbor.hasNodeType(currentEdge.otherNode.nodeType()) &&
				checkAttributeRequirements(pattern.edgeRequirements().get(currentEdge.edge), graphEdge) &&
				checkAttributeRequirements(pattern.nodeRequirements().get(currentEdge.otherNode), neighbor);
	}

	private boolean checkAttributeRequirements(List<AttributeRequirement> requirements, AttributeAware graphElement) {
		if(requirements == null){
			return true;
		}
		for(AttributeRequirement attributeRequirement : requirements){
			if(!attributeRequirement.evaluate(graphElement)){
				return false;
			}
		}
		return true;
	}

	private List<RelevantEdge> getRelevantEdges(GPNode currentNode) {
		Collection<GPEdge> outgoingEdges = Objects.requireNonNullElse(pattern.graph().outgoingEdges().get(currentNode), Set.of());
		Collection<GPEdge> incomingEdges = Objects.requireNonNullElse(pattern.graph().incomingEdges().get(currentNode), Set.of());

		List<RelevantEdge> relevantEdges = new ArrayList<>();
		for(GPEdge edge : incomingEdges){
			relevantEdges.add(new RelevantEdge(edge, edge.source(), false));
		}
		for(GPEdge edge : outgoingEdges){
			relevantEdges.add(new RelevantEdge(edge, edge.target(), true));
		}
		return relevantEdges;
	}

	private record RelevantEdge(GPEdge edge, GPNode otherNode, boolean isOutgoing) {
	}

}
