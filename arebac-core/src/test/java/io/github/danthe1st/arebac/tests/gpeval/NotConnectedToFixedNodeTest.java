package io.github.danthe1st.arebac.tests.gpeval;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement.ID_KEY;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator.EQUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.data.memory.InMemoryGraph;
import io.github.danthe1st.arebac.data.memory.InMemoryGraphNode;
import io.github.danthe1st.arebac.gpeval.GPEval;
import org.junit.jupiter.api.Test;

class NotConnectedToFixedNodeTest {
	@Test
	void testGPGraphWithoutFixedNode() {
		GraphPattern pattern = createGraphPatternWithSingleNonFixedNode();
		InMemoryGraph targetGraph = new InMemoryGraph(List.of(new InMemoryGraphNode("a", "NODE", Map.of())), List.of());
		
		assertThrows(IllegalStateException.class, () -> GPEval.evaluate(targetGraph, pattern), "No candidate node found. Make sure all nodes in the graph pattern are have some connection to a fixed node.");
	}
	
	@Test
	void testGPGraphWithoutFixedNodeOnEmptyTargetGraph() {
		GraphPattern pattern = createGraphPatternWithSingleNonFixedNode();
		InMemoryGraph targetGraph = new InMemoryGraph(List.of(), List.of());
		
		assertThrows(IllegalStateException.class, () -> GPEval.evaluate(targetGraph, pattern), "No candidate node found. Make sure all nodes in the graph pattern are have some connection to a fixed node.");
	}

	private GraphPattern createGraphPatternWithSingleNonFixedNode() {
		GPNode node = new GPNode("nId", "NODE");
		GPGraph graph = new GPGraph(List.of(node), List.of());
		return new GraphPattern(graph, List.of(), Map.of(), Map.of(), List.of(node), Map.of());
	}
	
	@Test
	void testGPGraphWithoutConnectionToFixedNode() {
		GraphPattern pattern = createGraphPatternWithFixedNodeAndUnconnectedNode();
		InMemoryGraph targetGraph = new InMemoryGraph(List.of(), List.of());
		
		// if a fixed node cannot be found, the result is the empty set
		Set<List<InMemoryGraphNode>> result = GPEval.evaluate(targetGraph, pattern);
		assertEquals(Set.of(), result);
	}
	
	@Test
	void testGPGraphWithoutConnectionToFixedNodeNoNodesInGraph() {
		GraphPattern pattern = createGraphPatternWithFixedNodeAndUnconnectedNode();
		InMemoryGraph targetGraph = new InMemoryGraph(List.of(new InMemoryGraphNode("FIXED_NODE", "NODE", Map.of()), new InMemoryGraphNode("otherNode", "NODE", Map.of())), List.of());
		
		assertThrows(IllegalStateException.class, () -> GPEval.evaluate(targetGraph, pattern), "No candidate node found. Make sure all nodes in the graph pattern are have some connection to a fixed node.");
	}
	
	private GraphPattern createGraphPatternWithFixedNodeAndUnconnectedNode() {
		GPNode fixedNode = new GPNode("nId", "NODE");
		GPNode unconnectedNode = new GPNode("otherId", "NODE");
		GPGraph graph = new GPGraph(List.of(fixedNode, unconnectedNode), List.of());
		return new GraphPattern(
				graph, List.of(), Map.of(
						fixedNode, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute("FIXED_NODE")))
				), Map.of(), List.of(unconnectedNode), Map.of()
		);
	}
}
