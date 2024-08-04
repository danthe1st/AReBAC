package io.github.danthe1st.arebac.tests.weaving;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement.ID_KEY;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator.EQUAL;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator.LESS_THAN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import io.github.danthe1st.arebac.data.graph_pattern.GPEdge;
import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.weaving.Weaving;
import org.junit.jupiter.api.Test;

class StudentExampleWeavingTest {
	@Test
	void studentExample() {
		GraphPattern requestPattern = createRequestPattern();
		GraphPattern policyPattern = createPolicyPattern();
		GraphPattern expectedPattern = createExpectedCombinedPattern();
		GraphPattern combinedPattern = Weaving.combinePatterns(List.of(requestPattern, policyPattern));
		assertEquals(expectedPattern, combinedPattern);
	}


	private GraphPattern createRequestPattern() {
		GPNode student = new GPNode("student", "student");
		GPNode course = new GPNode("course", "course");
		GPEdge attendCourse = new GPEdge(student, course, "ac", "attend_course");
		GPGraph requestGraph = new GPGraph(
				List.of(student, course),
				List.of(attendCourse)
		);
		return new GraphPattern(
				requestGraph,
				List.of(),
				Map.of(student, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute("123")))),
				Map.of(attendCourse, List.of(new AttributeRequirement("grade", LESS_THAN, attribute(5)))),
				List.of(course),
				Map.of("student", student, "course", course)
		);
	}
	
	private GraphPattern createPolicyPattern() {

		GPNode requestorNode = new GPNode("requestor", "professor");
		GPNode courseNode = new GPNode("course2", "course");
		GPEdge teachCourse = new GPEdge(requestorNode, courseNode, "tc", "teach_course");
		
		GPGraph policyGraph = new GPGraph(
				List.of(requestorNode, courseNode),
				List.of(teachCourse)
		);

		return new GraphPattern(
				policyGraph,
				List.of(),
				Map.of(requestorNode, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute("123")))),
				Map.of(),
				List.of(),
				Map.of("course", courseNode)
		);
	}
	
	private GraphPattern createExpectedCombinedPattern() {
		GPNode student = new GPNode("p0,student", "student");
		GPNode course = new GPNode("p0,course", "course");
		GPNode requestor = new GPNode("p1,requestor", "professor");
		
		GPEdge attendCourse = new GPEdge(student, course, "p0,ac", "attend_course");
		GPEdge teachCourse = new GPEdge(requestor, course, "p1,tc", "teach_course");
		
		GPGraph graph = new GPGraph(List.of(student, course, requestor), List.of(attendCourse, teachCourse));
		
		return new GraphPattern(
				graph,
				List.of(),
				Map.of(
						student, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute("123"))),
						requestor, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute("123")))
				),
				Map.of(attendCourse, List.of(new AttributeRequirement("grade", LESS_THAN, attribute(5)))),
				List.of(course),
				Map.of("student", student, "course", course)
		);
	}
}
