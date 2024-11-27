package io.github.danthe1st.arebac.neo4j.tests.airbnb;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement.ID_KEY;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator.EQUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.gpeval.GPEval;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jDB;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.dbms.archive.IncorrectFormat;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

class ElementIdTest {

	private GraphDatabaseService database;

	@BeforeEach
	void setUp() throws IOException, IncorrectFormat {
		database = AirbnbSetup.getDatabase();
	}

	@Test
	void testLookupWithElementId() {
		try(Transaction tx = database.beginTx()){
			Node someNode = findSomeNode(tx);
			Neo4jNode retrieved = new Neo4jDB(tx).findNodeById(someNode.getElementId());
			assertNotNull(retrieved);
			assertEquals(someNode.getElementId(), retrieved.getDBNode().getElementId());
		}
	}

	@Test
	void testLookupWithNonexistentElementId() {
		try(Transaction tx = database.beginTx()){
			Neo4jNode retrieved = new Neo4jDB(tx).findNodeById("thisdoesnotexist");
			assertNull(retrieved);
		}
	}

	@Test
	void testLookupWithNonexistentElementIdUsingGPEval() {
		try(Transaction tx = database.beginTx()){
			GraphPattern pattern = createGraphPatternExpectingElementId("thisdoesnotexist");
			Set<List<Neo4jNode>> result = GPEval.evaluate(new Neo4jDB(tx), pattern);
			assertEquals(Set.of(), result);
		}
	}
	
	@Test
	void testLookupWithElementIdUsingGPEval() {
		try(Transaction tx = database.beginTx()){
			Node someNode = findSomeNode(tx);
			String elementId = someNode.getElementId();
			GraphPattern pattern = createGraphPatternExpectingElementId(elementId);
			Set<List<Neo4jNode>> result = GPEval.evaluate(new Neo4jDB(tx), pattern);
			assertEquals(1, result.size());
			assertEquals(result, Set.of(List.of(new Neo4jNode(someNode))));
		}
	}

	private GraphPattern createGraphPatternExpectingElementId(String elementId) {
		GPNode patternNode = new GPNode("node", AirbnbSetup.HOST);
		return new GraphPattern(
				new GPGraph(List.of(patternNode), List.of()),
				List.of(),
				Map.of(patternNode, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute(elementId)))),
				Map.of(),
				List.of(patternNode),
				Map.of()
		);
	}
	
	private Node findSomeNode(Transaction tx) {
		try(ResourceIterator<Node> nodes = tx.findNodes(Label.label(AirbnbSetup.HOST))){
			return nodes.next();
		}
	}
}
