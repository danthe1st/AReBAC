package io.github.danthe1st.arebac.tests.gpeval;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement.ID_KEY;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator.EQUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.danthe1st.arebac.data.graph_pattern.GPEdge;
import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.data.memory.InMemoryGraph;
import io.github.danthe1st.arebac.data.memory.InMemoryGraphEdge;
import io.github.danthe1st.arebac.data.memory.InMemoryGraphNode;
import io.github.danthe1st.arebac.gpeval.GPEval;
import org.junit.jupiter.api.Test;

class RequiredSelfConnectionTest {
	
	private static final String EDGE_TYPE = "EDGE";
	private static final String NODE_TYPE = "NODE";
	
	private static final String NODE_ID = "someNodeId";
	
	@Test
	void testWithSelfConnectionInSingleNodeGraph() {
		InMemoryGraphNode node = new InMemoryGraphNode(NODE_ID, NODE_TYPE, Map.of());
		InMemoryGraph graph = new InMemoryGraph(
				List.of(node),
				List.of(new InMemoryGraphEdge(node, node, "someEdgeId", EDGE_TYPE, Map.of()))
		);
		Set<List<InMemoryGraphNode>> result = GPEval.evaluate(graph, createSingleNodeGraphPattern());
		assertEquals(Set.of(List.of(node)), result);
	}
	
	@Test
	void testWithoutSelfConnectionInSingleNodeGraph() {
		InMemoryGraphNode node = new InMemoryGraphNode(NODE_ID, NODE_TYPE, Map.of());
		InMemoryGraph graph = new InMemoryGraph(
				List.of(node),
				List.of()
		);
		Set<List<InMemoryGraphNode>> result = GPEval.evaluate(graph, createSingleNodeGraphPattern());
		assertEquals(Set.of(), result);
	}
	
	private GraphPattern createSingleNodeGraphPattern() {
		GPNode node = new GPNode("someId", NODE_TYPE);
		return new GraphPattern(
				new GPGraph(
						List.of(node),
						List.of(new GPEdge(node, node, null, EDGE_TYPE))
				),
				List.of(), Map.of(
						node, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute(NODE_ID)))
				), Map.of(),
				List.of(node), Map.of()
		);
	}
	
	@Test
	void testWithSelfConnectionInMultiNodeGraph() {
		InMemoryGraphNode fixedNode = new InMemoryGraphNode(NODE_ID, NODE_TYPE, Map.of());
		InMemoryGraphNode selfConnectedNode = new InMemoryGraphNode("selfConnectNode", NODE_TYPE, Map.of());
		InMemoryGraph graph = new InMemoryGraph(
				List.of(fixedNode, selfConnectedNode),
				List.of(
						new InMemoryGraphEdge(fixedNode, selfConnectedNode, "someEdgeId", EDGE_TYPE, Map.of()),
						new InMemoryGraphEdge(selfConnectedNode, selfConnectedNode, "selfConnectEdge", EDGE_TYPE, Map.of())
				)
		);
		Set<List<InMemoryGraphNode>> result = GPEval.evaluate(graph, createMultipleNodesGraphPattern());
		assertEquals(Set.of(List.of(selfConnectedNode)), result);
	}
	
	@Test
	void testWithoutSelfConnectionInMultiNodeGraph() {
		InMemoryGraphNode fixedNode = new InMemoryGraphNode(NODE_ID, NODE_TYPE, Map.of());
		InMemoryGraphNode selfConnectedNode = new InMemoryGraphNode("selfConnectNode", NODE_TYPE, Map.of());
		InMemoryGraph graph = new InMemoryGraph(
				List.of(fixedNode, selfConnectedNode),
				List.of(
						new InMemoryGraphEdge(fixedNode, selfConnectedNode, "someEdgeId", EDGE_TYPE, Map.of())
				)
		);
		Set<List<InMemoryGraphNode>> result = GPEval.evaluate(graph, createMultipleNodesGraphPattern());
		assertEquals(Set.of(), result);
	}
	
	private GraphPattern createMultipleNodesGraphPattern() {
		GPNode fixedNode = new GPNode("someId", NODE_TYPE);
		GPNode selfConnectedNode = new GPNode("otherId", NODE_TYPE);
		return new GraphPattern(
				new GPGraph(
						List.of(fixedNode, selfConnectedNode),
						List.of(
								new GPEdge(fixedNode, selfConnectedNode, null, EDGE_TYPE),
								new GPEdge(selfConnectedNode, selfConnectedNode, null, EDGE_TYPE)
						)
				),
				List.of(), Map.of(
						fixedNode, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute(NODE_ID)))
				), Map.of(),
				List.of(selfConnectedNode), Map.of()
		);
	}
}
