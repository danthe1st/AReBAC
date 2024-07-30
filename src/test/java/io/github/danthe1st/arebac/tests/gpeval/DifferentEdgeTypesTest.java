package io.github.danthe1st.arebac.tests.gpeval;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue;
import io.github.danthe1st.arebac.data.graph_pattern.GPEdge;
import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator;
import io.github.danthe1st.arebac.data.memory.InMemoryGraph;
import io.github.danthe1st.arebac.data.memory.InMemoryGraphEdge;
import io.github.danthe1st.arebac.data.memory.InMemoryGraphNode;
import io.github.danthe1st.arebac.gpeval.GPEval;
import org.junit.jupiter.api.Test;

class DifferentEdgeTypesTest {
	private static final String REQUESTOR_ID = "UID";
	private static final String NODE_TYPE = "user";

	@Test
	void test() {
		InMemoryGraphNode requestorNode = new InMemoryGraphNode(REQUESTOR_ID, NODE_TYPE, Map.of());
		InMemoryGraphNode fakeNode = new InMemoryGraphNode("fake", NODE_TYPE, Map.of());
		InMemoryGraphNode fakeEndNode = new InMemoryGraphNode("fakeEnd", NODE_TYPE, Map.of("end", AttributeValue.attribute(false)));
		InMemoryGraphNode middleNode = new InMemoryGraphNode("mid", NODE_TYPE, Map.of());
		InMemoryGraphNode endNode = new InMemoryGraphNode("end", NODE_TYPE, Map.of("end", AttributeValue.attribute(true)));

		InMemoryGraph graph = new InMemoryGraph(
				List.of(requestorNode, fakeNode, fakeEndNode, middleNode, endNode),
				List.of(
						new InMemoryGraphEdge(requestorNode, fakeNode, "req->fake", "e1", Map.of()),
						new InMemoryGraphEdge(fakeNode, fakeEndNode, "fake->fakeEnd", "e2", Map.of()),
						new InMemoryGraphEdge(requestorNode, middleNode, "req->mid", "e1", Map.of()),
						new InMemoryGraphEdge(middleNode, endNode, "mid->end", "e2", Map.of())
				)
		);
		
		Set<List<InMemoryGraphNode>> result = GPEval.evaluate(graph, createPattern());
		assertEquals(Set.of(List.of(middleNode, endNode)), result);
	}
	
	private GraphPattern createPattern() {
		GPNode requestor = new GPNode("requestor", NODE_TYPE);
		GPNode someNode = new GPNode("someNode", NODE_TYPE);
		GPNode otherNode = new GPNode("otherNode", NODE_TYPE);

		return new GraphPattern(
				new GPGraph(List.of(requestor, someNode, otherNode),
						List.of(
								new GPEdge(requestor, someNode, null, "e1"),
								new GPEdge(someNode, otherNode, null, "e2")
						)
				),
				List.of(),
				Map.of(
						requestor, List.of(new AttributeRequirement(AttributeRequirement.ID_KEY, AttributeRequirementOperator.EQUAL, AttributeValue.attribute(REQUESTOR_ID))),
						otherNode, List.of(new AttributeRequirement("end", AttributeRequirementOperator.EQUAL, AttributeValue.attribute(true)))
				),
				Map.of(),
				List.of(someNode, otherNode), Map.of()
		);
	}
}
