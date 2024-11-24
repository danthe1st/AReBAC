package io.github.danthe1st.arebac.neo4j.tests.stackoverflow;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator;
import io.github.danthe1st.arebac.gpeval.GPEval;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jDB;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.dbms.archive.IncorrectFormat;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

class UniquenessTest {
	private GraphDatabaseService database;
	
	@BeforeEach
	void setUp() throws IOException, IncorrectFormat, InterruptedException, URISyntaxException {
		database = SOSetup.getDatabase();
	}
	
	@Test
	void testUniqueConstraint() {
		try(Transaction tx = database.beginTx()){
			Neo4jDB db = new Neo4jDB(tx);
			assertTrue(db.isAttributeUniqueForNodeType("name", "Tag"));
			// executing it twice (using the lookup) shouldn't change the result
			assertTrue(db.isAttributeUniqueForNodeType("name", "Tag"));
		}
	}
	
	@Test
	void testGetByUniqueAttribute() {
		try(Transaction tx = database.beginTx()){
			Neo4jDB db = new Neo4jDB(tx);
			Neo4jNode node = db.getNodeByUniqueAttribute("Tag", "name", attribute("neo4j"));
			assertEquals("neo4j", node.getAttribute("name").value());
		}
	}
	
	@Test
	void testGetByUniqueAttributeWithNonexistentValue() {
		try(Transaction tx = database.beginTx()){
			Neo4jDB db = new Neo4jDB(tx);
			Neo4jNode node = db.getNodeByUniqueAttribute("Tag", "name", attribute("dfsgdfgd"));
			assertNull(node);
		}
	}
	
	@Test
	void evaluateGraphPatternWithUniqueAttributeWithNonexistentValue() {
		GPNode gpNode = new GPNode("tag", "Tag");
		GraphPattern pattern = new GraphPattern(
				new GPGraph(List.of(gpNode), List.of()),
				List.of(),
				Map.of(gpNode, List.of(new AttributeRequirement("name", AttributeRequirementOperator.EQUAL, attribute("sgdfjkghsdkgh")))),
				Map.of(), List.of(gpNode), Map.of()
		);
		try(Transaction tx = database.beginTx()){
			Neo4jDB db = new Neo4jDB(tx);
			Set<List<Neo4jNode>> result = GPEval.evaluate(db, pattern);
			assertTrue(result.isEmpty());
		}
	}
}
