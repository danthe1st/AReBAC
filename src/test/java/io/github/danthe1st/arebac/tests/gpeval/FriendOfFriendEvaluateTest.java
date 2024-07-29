package io.github.danthe1st.arebac.tests.gpeval;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement.ID_KEY;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator.EQUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.danthe1st.arebac.data.graph_pattern.GPEdge;
import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.MutualExclusionConstraint;
import io.github.danthe1st.arebac.data.memory.InMemoryGraph;
import io.github.danthe1st.arebac.data.memory.InMemoryGraphEdge;
import io.github.danthe1st.arebac.data.memory.InMemoryGraphNode;
import io.github.danthe1st.arebac.gpeval.GPEval;
import org.junit.jupiter.api.Test;

class FriendOfFriendEvaluateTest {

	private static final String FRIEND_EDGE_TYPE = "friend";
	private static final String USER_NODE_TYPE = "user";
	private static final String GOOD_FRIEND_EDGE_ATTRIBUTE = "goodFriend";

	private InMemoryGraphNode outsider = new InMemoryGraphNode("out", USER_NODE_TYPE, Map.of());
	private InMemoryGraphNode connector = new InMemoryGraphNode("con", USER_NODE_TYPE, Map.of());
	private InMemoryGraphNode connectorFriend = new InMemoryGraphNode("conFriend", USER_NODE_TYPE, Map.of());
	private InMemoryGraphNode triangleCompletor = new InMemoryGraphNode("completor", USER_NODE_TYPE, Map.of());
	private InMemoryGraph graph;

	/**
	 * <pre>
	 * outsider
	 * |
	 * connector -- triangleCompletor
	 * |               ^
	 * |              / (unidirectional)
	 * connectorFriend
	 * </pre>
	 */
	public FriendOfFriendEvaluateTest() {
		// outsider and connector are both friends
		// outsider considers connector to be a good friend but not vice-versa
		InMemoryGraphEdge outsiderEdge = new InMemoryGraphEdge(
				outsider, connector, "out->con", FRIEND_EDGE_TYPE,
				Map.of(GOOD_FRIEND_EDGE_ATTRIBUTE, attribute(true))
		);
		InMemoryGraphEdge outsiderBackEdge = new InMemoryGraphEdge(
				connector, outsider, "con->out", FRIEND_EDGE_TYPE,
				Map.of(GOOD_FRIEND_EDGE_ATTRIBUTE, attribute(false))
		);

		// connector and connectorFriend are (mutually) good friends
		InMemoryGraphEdge connectorFriendEdge = new InMemoryGraphEdge(
				connector, connectorFriend, "con->conFriend", FRIEND_EDGE_TYPE,
				Map.of(GOOD_FRIEND_EDGE_ATTRIBUTE, attribute(true))
		);
		InMemoryGraphEdge connectorFriendBackEdge = new InMemoryGraphEdge(
				connectorFriend, connector, "conFriend->con", FRIEND_EDGE_TYPE,
				Map.of(GOOD_FRIEND_EDGE_ATTRIBUTE, attribute(true))
		);

		// completor considers connector to be good friend but not vice-versa
		InMemoryGraphEdge connectorCompletorEdge = new InMemoryGraphEdge(
				connector, triangleCompletor, "con->completor", FRIEND_EDGE_TYPE,
				Map.of(GOOD_FRIEND_EDGE_ATTRIBUTE, attribute(false))
		);
		InMemoryGraphEdge connectorCompletorBackEdge = new InMemoryGraphEdge(
				triangleCompletor, connector, "completor->con", FRIEND_EDGE_TYPE,
				Map.of(GOOD_FRIEND_EDGE_ATTRIBUTE, attribute(true))
		);

		InMemoryGraphEdge connectorFriendToTriangleEdge = new InMemoryGraphEdge(
				connectorFriend, triangleCompletor, "con->completor", FRIEND_EDGE_TYPE,
				Map.of(GOOD_FRIEND_EDGE_ATTRIBUTE, attribute(false))
		);
		graph = new InMemoryGraph(
				List.of(outsider, connector, connectorFriend, triangleCompletor),
				List.of(
						outsiderEdge, outsiderBackEdge,
						connectorFriendEdge, connectorFriendBackEdge,
						connectorCompletorEdge, connectorCompletorBackEdge,
						connectorFriendToTriangleEdge
				)
		);
	}

	@Test
	void evaluateOutsider() {
		GraphPattern pattern = createFriendOfFriendPattern(outsider.id());
		Set<List<InMemoryGraphNode>> result = GPEval.evaluate(graph, pattern);
		assertEquals(Set.of(List.of(triangleCompletor), List.of(connectorFriend)), result);
	}

	@Test
	void evaluateGoodOutsiderFriends() {
		GraphPattern pattern = createGoodFriendOfFriendRequirement(outsider.id());
		Set<List<InMemoryGraphNode>> result = GPEval.evaluate(graph, pattern);
		assertEquals(Set.of(List.of(connectorFriend)), result);
	}
	
	@Test
	void evaluateConnector() {
		GraphPattern pattern = createFriendOfFriendPattern(connector.id());
		Set<List<InMemoryGraphNode>> result = GPEval.evaluate(graph, pattern);
		assertEquals(Set.of(List.of(triangleCompletor)), result);
	}

	@Test
	void evaluateGoodConnectorFriends() {
		GraphPattern pattern = createGoodFriendOfFriendRequirement(connector.id());
		Set<List<InMemoryGraphNode>> result = GPEval.evaluate(graph, pattern);
		assertEquals(Set.of(), result);
	}
	
	@Test
	void evaluateCompletor() {
		GraphPattern pattern = createFriendOfFriendPattern(triangleCompletor.id());
		Set<List<InMemoryGraphNode>> result = GPEval.evaluate(graph, pattern);
		assertEquals(Set.of(List.of(outsider), List.of(connectorFriend)), result);
	}

	@Test
	void evaluateGoodCompletorFriends() {
		GraphPattern pattern = createGoodFriendOfFriendRequirement(triangleCompletor.id());
		Set<List<InMemoryGraphNode>> result = GPEval.evaluate(graph, pattern);
		assertEquals(Set.of(List.of(connectorFriend)), result);
	}
	
	// adaptation from example 17 of https://doi.org/10.1145/3401027
	private GraphPattern createFriendOfFriendPattern(String requestorId) {
		GPNode requestor = new GPNode("requestor", USER_NODE_TYPE);
		GPNode friend = new GPNode("f", USER_NODE_TYPE);
		GPNode friendOfFriend = new GPNode("fof", USER_NODE_TYPE);

		return new GraphPattern(
				new GPGraph(
						List.of(requestor, friend, friendOfFriend),
						List.of(
								new GPEdge(requestor, friend, null, FRIEND_EDGE_TYPE),
								new GPEdge(friend, friendOfFriend, null, FRIEND_EDGE_TYPE)
						)
				),
				List.of(new MutualExclusionConstraint(requestor, friendOfFriend)),
				Map.of(requestor, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute(requestorId)))),
				Map.of(),
				List.of(friendOfFriend),
				Map.of("requestor", requestor, FRIEND_EDGE_TYPE, friend, "friendOfFriend", friendOfFriend)
		);
	}
	
	private GraphPattern createGoodFriendOfFriendRequirement(String requestorId) {
		GraphPattern friendOfFriendPattern = createFriendOfFriendPattern(requestorId);
		Map<GPEdge, List<AttributeRequirement>> newEdgeRequirements =
				friendOfFriendPattern
				.graph()
				.outgoingEdges()
				.values()
				.stream()
				.flatMap(List::stream)
					.collect(Collectors.toMap(e -> e, e -> List.of(new AttributeRequirement(GOOD_FRIEND_EDGE_ATTRIBUTE, EQUAL, attribute(true)))));
		return new GraphPattern(friendOfFriendPattern.graph(), friendOfFriendPattern.mutualExclusionConstraints(), friendOfFriendPattern.nodeRequirements(), newEdgeRequirements, friendOfFriendPattern.returnedNodes(), friendOfFriendPattern.actorsToNodes());
	}
}
