package io.github.danthe1st.arebac.tests.weaving;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import io.github.danthe1st.arebac.data.graph_pattern.GPEdge;
import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.weaving.Weaving;
import org.junit.jupiter.api.Test;

public class SameEdgesInBothPatternsTest {
	
	private final String SAMPLE_NODE_TYPE = "NODE_TYPE";
	private final String SAMPLE_EDGE_TYPE = "EDGE_TYPE";
	
	@Test
	void testCombinePatternsWithDuplicateEdge() {
		GraphPattern combined = Weaving.combinePatterns(List.of(createFirstPattern(), createSecondPattern()));
		assertEquals(createExpectedCombinedPattern(), combined);
	}
	
	private GraphPattern createFirstPattern() {
		GPNode subject = new GPNode("S", SAMPLE_NODE_TYPE);
		GPNode resource = new GPNode("R", SAMPLE_NODE_TYPE);
		GPNode common = new GPNode("a", SAMPLE_NODE_TYPE);
		GPNode other = new GPNode("b", SAMPLE_NODE_TYPE);

		GPGraph graph = new GPGraph(
				List.of(subject, resource, common, other), List.of(
						new GPEdge(subject, common, "Sa", SAMPLE_EDGE_TYPE),
						new GPEdge(common, resource, "aR", SAMPLE_EDGE_TYPE),
						new GPEdge(subject, other, "Sb", SAMPLE_EDGE_TYPE),
						new GPEdge(other, resource, "bR", SAMPLE_EDGE_TYPE)
				)
		);
		return new GraphPattern(
				graph, List.of(), Map.of(), Map.of(), List.of(resource), Map.of(
						"subject", subject,
						"resource", resource,
						"common", common
				)
		);
	}
	
	private GraphPattern createSecondPattern() {
		GPNode subject = new GPNode("S", SAMPLE_NODE_TYPE);
		GPNode resource = new GPNode("R", SAMPLE_NODE_TYPE);
		GPNode common = new GPNode("d", SAMPLE_NODE_TYPE);
		GPNode other = new GPNode("c", SAMPLE_NODE_TYPE);
		
		GPGraph graph = new GPGraph(
				List.of(subject, resource, common, other), List.of(
						new GPEdge(subject, common, "Sd", SAMPLE_EDGE_TYPE),
						new GPEdge(common, resource, "dR", SAMPLE_EDGE_TYPE),
						new GPEdge(subject, other, "Sc", SAMPLE_EDGE_TYPE),
						new GPEdge(other, resource, "cR", SAMPLE_EDGE_TYPE),
						new GPEdge(other, common, "cd", SAMPLE_EDGE_TYPE)
				)
		);
		return new GraphPattern(
				graph, List.of(), Map.of(), Map.of(), List.of(), Map.of(
						"subject", subject,
						"resource", resource,
						"common", common
				)
		);
	}
	
	private GraphPattern createExpectedCombinedPattern() {
		GPNode subject = new GPNode("p0,S", SAMPLE_NODE_TYPE);
		GPNode resource = new GPNode("p0,R", SAMPLE_NODE_TYPE);
		GPNode common = new GPNode("p0,a", SAMPLE_NODE_TYPE);
		GPNode firstPatternIntermediate = new GPNode("p0,b", SAMPLE_NODE_TYPE);
		GPNode secondPatternIntermediate = new GPNode("p1,c", SAMPLE_NODE_TYPE);
		
		GPGraph graph = new GPGraph(
				List.of(subject, resource, common, firstPatternIntermediate, secondPatternIntermediate), List.of(
						new GPEdge(subject, common, "p0,Sa", SAMPLE_EDGE_TYPE),
						new GPEdge(common, resource, "p0,aR", SAMPLE_EDGE_TYPE),
						new GPEdge(subject, firstPatternIntermediate, "p0,Sb", SAMPLE_EDGE_TYPE),
						new GPEdge(firstPatternIntermediate, resource, "p0,bR", SAMPLE_EDGE_TYPE),
						
						new GPEdge(subject, common, "p1,Sd", SAMPLE_EDGE_TYPE),
						new GPEdge(common, resource, "p1,dR", SAMPLE_EDGE_TYPE),
						new GPEdge(subject, secondPatternIntermediate, "p1,Sc", SAMPLE_EDGE_TYPE),
						new GPEdge(secondPatternIntermediate, resource, "p1,cR", SAMPLE_EDGE_TYPE),
						new GPEdge(secondPatternIntermediate, common, "p1,cd", SAMPLE_EDGE_TYPE)
				)
		);
		return new GraphPattern(
				graph, List.of(), Map.of(), Map.of(), List.of(resource), Map.of(
						"subject", subject,
						"resource", resource,
						"common", common
				)
		);
	}
	
}
