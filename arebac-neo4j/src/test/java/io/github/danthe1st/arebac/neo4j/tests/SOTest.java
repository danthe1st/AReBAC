package io.github.danthe1st.arebac.neo4j.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
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
import io.github.danthe1st.arebac.gpeval.GPEval;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jDB;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jNode;
import org.junit.jupiter.api.Test;
import org.neo4j.dbms.archive.IncorrectFormat;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;

class SOTest {
	private GraphDatabaseService database;
	
	@Test
	void evaluate() throws IOException, IncorrectFormat, InterruptedException, URISyntaxException {
		database = Neo4JSetup.getDatabase();
		try(Transaction tx = database.beginTx()){
			Neo4jDB dbAsGraph = new Neo4jDB(tx);
			Node someUserNode = tx.findNode(Label.label("User"), "uuid", 6309);
			String someUserId = someUserNode.getElementId();
			GraphPattern pattern = createPattern(someUserId);
			Set<List<Neo4jNode>> results = GPEval.evaluate(dbAsGraph, pattern);
			assertNotEquals(0, results.size());
			Set<List<Neo4jNode>> expectedAnswers = new HashSet<>();
			ResourceIterable<Relationship> rels = someUserNode.getRelationships(Direction.OUTGOING, Neo4JSetup.RelType.PROVIDED);
			for(Relationship relationship : rels){
				Node otherNode = relationship.getOtherNode(someUserNode);
				expectedAnswers.add(List.of(new Neo4jNode(otherNode)));
			}
			assertEquals(expectedAnswers, results);
		}
	}
	
	private GraphPattern createPattern(String requestorId) {
		GPNode requestor = new GPNode("requestor", Neo4JSetup.USER.name());
		GPNode answer = new GPNode("answer", Neo4JSetup.ANSWER.name());
		return new GraphPattern(
				new GPGraph(
						List.of(requestor, answer),
						List.of(new GPEdge(requestor, answer, null, Neo4JSetup.RelType.PROVIDED.name()))
				),
				List.of(),
				Map.of(requestor, List.of(new AttributeRequirement(AttributeRequirement.ID_KEY, AttributeRequirementOperator.EQUAL, AttributeValue.attribute(requestorId)))),
				Map.of(),
				List.of(answer), Map.of("requestor", requestor, "answer", answer)
		);
	}
}

