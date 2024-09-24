package io.github.danthe1st.arebac.neo4j.tests.airbnb;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator.EQUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.danthe1st.arebac.data.graph_pattern.GPEdge;
import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.gpeval.GPEval;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jDB;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jNode;
import io.github.danthe1st.arebac.weaving.Weaving;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.neo4j.dbms.archive.IncorrectFormat;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;

/*
 * The idea/scenario this file is based on was provided by the Institut für anwendungsorientierte Wissensverarbeitung at Johannes Kepler University Linz.
 * The source code was written by Daniel Schmid.
 */
class Scenario2Test {

	private GraphDatabaseService database;
	
	@BeforeEach
	void setUp() throws IOException, IncorrectFormat, InterruptedException, URISyntaxException {
		database = AirbnbSetup.getDatabase();
	}

	@ParameterizedTest
	@CsvSource({
			"278934759,1310402,3", // a host with 3 listings in the neighborhood
			"363134483,1310402,1"// a host with 2 listings, one in the given neighborhood
	})
	void accessListingsInNeighborhood(String subjectId, String neighborhoodId, int expectedResultCount) {
		GraphPattern combined = editListingInNeighborhoodPattern(subjectId, neighborhoodId);
		try(Transaction tx = database.beginTx()){
			Neo4jDB db = new Neo4jDB(tx);
			Set<List<Neo4jNode>> result = GPEval.evaluate(db, combined);
			assertEquals(expectedResultCount, result.size());
			for(List<Neo4jNode> resultEntry : result){
				assertEquals(1, resultEntry.size());
				Node listing = resultEntry.get(0).getDBNode();
				assertEquals(subjectId, getSingleRelationship(listing, "HOSTS").getStartNode().getProperty("host_id"));
				assertEquals(neighborhoodId, getSingleRelationship(listing, "IN_NEIGHBORHOOD").getEndNode().getProperty("region_id"));
			}
		}
	}
	
	private Relationship getSingleRelationship(Node node, String name) {
		try(ResourceIterable<Relationship> relationships = node.getRelationships(RelationshipType.withName(name))){
			List<Relationship> outgoingRelationships = relationships.stream().toList();
			assertEquals(1, outgoingRelationships.size());
			return outgoingRelationships.get(0);
		}
	}

	private GraphPattern editListingInNeighborhoodPattern(String subjectId, String neighborhoodId) {
		GraphPattern policy = editOwnListingPattern(subjectId);

		GPNode listing = new GPNode("listing", AirbnbSetup.LISTING);
		GPNode neighborhood = new GPNode("neighborhood", AirbnbSetup.NEIGHBORHOOD);
		GraphPattern query = new GraphPattern(
				new GPGraph(
						List.of(listing, neighborhood),
						List.of(new GPEdge(listing, neighborhood, "inNeighborhood", "IN_NEIGHBORHOOD"))
				), List.of(),
				Map.of(neighborhood, List.of(new AttributeRequirement("region_id", EQUAL, attribute(neighborhoodId)))),
				Map.of(),
				List.of(listing),
				Map.of("listing", listing)
		);
		return Weaving.combinePatterns(List.of(policy, query));
	}

	private GraphPattern editOwnListingPattern(String subjectId) {
		GPNode subject = new GPNode("host", AirbnbSetup.HOST);
		GPNode listing = new GPNode("listing", AirbnbSetup.LISTING);
		return new GraphPattern(
				new GPGraph(
						List.of(subject, listing),
						List.of(new GPEdge(subject, listing, "hosts", "HOSTS"))
				),
				List.of(),
				Map.of(subject, List.of(new AttributeRequirement("host_id", EQUAL, attribute(subjectId)))),
				Map.of(),
				List.of(),
				Map.of("subject", subject, "listing", listing)
		);
	}
}
