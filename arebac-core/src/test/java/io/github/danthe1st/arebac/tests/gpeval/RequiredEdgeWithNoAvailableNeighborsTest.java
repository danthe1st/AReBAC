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
import io.github.danthe1st.arebac.data.memory.InMemoryGraphNode;
import io.github.danthe1st.arebac.gpeval.GPEval;
import org.junit.jupiter.api.Test;

class RequiredEdgeWithNoAvailableNeighborsTest {
	private static final String NODE_TYPE = "user";
	private static final String EDGE_TYPE = "friend";
	
	@Test
	void test() {
		GPNode node = new GPNode("test", NODE_TYPE);
		GPNode otherNode = new GPNode("nonexisting", NODE_TYPE);
		GraphPattern pattern = new GraphPattern(
				new GPGraph(
						List.of(node, otherNode),
						List.of(new GPEdge(node, otherNode, "", EDGE_TYPE))
				), List.of(),
				Map.of(node, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute("123")))),
				Map.of(), List.of(otherNode), Map.of()
		);
		
		InMemoryGraph graph = new InMemoryGraph(List.of(new InMemoryGraphNode("123", NODE_TYPE, Map.of())), List.of());
		
		Set<List<InMemoryGraphNode>> result = GPEval.evaluate(graph, pattern);
		assertEquals(Set.of(), result);
	}
}
