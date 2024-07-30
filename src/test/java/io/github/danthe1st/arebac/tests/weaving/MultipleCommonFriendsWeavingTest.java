package io.github.danthe1st.arebac.tests.weaving;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement.ID_KEY;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator.EQUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import io.github.danthe1st.arebac.data.graph_pattern.GPEdge;
import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.MutualExclusionConstraint;
import io.github.danthe1st.arebac.weaving.Weaving;
import org.junit.jupiter.api.Test;

//see also example 18 of https://doi.org/10.1145/3401027
class MultipleCommonFriendsWeavingTest {
	private static final String FRIEND_EDGE_TYPE = "friend";
	private static final String USER_NODE_TYPE = "user";

	@Test
	void testCombineFriendsOfFriendsWithNotSelf() {
		GraphPattern combined = Weaving.combinePatterns(List.of(createCommonFriendsPattern(), createRequestorNotTargetPattern()));
		assertEquals(createExpectedCombinedFriendsOfFriendsButNotSelfPattern(), combined);
	}
	
	private GraphPattern createExpectedCombinedFriendsOfFriendsButNotSelfPattern() {
		GPNode requestor = new GPNode("p0,requestor", USER_NODE_TYPE);
		GPNode target = new GPNode("p0,target", USER_NODE_TYPE);
		GPNode firstFriend = new GPNode("p0,f1", USER_NODE_TYPE);
		GPNode secondFriend = new GPNode("p0,f2", USER_NODE_TYPE);
		
		return new GraphPattern(
				new GPGraph(
						List.of(requestor, target, firstFriend, secondFriend),
						List.of(
								new GPEdge(requestor, firstFriend, "p0,req->f1", FRIEND_EDGE_TYPE),
								new GPEdge(requestor, secondFriend, "p0,req->f2", FRIEND_EDGE_TYPE),
								new GPEdge(firstFriend, target, "p0,f1->target", FRIEND_EDGE_TYPE),
								new GPEdge(secondFriend, target, "p0,f2->target", FRIEND_EDGE_TYPE)
						)
				),
				List.of(
						new MutualExclusionConstraint(firstFriend, secondFriend),
						new MutualExclusionConstraint(requestor, target)
				),
				Map.of(),
				Map.of(),
				List.of(target),
				Map.of("requestor", requestor, "target", target)
		);
	}
	
	private GraphPattern createRequestorNotTargetPattern() {
		GPNode requestor = new GPNode("requestor", USER_NODE_TYPE);
		GPNode target = new GPNode("target", USER_NODE_TYPE);

		return new GraphPattern(
				new GPGraph(List.of(requestor, target), List.of()),
				List.of(new MutualExclusionConstraint(requestor, target)),
				Map.of(),
				Map.of(),
				List.of(),
				Map.of("requestor", requestor, "target", target)
		);
	}

	private GraphPattern createCommonFriendsPattern() {
		GPNode requestor = new GPNode("requestor", USER_NODE_TYPE);
		GPNode target = new GPNode("target", USER_NODE_TYPE);
		GPNode firstFriend = new GPNode("f1", USER_NODE_TYPE);
		GPNode secondFriend = new GPNode("f2", USER_NODE_TYPE);

		return new GraphPattern(
				new GPGraph(
						List.of(requestor, target, firstFriend, secondFriend),
						List.of(
								new GPEdge(requestor, firstFriend, "req->f1", FRIEND_EDGE_TYPE),
								new GPEdge(requestor, secondFriend, "req->f2", FRIEND_EDGE_TYPE),
								new GPEdge(firstFriend, target, "f1->target", FRIEND_EDGE_TYPE),
								new GPEdge(secondFriend, target, "f2->target", FRIEND_EDGE_TYPE)
						)
				), List.of(new MutualExclusionConstraint(firstFriend, secondFriend)),
				Map.of(),
				Map.of(),
				List.of(target),
				Map.of("requestor", requestor, "target", target)
		);
	}
	
	@Test
	void testAtLeast3MutualFriends() {
		GraphPattern combined = Weaving.combinePatterns(
				List.of(
						createSingleFriendOfFriendPattern("fA"),
						createSingleFriendOfFriendPattern("fB"),
						createSingleFriendOfFriendPattern("fC"),
						createMultipleFriendOfFriendExclusionConstraintsPattern("fA", "fB"),
						createMultipleFriendOfFriendExclusionConstraintsPattern("fA", "fC"),
						createMultipleFriendOfFriendExclusionConstraintsPattern("fB", "fC"),
						createRequestorTargetRequirementPattern()
				)
		);
		assertEquals(createExpected3MutualFriendsPattern(), combined);
	}
	
	private GraphPattern createSingleFriendOfFriendPattern(String actorName) {
		GPNode requestor = new GPNode("requestor", USER_NODE_TYPE);
		GPNode friend = new GPNode("f", USER_NODE_TYPE);
		GPNode friendOfFriend = new GPNode("fof", USER_NODE_TYPE);
		
		return new GraphPattern(
				new GPGraph(
						List.of(requestor, friend, friendOfFriend),
						List.of(
								new GPEdge(requestor, friend, "rf", FRIEND_EDGE_TYPE),
								new GPEdge(friend, friendOfFriend, "ft", FRIEND_EDGE_TYPE)
						)
				),
				List.of(),
				Map.of(),
				Map.of(),
				List.of(),
				Map.of("requestor", requestor, actorName, friend, "target", friendOfFriend)
		);
	}
	
	private GraphPattern createRequestorTargetRequirementPattern() {
		GPNode requestor = new GPNode("requestor", USER_NODE_TYPE);
		GPNode friendOfFriend = new GPNode("fof", USER_NODE_TYPE);
		
		return new GraphPattern(
				new GPGraph(
						List.of(requestor, friendOfFriend),
						List.of()
				),
				List.of(new MutualExclusionConstraint(requestor, friendOfFriend)),
				Map.of(requestor, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute("123")))),
				Map.of(),
				List.of(friendOfFriend),
				Map.of("requestor", requestor, "target", friendOfFriend)
		);
	}
	
	private GraphPattern createMultipleFriendOfFriendExclusionConstraintsPattern(String firstActorName, String secondActorName) {
		GPNode friendA = new GPNode("fA", USER_NODE_TYPE);
		GPNode friendB = new GPNode("fB", USER_NODE_TYPE);
		
		return new GraphPattern(
				new GPGraph(
						List.of(friendA, friendB),
						List.of()
				),
				List.of(new MutualExclusionConstraint(friendA, friendB)),
				Map.of(),
				Map.of(),
				List.of(),
				Map.of(firstActorName, friendA, secondActorName, friendB)
		);
	}
	
	private GraphPattern createExpected3MutualFriendsPattern() {
		GPNode requestor = new GPNode("p0,requestor", USER_NODE_TYPE);
		GPNode fA = new GPNode("p0,f", USER_NODE_TYPE);
		GPNode fB = new GPNode("p1,f", USER_NODE_TYPE);
		GPNode fC = new GPNode("p2,f", USER_NODE_TYPE);
		GPNode friendOfFriend = new GPNode("p0,fof", USER_NODE_TYPE);
		
		return new GraphPattern(
				new GPGraph(
						List.of(requestor, fA, fB, fC, friendOfFriend),
						List.of(
								new GPEdge(requestor, fA, "p0,rf", FRIEND_EDGE_TYPE),
								new GPEdge(requestor, fB, "p1,rf", FRIEND_EDGE_TYPE),
								new GPEdge(requestor, fC, "p2,rf", FRIEND_EDGE_TYPE),
								new GPEdge(fA, friendOfFriend, "p0,ft", FRIEND_EDGE_TYPE),
								new GPEdge(fB, friendOfFriend, "p1,ft", FRIEND_EDGE_TYPE),
								new GPEdge(fC, friendOfFriend, "p2,ft", FRIEND_EDGE_TYPE)
						)
				),
				List.of(
						new MutualExclusionConstraint(fA, fB),
						new MutualExclusionConstraint(fA, fC),
						new MutualExclusionConstraint(fB, fC),
						new MutualExclusionConstraint(requestor, friendOfFriend)
				),
				Map.of(requestor, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute("123")))),
				Map.of(),
				List.of(friendOfFriend),
				Map.of("requestor", requestor, "fA", fA, "fB", fB, "fC", fC, "target", friendOfFriend)
		);
	}
}
