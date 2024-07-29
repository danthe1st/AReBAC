package io.github.danthe1st.arebac.tests.weaving;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.weaving.Weaving;
import org.junit.jupiter.api.Test;

class MultiplePatternsReturnSameNodeTest {
	@Test
    void testMeregPatternsReturnSameResult() {
        GPNode singleNode = new GPNode("someNode", "user");
		GraphPattern simplePattern = new GraphPattern(
				new GPGraph(List.of(singleNode), List.of()),
				List.of(), Map.of(), Map.of(),
				List.of(singleNode),
				Map.of("relevantNode", singleNode)
		);
		GraphPattern combinedWithItself = Weaving.combinePatterns(List.of(simplePattern, simplePattern));
		
		GPNode nodeInResult = new GPNode("p0,someNode", "user");
		GraphPattern expectedPattern = new GraphPattern(
				new GPGraph(List.of(nodeInResult), List.of()),
				List.of(), Map.of(), Map.of(),
				List.of(nodeInResult, nodeInResult),
				Map.of("relevantNode", nodeInResult)
		);
		assertEquals(expectedPattern, combinedWithItself);
    }
}
