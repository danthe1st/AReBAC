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
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedEdge;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedGraph;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedNode;
import io.github.danthe1st.arebac.data.graph_pattern.GPEdge;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.MutualExclusionConstraint;
import io.github.danthe1st.arebac.gpeval.events.FilterMutualExclusionConstraintsEvent;
import io.github.danthe1st.arebac.gpeval.events.ForwardCheckingEvent;
import io.github.danthe1st.arebac.gpeval.events.IntersectionEvent;

/**
 * Implementation of the GP-eval algorithm.
 *
 * This algorithm finds all assignments in a {@link AttributedGraph graph} that match a specified {@link GraphPattern}
 *
 * @param <N> The type of nodes in the graph
 * @param <E> The type of edges in the graph
 */
public final class GPEval<N extends AttributedNode, E extends AttributedEdge<N>> {

	private final AttributedGraph<N, E> graph;
	private final GraphPattern pattern;

	private final Map<GPNode, Set<GPNode>> mutualExclusionConstraints;

	// node in pattern -> list of nodes in graph
	private Map<GPNode, List<N>> candidates = new HashMap<>();

	// node in pattern -> node in graph
	private Map<GPNode, N> assignments = new HashMap<>();

	private Set<List<N>> results = new HashSet<>();

	public static <N extends AttributedNode, E extends AttributedEdge<N>> Set<List<N>> evaluate(AttributedGraph<N, E> graph, GraphPattern pattern) {
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

	/**
	 * Constructor used for recursive calls with different data
	 * @param graph the attributed graph to match against, shared between recursive calls
	 * @param pattern the graph pattern to match, shared between recursive calls
	 * @param mutualExclusionConstraints the mutual exclusion constraints, shared between recursive calls
	 * @param candidates candidate nodes in the attributed graph for each vertex in the pattern where candidates have been discovered, copied in recursive calls
	 * @param assignments vertices assigned so far, copied in recursive calls
	 * @param results a {@link Set} storing the results of each pattern, shared between recursive calls
	 */
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

	/**
	 * Finds vertices that are known to only have one possible assignment and stores these in {@link GPEval#candidates}
	 * @throws NoResultException if the vertex attribute requirements fail on these vertices indicating there can be no results
	 */
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

	/**
	 * Checks vertex attribute requirements on fixed vertices - this assumes that all entries in {@link GPEval#candidates} only have one corresponding node in the attributed graph
	 * @throws NoResultException if any vertex attribute requirement cannot be satisfied for a fixed vertex
	 */
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

	/**
	 * Checks vertex attribute requirements for a specified vertex
	 * @param patternNode The vertex in the graph pattern
	 * @param graphNode The node in the attributed graph the vertex may correspond to
	 * @return {@code true} if the attribute requirements match, else {@code false}
	 */
	private boolean checkRequirementsForNode(GPNode patternNode, N graphNode) {
		for(AttributeRequirement requirement : pattern.nodeRequirements().get(patternNode)){
			if(!requirement.evaluate(graphNode)){
				return false;
			}
		}
		return checkSelfConnectionRequirements(patternNode, graphNode);
	}

	/**
	 * Runs the recursive phase of the GP-Eval algorithm
	 * @param incomingConflicts a mapping storing information on which vertex could result in conflicts with which other nodes
	 * @return a {@link Set} of vertices when backjumping should stop
	 */
	private Set<GPNode> run(Map<GPNode, List<GPNode>> incomingConflicts) {// returns nodes for backjumping
		if(assignments.size() == pattern.graph().nodes().size()){

			List<N> result = new ArrayList<>();
			for(GPNode nodeToReturn : pattern.returnedNodes()){
				result.add(Objects.requireNonNull(assignments.get(nodeToReturn)));
			}

			results.add(List.copyOf(result));
			// backjump until any of the returned nodes are reassigned
			// there is no point in looking at assignments leading to the same returned nodes
			return Set.copyOf(pattern.returnedNodes());
		}

		boolean deadEnd = true;
		// all vertices where conflicts happened, used for backjumping
		// if a conflict happened on a vertex and that vertex is reassigned, backjumping should stop/the search should continue
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
				Set<GPNode> jump = child.run(newIncomingConflicts);// the paper uses incomingConflicts (confIn) here but that's just a missing single quote
				// backjumping:
				// if the currently assigned node is within the returned vertices, continue checking
				if(!jump.isEmpty() && !jump.contains(currentNode)){
					return jump;
				}
				conflicts.addAll(jump);
			}
		}

		// in case of a dead end (no candidate resulted in forward checking to succeed)
		// perform backjumping with incoming conflicts related to current vertex and all incoming conflicts related to nodes listed in outgoingConflicts
		if(deadEnd){
			addAllIncomingConflictsForNode(incomingConflicts, conflicts, currentNode);
			for(GPNode node : outgoingConflicts){
				addAllIncomingConflictsForNode(incomingConflicts, conflicts, node);
			}
			return conflicts;
		}
		// forward checking successful / no dead end
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

	/**
	 * create a deep copy of a {@code Map<K, List<V>>} but don't include the specified key in the copy
	 * @param <K> the key type
	 * @param <V> the value element type
	 * @param multimap the {@link Map} to copy
	 * @param keyToSkip the key to not copy
	 * @return the copy of the map, not including the entry identified by {@code keyToSkip}
	 */
	private <K, V> Map<K, List<V>> deepCopyExceptKey(Map<K, List<V>> multimap, K keyToSkip) {
		Map<K, List<V>> result = HashMap.newHashMap(multimap.size());
		for(Entry<K, List<V>> entry : multimap.entrySet()){
			if(!entry.getKey().equals(keyToSkip)){
				result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
			}
		}
		return result;
	}

	/**
	 * create a deep copy of a {@code Map<K, List<V>>}
	 * @param <K> the key type
	 * @param <V> the value element type
	 * @param multimap the {@link Map} to copy
	 * @return the copy of the map
	 */
	private <K, V> Map<K, List<V>> deepCopy(Map<K, List<V>> multimap) {
		Map<K, List<V>> result = HashMap.newHashMap(multimap.size());
		for(Entry<K, List<V>> entry : multimap.entrySet()){
			result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		return result;
	}

	/**
	 * Add all incoming conflicts associated with a specified node to the {@link Set} of conflicts
	 * @param incomingConflicts all incoming conflicts for any node
	 * @param conflicts the {@link Set} of conflicts to modify
	 * @param currentNode the node relevant for incoming conflicts
	 */
	private void addAllIncomingConflictsForNode(Map<GPNode, List<GPNode>> incomingConflicts, Set<GPNode> conflicts, GPNode currentNode) {
		List<GPNode> nodeIncomingConflicts = incomingConflicts.get(currentNode);
		if(nodeIncomingConflicts != null){
			conflicts.addAll(nodeIncomingConflicts);
		}
	}

	/**
	 * Pick the next node to assign
	 * @return a {@link GPNode} to assign
	 */
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
		if (candidate == null) {
			throw new IllegalStateException("No candidate node found. Make sure all nodes in the graph pattern are have some connection to a fixed node.");
		}
		return candidate;
	}

	/**
	 * Removes candidates violating a mutual exclusion constraint with already assigned nodes
	 * @param candidatesForNode The candidates to filter
	 * @param exclusionConstraints all vertices that must have different assignments than the candidates
	 * @param incomingConflicts vertices violating mutual exclusion constraints are added to this collection
	 */
	private void filterMutualExclusionConstraints(List<N> candidatesForNode, Set<GPNode> exclusionConstraints, List<GPNode> incomingConflicts) {
		FilterMutualExclusionConstraintsEvent event = new FilterMutualExclusionConstraintsEvent();
		event.begin();
		for(Iterator<N> it = candidatesForNode.iterator(); it.hasNext();){
			filterMutualExclusionConstraintWithSpecificCandidate(exclusionConstraints, incomingConflicts, it);
		}
		event.commit();
	}

	/**
	 * Removes a specific candidate
	 * @param exclusionConstraints the mutual exclusion constraints applying to the candidate
	 * @param incomingConflicts if the vertex violates a mutual exclusion constraint, the other vertex is added as an incoming conflict
	 * @param it used for obtaining the node and removing it if it matches a mutual exclusion constraint
	 */
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

	/**
	 * Runs the forward checking part of GP-Eval
	 * This checks all expected incoming and outgoing edges of a specified vertex as follows:
	 * <ul>
	 *   <li>If the other node is not assigned yet, ignore it</li>
	 *   <li>Else, check whether specified in the graph pattern should be satisfied with the specified neighbor</li>
	 *   <li>This also discovers further neighbors and adds them.</li>
	 * </ul>
	 * @param currentNode The node to check
	 * @param incomingConflicts potential conflicts discovered in forward checking are added as incoming conflicts
	 * @param outgoingConflicts if a vertex cannot be assigned (no candidates found), that is added to outgoing conflicts
	 * @return {@code true} if forward checking succeeds, else {@code false}
	 */
	private boolean forwardChecking(GPNode currentNode, Map<GPNode, List<GPNode>> incomingConflicts, Set<GPNode> outgoingConflicts) {
		ForwardCheckingEvent forwardCheckingEvent = new ForwardCheckingEvent();
		forwardCheckingEvent.begin();

		List<RelevantEdge> relevantEdges = getRelevantEdges(currentNode);
		forwardCheckingEvent.setRelevantEdges(relevantEdges.size());
		for(RelevantEdge relevantEdge : relevantEdges){
			GPNode otherNode = relevantEdge.otherNode();
			if(!assignments.containsKey(otherNode)){
				forwardCheckingEvent.addUnknownEdge();
				List<N> neighbors = getNeighborsSatisfyingEdgeAndAttributeRequirements(currentNode, relevantEdge, forwardCheckingEvent);
				forwardCheckingEvent.addNeighborsProcessed(neighbors.size());
				List<N> otherNodeCandidates = candidates.get(otherNode);
				assert otherNodeCandidates == null || !otherNodeCandidates.isEmpty();// this should normally not happen, null is written as empty in the paper
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
					// intersect otherNodeCandidates with neighbors
					IntersectionEvent event = new IntersectionEvent();
					event.setNeighborsCount(neighbors.size());
					event.setCandidatesCountBefore(otherNodeCandidates.size());
					event.begin();

					neighbors.retainAll(otherNodeCandidates);
					otherNodeCandidates = neighbors;
					candidates.put(otherNode, otherNodeCandidates);

					event.setCandidateCountAfter(otherNodeCandidates.size());
					event.commit();
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

	/**
	 * Gets all neighbors of a specific node along a specific edge
	 * @param currentNode the current node
	 * @param relevantEdge the edge of the current node to the other node
	 * @param forwardCheckingEvent used for diagnosis, not necessary for GP-Eval
	 * @return the neighbors satisfying the requirements
	 */
	private List<N> getNeighborsSatisfyingEdgeAndAttributeRequirements(GPNode currentNode, RelevantEdge relevantEdge, ForwardCheckingEvent forwardCheckingEvent) {
		N currentNodeInDB = assignments.get(currentNode);
		Collection<E> graphEdges;
		Function<E, N> neighborFinder;
		if(relevantEdge.isOutgoing()){
			// TODO make this more efficient by finding the exact relevant edges
			graphEdges = graph.findOutgoingEdges(currentNodeInDB, relevantEdge.edgeType());
			neighborFinder = AttributedEdge::target;
		}else{
			graphEdges = graph.findIncomingEdges(currentNodeInDB, relevantEdge.edgeType());
			neighborFinder = AttributedEdge::source;
		}
		graphEdges = Objects.requireNonNullElse(graphEdges, List.of());
		List<N> neighborsSatisfyingRequirements = new ArrayList<>();
		for(E graphEdge : graphEdges){
			N neighbor = neighborFinder.apply(graphEdge);
			if(satisfiesRequirements(relevantEdge, graphEdge, neighbor)){
				neighborsSatisfyingRequirements.add(neighbor);
			}
		}

		forwardCheckingEvent.addNeighborsTotal(graphEdges.size());

		return neighborsSatisfyingRequirements;
	}

	/**
	 * Checks whether an edge with a specific neighbor satisfies the specified labels/requirements
	 * @param currentEdge The graph pattern edge to check
	 * @param graphEdge The edge in the attributed graph to check against
	 * @param neighbor The other graph node in the edge
	 * @return {@code true} if the requirements are met, else {@code false}
	 */
	private boolean satisfiesRequirements(RelevantEdge currentEdge, E graphEdge, N neighbor) {
		return graphEdge.hasEdgeType(currentEdge.edge.edgeType()) &&
				neighbor.hasNodeType(currentEdge.otherNode.nodeType()) &&
				checkAttributeRequirements(pattern.edgeRequirements().get(currentEdge.edge), graphEdge) &&
				checkAttributeRequirements(pattern.nodeRequirements().get(currentEdge.otherNode), neighbor) &&
				checkSelfConnectionRequirements(currentEdge.otherNode(), neighbor);
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

	/**
	 * Checks whether all edges of a node in the graph pattern to itself are satisfied
	 * These edges are not otherwise checked in the forward-checking because it only considers nodes with unknown assignments.
	 * @param patternNode The node in the graph pattern that might have self-connections
	 * @param graphNode The corresponding node in the attributed graph to check against
	 * @return {@code false} if any violations have been found, else {@code true}
	 */
	private boolean checkSelfConnectionRequirements(GPNode patternNode, N graphNode) {
		for(GPEdge edge : pattern.graph().outgoingEdges().getOrDefault(patternNode, List.of())){
			if(edge.target().equals(patternNode)){
				List<AttributeRequirement> requirements = pattern.edgeRequirements().get(edge);
				if(!isSelfEdgeSatisfied(graphNode, edge, requirements)){
					return false;
				}
			}
		}
		return true;
	}

	private boolean isSelfEdgeSatisfied(N graphNode, GPEdge edge, List<AttributeRequirement> requirements) {
		for(E graphSelfEdge : graph.findOutgoingEdges(graphNode, edge.edgeType())){
			if(graphSelfEdge.target().equals(graphNode) && checkAttributeRequirements(requirements, graphSelfEdge)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets all edges of a specified node in the graph pattern
	 * @param currentNode the node
	 * @return all edges related to the given node
	 */
	private List<RelevantEdge> getRelevantEdges(GPNode currentNode) {
		Collection<GPEdge> outgoingEdges = Objects.requireNonNullElse(pattern.graph().outgoingEdges().get(currentNode), Set.of());
		Collection<GPEdge> incomingEdges = Objects.requireNonNullElse(pattern.graph().incomingEdges().get(currentNode), Set.of());

		List<RelevantEdge> relevantEdges = new ArrayList<>();
		for(GPEdge edge : incomingEdges){
			relevantEdges.add(new RelevantEdge(edge, edge.source(), edge.edgeType(), false));
		}
		for(GPEdge edge : outgoingEdges){
			relevantEdges.add(new RelevantEdge(edge, edge.target(), edge.edgeType(), true));
		}
		return relevantEdges;
	}

	private record RelevantEdge(GPEdge edge, GPNode otherNode, String edgeType, boolean isOutgoing) {
	}

}
