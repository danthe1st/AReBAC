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

class StudentExampleTest {
	@Test
	void studentExample() {
		GraphPattern pattern = createStudentGraphPattern();
		InMemoryGraph sampleGraph = createSampleGraph();
		assertEquals(Set.of(List.of(new InMemoryGraphNode("somecourse", "course", Map.of()))), GPEval.evaluate(sampleGraph, pattern));
	}

	private GraphPattern createStudentGraphPattern() {
		GPNode requestor = new GPNode("requestor", "prof");
		GPNode student = new GPNode("student", "student");
		GPNode course = new GPNode("course", "course");
		GPEdge attendCourse = new GPEdge(student, course, "ac", "attend_course");
		GPEdge teachCourse = new GPEdge(requestor, course, "tc", "teach_course");
		GPGraph graph = new GPGraph(List.of(requestor, student, course), List.of(attendCourse, teachCourse));
		return new GraphPattern(
				graph,
				List.of(),
				Map.of(
						requestor, List.of(new AttributeRequirement(AttributeRequirement.ID_KEY, AttributeRequirementOperator.EQUAL, AttributeValue.attribute("1337331"))),
						student, List.of(new AttributeRequirement(AttributeRequirement.ID_KEY, AttributeRequirementOperator.EQUAL, AttributeValue.attribute("12345678")))
				),
				Map.of(
						attendCourse, List.of(new AttributeRequirement("grade", AttributeRequirementOperator.LESS_THAN, new AttributeValue.IntAttribute(5)))
				),
				List.of(course)
		);
	}
	
	private InMemoryGraph createSampleGraph() {
		InMemoryGraphNode requestor = new InMemoryGraphNode("1337331", "prof", Map.of());
		InMemoryGraphNode student = new InMemoryGraphNode("12345678", "student", Map.of());
		InMemoryGraphNode course = new InMemoryGraphNode("somecourse", "course", Map.of());
		InMemoryGraphNode otherCourse = new InMemoryGraphNode("othercourse", "course", Map.of());
		InMemoryGraphEdge attendCourse = new InMemoryGraphEdge(student, course, "ac", "attend_course", Map.of("grade", AttributeValue.attribute(1)));
		InMemoryGraphEdge attendOtherCourse = new InMemoryGraphEdge(student, otherCourse, "ac2", "attend_course", Map.of("grade", AttributeValue.attribute(1)));
		InMemoryGraphEdge teachCourse = new InMemoryGraphEdge(requestor, course, "tc", "teach_course", Map.of());
		return new InMemoryGraph(List.of(requestor, student, course), List.of(attendCourse, attendOtherCourse, teachCourse));
	}
}
