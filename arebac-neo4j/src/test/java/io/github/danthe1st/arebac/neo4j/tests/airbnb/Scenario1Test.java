package io.github.danthe1st.arebac.neo4j.tests.airbnb;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator.EQUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
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
 * The idea/scenario/policy this file is based on was provided by the Institut für anwendungsorientierte Wissensverarbeitung at Johannes Kepler University Linz.
 * The source code was written by Daniel Schmid.
 */
class Scenario1Test {
	
	private GraphDatabaseService database;
	
	@BeforeEach
	void setUp() throws IOException, IncorrectFormat {
		database = AirbnbSetup.getDatabase();
	}
	
	@ParameterizedTest
	@CsvSource({
			"131304391,120",
			"155715332,138",
			"404944621,48"
	})
	void testGetAllReviewsFromSomeHost(String hostId, int expectedCount) {
		/*
		 * get expected count of reviews from Neo4J:
		 * MATCH (r:Review) -[: REVIEWS]->(l: Listing)<-[HOSTS]-(h: Host)
		 * WHERE h.host_id="HOST_ID"
		 * RETURN COUNT(r)
		 */
		GraphPattern combined = createAuthorizedCetAllReviewsFromHostGraphPattern(hostId);
		try(Transaction tx = database.beginTx()){
			Neo4jDB db = new Neo4jDB(tx);
			Set<List<Neo4jNode>> result = GPEval.evaluate(db, combined);
			assertEquals(expectedCount, result.size());
			for(List<Neo4jNode> resultEntry : result){
				assertEquals(2, resultEntry.size());
				Node reviewerNode = resultEntry.get(0).getDBNode();
				Node reviewNode = resultEntry.get(1).getDBNode();
				assertEquals(reviewerNode, getSingleRelationship(reviewNode, "WROTE").getStartNode());
				Node listingNode = getSingleRelationship(reviewNode, "REVIEWS").getEndNode();
				Node hostNode = getSingleRelationship(listingNode, "HOSTS").getStartNode();
				assertEquals(hostId, hostNode.getProperty("host_id"));
			}
		}
	}

	static GraphPattern createAuthorizedCetAllReviewsFromHostGraphPattern(String hostId) {
		GraphPattern policy = wroteReviewGraphPatternForHostSubject(hostId);
		GPNode reviewer = new GPNode("reviewer", AirbnbSetup.REVIEWER);
		GPNode review = new GPNode("review", AirbnbSetup.REVIEW);
		GraphPattern query = new GraphPattern(
				new GPGraph(List.of(reviewer, review), List.of()),
				List.of(),
				Map.of(), Map.of(),
				List.of(reviewer, review),
				Map.of("review", review, "reviewer", reviewer)
		);
		return Weaving.combinePatterns(List.of(policy, query));
	}
	
	@ParameterizedTest
	@CsvSource({
			"272671293,6",
			"227163707,19",
			"268281268,11",
			"31292360,8"
	})
	void testGetAllReviewsFromReviewer(String reviewerId, int expectedCount) {
		/*
		 * get count of reviews for reviewer:
		 * MATCH (r:Reviewer) -[: WROTE]-> (r2: Review)
		 * WHERE r.reviewer_id="REVIEWER_ID"
		 *
		 * or:
		 * MATCH (r:Reviewer) -[: WROTE]-> (r2: Review)
		 * RETURN r,COUNT(DISTINCT r2)
		 * ORDER BY COUNT(DISTINCT r2) DESC
		 *
		 */
		GraphPattern combined = createAuthorizedGetAllReviewsFromReviewerGraphPattern(reviewerId);
		try(Transaction tx = database.beginTx()){
			Neo4jDB db = new Neo4jDB(tx);
			Set<List<Neo4jNode>> result = GPEval.evaluate(db, combined);
			assertEquals(expectedCount, result.size());
			for(List<Neo4jNode> resultEntry : result){
				assertEquals(1, resultEntry.size());
				Node reviewNode = resultEntry.get(0).getDBNode();
				assertEquals(reviewerId, getSingleRelationship(reviewNode, "WROTE").getStartNode().getProperty("reviewer_id"));
			}
		}
	}

	static GraphPattern createAuthorizedGetAllReviewsFromReviewerGraphPattern(String reviewerId) {
		GraphPattern policy = wroteReviewGraphPatternForReviewerSubject(reviewerId);
		GPNode review = new GPNode("review", AirbnbSetup.REVIEW);
		GraphPattern query = new GraphPattern(
				new GPGraph(List.of(review), List.of()),
				List.of(),
				Map.of(), Map.of(),
				List.of(review),
				Map.of("review", review)
		);
		return Weaving.combinePatterns(List.of(policy, query));
	}

	private Relationship getSingleRelationship(Node node, String name) {
		try(ResourceIterable<Relationship> relationships = node.getRelationships(RelationshipType.withName(name))){
			List<Relationship> outgoingRelationships = relationships.stream().toList();
			assertEquals(1, outgoingRelationships.size());
			return outgoingRelationships.get(0);
		}
	}
	
	private static GraphPattern wroteReviewGraphPatternForReviewerSubject(String subjectId) {
        GPNode subject = new GPNode("subject", AirbnbSetup.REVIEWER);
        GPNode review = new GPNode("review", AirbnbSetup.REVIEW);
		return new GraphPattern(
				new GPGraph(
						List.of(subject, review),
						List.of(new GPEdge(subject, review, "write_review", "WROTE"))
				),
				List.of(),
				Map.of(subject, List.of(new AttributeRequirement("reviewer_id", EQUAL, attribute(subjectId)))),
				Map.of(),
				List.of(),
				Map.of("subject", subject, "reviewer", subject, "review", review)
		);
    }
	
	private static GraphPattern wroteReviewGraphPatternForHostSubject(String subjectId) {
		GPNode reviewer = new GPNode("reviewer", AirbnbSetup.REVIEWER);
		GPNode subject = new GPNode("host", AirbnbSetup.HOST);
		GPNode review = new GPNode("review", AirbnbSetup.REVIEW);
		GPNode listing = new GPNode("listing", AirbnbSetup.LISTING);
		return new GraphPattern(
				new GPGraph(
						List.of(reviewer, review, subject, listing),
						List.of(
								new GPEdge(reviewer, review, "write_review", "WROTE"),
								new GPEdge(review, listing, "reviews", "REVIEWS"),
								new GPEdge(subject, listing, "hosts", "HOSTS")
						)
				),
				List.of(),
				Map.of(subject, List.of(new AttributeRequirement("host_id", EQUAL, attribute(subjectId)))),
				Map.of(),
				List.of(),
				Map.of("subject", subject, "reviewer", reviewer, "review", review)
		);
	}
}
