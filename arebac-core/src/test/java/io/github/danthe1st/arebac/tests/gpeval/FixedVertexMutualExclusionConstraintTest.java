package io.github.danthe1st.arebac.tests.gpeval;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement.ID_KEY;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator.EQUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.MutualExclusionConstraint;
import io.github.danthe1st.arebac.data.memory.InMemoryGraph;
import io.github.danthe1st.arebac.data.memory.InMemoryGraphNode;
import io.github.danthe1st.arebac.gpeval.GPEval;
import org.junit.jupiter.api.Test;

class FixedVertexMutualExclusionConstraintTest {
	@Test
	void testSelfMutualExclusionConstraintWithFixedVertices() {
		GPNode v1 = new GPNode("a", "NODE");
		GPNode v2 = new GPNode("b", "NODE");
		GPGraph graph = new GPGraph(List.of(v1, v2), List.of());
		GraphPattern pattern = new GraphPattern(
				graph,
				List.of(new MutualExclusionConstraint(v1, v2)),
				Map.of(
						v1, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute("NID"))),
						v2, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute("NID")))
				),
				Map.of(),
				List.of(v1, v2), Map.of()
		);
		InMemoryGraphNode node = new InMemoryGraphNode("NID", "NODE", Map.of());
		InMemoryGraph dataStore = new InMemoryGraph(List.of(node), List.of());
		
		Set<List<InMemoryGraphNode>> result = GPEval.evaluate(dataStore, pattern);
		assertEquals(Set.of(), result);
	}
}
