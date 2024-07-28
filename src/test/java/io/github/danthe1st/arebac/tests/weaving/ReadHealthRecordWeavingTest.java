package io.github.danthe1st.arebac.tests.weaving;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement.ID_KEY;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator.EQUAL;
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

//example 12-14 from the paper
class ReadHealthRecordWeavingTest {

	@Test
	void testCombineDoctorTreatingClinitian() {
		GraphPattern combined = Weaving.combinePatterns(List.of(createDoctorRequirementPolicy(), createTreatingClinitianRequirementPolicy()));
		assertEquals(createExpectedDoctorTreatingClinitianCombinedPolicy(), combined);
	}

	@Test
	void testCombineRequestWithCombinedPolicyPattern() {
		GraphPattern combined = Weaving.combinePatterns(List.of(createQueryPattern(), createExpectedDoctorTreatingClinitianCombinedPolicy()));
		assertEquals(createExpectedRequestWithPolicyPattern("p1,p1,treating_clinitian"), combined);
	}
	
	@Test
	void testCombineRequestWithBothPolicies() {
		GraphPattern combined = Weaving.combinePatterns(List.of(createQueryPattern(), createDoctorRequirementPolicy(), createTreatingClinitianRequirementPolicy()));
		assertEquals(createExpectedRequestWithPolicyPattern("p2,treating_clinitian"), combined);
	}
	
	private GraphPattern createDoctorRequirementPolicy() {
		GPNode requestor = new GPNode("r", "user");
		GPNode patient = new GPNode("p", "user");
		GPGraph graph = new GPGraph(List.of(requestor, patient), List.of());
		return new GraphPattern(
				graph,
				List.of(),
				Map.of(requestor, List.of(new AttributeRequirement("role", EQUAL, attribute("doctor")))),
				Map.of(),
				List.of(),
				Map.of("requestor", requestor, "patient", patient)
		);
	}

	private GraphPattern createTreatingClinitianRequirementPolicy() {
		GPNode requestor = new GPNode("r", "user");
		GPNode patient = new GPNode("p", "user");
		GPNode healthRecord = new GPNode("h", "health_record");
		GPEdge treatingClinitian = new GPEdge(requestor, patient, "treating_clinitian", "treating_clinitian");
		GPGraph graph = new GPGraph(List.of(requestor, patient, healthRecord), List.of(treatingClinitian));
		return new GraphPattern(
				graph,
				List.of(),
				Map.of(),
				Map.of(),
				List.of(),
				Map.of("requestor", requestor, "patient", patient, "health_record", healthRecord)
		);
	}

	private GraphPattern createExpectedDoctorTreatingClinitianCombinedPolicy() {
		GPNode requestor = new GPNode("p0,r", "user");
		GPNode patient = new GPNode("p0,p", "user");
		GPNode healthRecord = new GPNode("p1,h", "health_record");
		GPEdge treatingClinitian = new GPEdge(requestor, patient, "p1,treating_clinitian", "treating_clinitian");
		GPGraph graph = new GPGraph(List.of(requestor, patient, healthRecord), List.of(treatingClinitian));
		return new GraphPattern(
				graph,
				List.of(),
				Map.of(requestor, List.of(new AttributeRequirement("role", EQUAL, attribute("doctor")))),
				Map.of(),
				List.of(),
				Map.of("requestor", requestor, "patient", patient, "health_record", healthRecord)
		);
	}

	// example 9 from the paper
	private GraphPattern createQueryPattern() {
		GPNode requestor = new GPNode("requestor", "user");
		GPNode patient = new GPNode("patient", "user");
		GPNode healthRecord = new GPNode("health_record", "health_record");
		
		return new GraphPattern(
				new GPGraph(
						List.of(requestor, patient, healthRecord),
						List.of(new GPEdge(patient, healthRecord, "pr", "patient_record"))
				),
				List.of(),
				Map.of(
						requestor, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute("12321"))),
						healthRecord, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute("123")))
				),
				Map.of(),
				List.of(healthRecord),
				Map.of("requestor", requestor, "health_record", healthRecord, "patient", patient)
		);
	}
	
	private GraphPattern createExpectedRequestWithPolicyPattern(String treatingClinitianEdgeName) {
		GPNode requestor = new GPNode("p0,requestor", "user");
		GPNode patient = new GPNode("p0,patient", "user");
		GPNode healthRecord = new GPNode("p0,health_record", "health_record");
		
		return new GraphPattern(
				new GPGraph(
						List.of(requestor, patient, healthRecord),
						List.of(
								new GPEdge(patient, healthRecord, "p0,pr", "patient_record"),
								new GPEdge(requestor, patient, treatingClinitianEdgeName, "treating_clinitian")
						)
				),
				List.of(),
				Map.of(
						requestor, List.of(
								new AttributeRequirement(ID_KEY, EQUAL, attribute("12321")),
								new AttributeRequirement("role", EQUAL, attribute("doctor"))
						),
						healthRecord, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute("123")))
				),
				Map.of(),
				List.of(healthRecord),
				Map.of("requestor", requestor, "health_record", healthRecord, "patient", patient)
		);
	}

}
