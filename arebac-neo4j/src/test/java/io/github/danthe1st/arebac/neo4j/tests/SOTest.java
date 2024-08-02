package io.github.danthe1st.arebac.neo4j.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.github.danthe1st.arebac.data.graph_pattern.constraints.MutualExclusionConstraint;
import io.github.danthe1st.arebac.data.memory.InMemoryGraph;
import io.github.danthe1st.arebac.data.memory.InMemoryGraphEdge;
import io.github.danthe1st.arebac.data.memory.InMemoryGraphNode;
import io.github.danthe1st.arebac.gpeval.GPEval;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jDB;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jNode;
import io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup.RelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.neo4j.dbms.archive.IncorrectFormat;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

class SOTest {
	private GraphDatabaseService database;

	@BeforeEach
	void setUp() throws IOException, IncorrectFormat, InterruptedException, URISyntaxException {
		database = Neo4JSetup.getDatabase();
	}

	@Test
	@Order(1)
	void testFindAnswersOfUser() {
		try(Transaction tx = database.beginTx()){
			Neo4jDB dbAsGraph = new Neo4jDB(tx);
			Node someUserNode = tx.findNode(Label.label("User"), "uuid", 6309);
			String someUserId = someUserNode.getElementId();
			GraphPattern pattern = createFindAnswersPattern(someUserId);
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

	private GraphPattern createFindAnswersPattern(String requestorId) {
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

	@Test
	@Order(2)
	void testWithFakeDB() {
		InMemoryGraphNode q1 = new InMemoryGraphNode("q1", "Question", Map.of());
		InMemoryGraphNode q2 = new InMemoryGraphNode("q2", "Question", Map.of());

		InMemoryGraphNode u1 = new InMemoryGraphNode("u1", "User", Map.of());
		InMemoryGraphNode u2 = new InMemoryGraphNode("u2", "User", Map.of());

		InMemoryGraphNode u1q1 = new InMemoryGraphNode("u1q1", "Comment", Map.of());
		InMemoryGraphNode u1q2 = new InMemoryGraphNode("u1q2", "Comment", Map.of());
		InMemoryGraphNode u2q1 = new InMemoryGraphNode("u2q1", "Comment", Map.of());
		InMemoryGraphNode u2q2 = new InMemoryGraphNode("u2q2", "Comment", Map.of());

		InMemoryGraph graph = new InMemoryGraph(
				List.of(q1, q2, u1, u2, u1q1, u1q2, u2q1, u2q2),
				List.of(
						new InMemoryGraphEdge(u1q1, q1, "u1q1C", "COMMENTED_ON", Map.of()),
						new InMemoryGraphEdge(u1q2, q2, "u1q2C", "COMMENTED_ON", Map.of()),
						new InMemoryGraphEdge(u2q1, q1, "u2q1C", "COMMENTED_ON", Map.of()),
						new InMemoryGraphEdge(u2q2, q2, "u2q2C", "COMMENTED_ON", Map.of()),
						new InMemoryGraphEdge(u1, u1q1, "u1q1U", "COMMENTED", Map.of()),
						new InMemoryGraphEdge(u1, u1q2, "u1q2U", "COMMENTED", Map.of()),
						new InMemoryGraphEdge(u2, u2q1, "u2q1U", "COMMENTED", Map.of()),
						new InMemoryGraphEdge(u2, u2q2, "u2q2U", "COMMENTED", Map.of())
				)
		);

		Set<List<InMemoryGraphNode>> results = GPEval.evaluate(graph, createCommentsToSameQuestionInTagPattern("u1"));
		assertEquals(2, results.size());

	}
	
	@Test
	@Order(3)
	void testFindCommentsFromSameUsersToQuestionsInTag() {
		try(Transaction tx = database.beginTx()){
			long expectedElementCount;
			try(Result testResult = tx.execute(
					"""
							MATCH (q1:Question)<-[:COMMENTED_ON]-(u1c1:Comment)<-[:COMMENTED]-(u1:User{uuid:$uID})
							MATCH (q1:Question)<-[:COMMENTED_ON]-(u2c1:Comment)<-[:COMMENTED]-(u2:User)
							MATCH (q2:Question)<-[:COMMENTED_ON]-(u1c2:Comment)<-[:COMMENTED]-(u1:User)
							MATCH (q2:Question)<-[:COMMENTED_ON]-(u2c2:Comment)<-[:COMMENTED]-(u2:User)
							WHERE q1 <> q2 AND u1 <> u2 AND u1.uuid=$uID
							RETURN u1.uuid, u2.uuid
							""",
					Map.of("uID", 6692895)
			)){
				expectedElementCount = testResult.stream().count();
			}

			Node userNode = tx.findNode(Neo4JSetup.USER, "uuid", 6692895);
			GraphPattern pattern = createCommentsToSameQuestionInTagPattern(userNode.getElementId());
			Set<List<Neo4jNode>> results = GPEval.evaluate(new Neo4jDB(tx), pattern);
			assertNotEquals(0, results.size());
			assertEquals(expectedElementCount, results.size());
			for(List<Neo4jNode> result : results){
				Neo4jNode user1Comment1 = result.get(0);
				Neo4jNode user2Comment1 = result.get(1);
				Neo4jNode user1Comment2 = result.get(2);
				Neo4jNode user2Comment2 = result.get(3);
				checkHasSingleRelationToSameNode(user1Comment1, user2Comment1, Neo4JSetup.RelType.COMMENTED_ON);
				checkHasSingleRelationToSameNode(user1Comment2, user2Comment2, Neo4JSetup.RelType.COMMENTED_ON);
				checkHasSingleRelationToSameNode(user1Comment1, user1Comment2, Neo4JSetup.RelType.COMMENTED);
				checkHasSingleRelationToSameNode(user2Comment1, user2Comment2, Neo4JSetup.RelType.COMMENTED);

				checkHasSingleRelationToDifferentNodes(user1Comment1, user2Comment1, Neo4JSetup.RelType.COMMENTED);
				checkHasSingleRelationToDifferentNodes(user1Comment2, user2Comment2, Neo4JSetup.RelType.COMMENTED);
				checkHasSingleRelationToDifferentNodes(user1Comment1, user1Comment2, Neo4JSetup.RelType.COMMENTED_ON);
				checkHasSingleRelationToDifferentNodes(user2Comment1, user2Comment2, Neo4JSetup.RelType.COMMENTED_ON);
			}
		}
	}

	private void checkHasSingleRelationToSameNode(Neo4jNode first, Neo4jNode second, RelType relationType) {
		Node firstQuestionNode = getOtherNodeOfSingleRelationship(first, relationType);
		Node secondQuestionNode = getOtherNodeOfSingleRelationship(second, relationType);
		assertEquals(firstQuestionNode, secondQuestionNode);
	}

	private void checkHasSingleRelationToDifferentNodes(Neo4jNode first, Neo4jNode second, RelType relationType) {
		Node firstQuestionNode = getOtherNodeOfSingleRelationship(first, relationType);
		Node secondQuestionNode = getOtherNodeOfSingleRelationship(second, relationType);
		assertNotEquals(firstQuestionNode, secondQuestionNode);
	}

	private Node getOtherNodeOfSingleRelationship(Neo4jNode first, RelType relationshipType) {
		try(ResourceIterable<Relationship> relationships = first.getDBNode().getRelationships(relationshipType)){
			ResourceIterator<Relationship> it = relationships.iterator();
			assertTrue(it.hasNext());
			Relationship relationship = it.next();
			assertFalse(it.hasNext());
			return relationship.getOtherNode(first.getDBNode());
		}
	}

	private GraphPattern createCommentsToSameQuestionInTagPattern(String userId) {
		GPNode user1 = new GPNode("user1", Neo4JSetup.USER.name());
		GPNode user2 = new GPNode("user2", Neo4JSetup.USER.name());

		GPNode question1 = new GPNode("question1", Neo4JSetup.QUESTION.name());
		GPNode user1Comment1 = new GPNode("user1Answer1", Neo4JSetup.COMMENT.name());
		GPNode user2Comment1 = new GPNode("user2Answer1", Neo4JSetup.COMMENT.name());

		GPNode question2 = new GPNode("question2", Neo4JSetup.QUESTION.name());
		GPNode user1Comment2 = new GPNode("user1Answer2", Neo4JSetup.COMMENT.name());
		GPNode user2Comment2 = new GPNode("user2Answer2", Neo4JSetup.COMMENT.name());

		GPGraph graph = new GPGraph(
				List.of(
						user1, user2, question1, question2, user1Comment1, user1Comment2, user2Comment1, user2Comment2
				),
				List.of(

						new GPEdge(user1, user1Comment1, null, Neo4JSetup.RelType.COMMENTED.name()),
						new GPEdge(user1, user1Comment2, null, Neo4JSetup.RelType.COMMENTED.name()),
						new GPEdge(user2, user2Comment1, null, Neo4JSetup.RelType.COMMENTED.name()),
						new GPEdge(user2, user2Comment2, null, Neo4JSetup.RelType.COMMENTED.name()),

						new GPEdge(user1Comment1, question1, null, Neo4JSetup.RelType.COMMENTED_ON.name()),
						new GPEdge(user1Comment2, question2, null, Neo4JSetup.RelType.COMMENTED_ON.name()),
						new GPEdge(user2Comment1, question1, null, Neo4JSetup.RelType.COMMENTED_ON.name()),
						new GPEdge(user2Comment2, question2, null, Neo4JSetup.RelType.COMMENTED_ON.name())
				)
		);
		return new GraphPattern(
				graph,
				List.of(
						new MutualExclusionConstraint(user1, user2),
						new MutualExclusionConstraint(question1, question2)
				),
				Map.of(
						user1, List.of(new AttributeRequirement(AttributeRequirement.ID_KEY, AttributeRequirementOperator.EQUAL, AttributeValue.attribute(userId)))
				),
				Map.of(),
				List.of(user1Comment1, user2Comment1, user1Comment2, user2Comment2),
				Map.of("firstUser", user1, "secondUser", user2)
		);
	}
}

