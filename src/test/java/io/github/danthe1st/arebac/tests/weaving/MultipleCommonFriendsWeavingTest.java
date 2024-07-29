package io.github.danthe1st.arebac.tests.weaving;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import io.github.danthe1st.arebac.data.graph_pattern.GPEdge;
import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.MutualExclusionConstraint;
import io.github.danthe1st.arebac.weaving.Weaving;
import org.junit.jupiter.api.Test;

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
}
