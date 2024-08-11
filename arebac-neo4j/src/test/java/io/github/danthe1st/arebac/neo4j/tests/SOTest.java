package io.github.danthe1st.arebac.neo4j.tests;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement.ID_KEY;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator.EQUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.danthe1st.arebac.data.graph_pattern.GPEdge;
import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.MutualExclusionConstraint;
import io.github.danthe1st.arebac.data.memory.InMemoryGraph;
import io.github.danthe1st.arebac.data.memory.InMemoryGraphEdge;
import io.github.danthe1st.arebac.data.memory.InMemoryGraphNode;
import io.github.danthe1st.arebac.gpeval.GPEval;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jDB;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jNode;
import io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup.RelType;
import org.junit.jupiter.api.BeforeEach;
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
		database = SOSetup.getDatabase();
	}

	@Test
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
				Map.of(requestor, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute(requestorId)))),
				Map.of(),
				List.of(answer), Map.of("requestor", requestor, "answer", answer)
		);
	}

	@Test
	void testSimpleExampleWithInMemoryDB() {
		InMemoryGraphNode tag = new InMemoryGraphNode("t", "Tag", Map.of());

		InMemoryGraphNode q1 = new InMemoryGraphNode("q1", "Question", Map.of());
		InMemoryGraphNode q2 = new InMemoryGraphNode("q2", "Question", Map.of());

		InMemoryGraphNode u1 = new InMemoryGraphNode("u1", "User", Map.of());
		InMemoryGraphNode u2 = new InMemoryGraphNode("u2", "User", Map.of());

		InMemoryGraphNode u1q1 = new InMemoryGraphNode("u1q1", "Comment", Map.of());
		InMemoryGraphNode u1q2 = new InMemoryGraphNode("u1q2", "Comment", Map.of());
		InMemoryGraphNode u2q1 = new InMemoryGraphNode("u2q1", "Comment", Map.of());
		InMemoryGraphNode u2q2 = new InMemoryGraphNode("u2q2", "Comment", Map.of());

		InMemoryGraph graph = new InMemoryGraph(
				List.of(tag, q1, q2, u1, u2, u1q1, u1q2, u2q1, u2q2),
				List.of(
						new InMemoryGraphEdge(q1, tag, "q1T", "TAGGED", Map.of()),
						new InMemoryGraphEdge(q2, tag, "q2T", "TAGGED", Map.of()),
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

		Set<List<InMemoryGraphNode>> results = GPEval.evaluate(graph, createCommentsToSameQuestionInTagPattern(tag.id()));
		assertEquals(4, results.size());
		assertEquals(
				Set.of(
						List.of(u1q1, u2q1, u1q2, u2q2),
						List.of(u1q2, u2q2, u1q1, u2q1),
						List.of(u2q1, u1q1, u2q2, u1q2),
						List.of(u2q2, u1q2, u2q1, u1q1)
				), results
		);

	}

	@Test
	void testFindCommentsFromSameUsersToQuestionsInTag() {
		try(Transaction tx = database.beginTx()){
			long expectedElementCount;
			try(Result testResult = tx.execute(
					"""
							MATCH (t:Tag{name:$tagName})<-[:TAGGED]-(q1:Question)<-[:COMMENTED_ON]-(u1c1:Comment)<-[:COMMENTED]-(u1:User)
							MATCH                                   (q1:Question)<-[:COMMENTED_ON]-(u2c1:Comment)<-[:COMMENTED]-(u2:User)
							MATCH                (t:Tag)<-[:TAGGED]-(q2:Question)<-[:COMMENTED_ON]-(u1c2:Comment)<-[:COMMENTED]-(u1:User)
							MATCH                                   (q2:Question)<-[:COMMENTED_ON]-(u2c2:Comment)<-[:COMMENTED]-(u2:User)
							WHERE q1 <> q2 AND u1 <> u2
							RETURN u1c1, u2c1, u1c2, u2c2
							""",
					Map.of("tagName", "neo4j")
			)){
				expectedElementCount = testResult.stream().count();
			}

			Node tagNode = tx.findNode(Neo4JSetup.TAG, "name", "neo4j");
			GraphPattern pattern = createCommentsToSameQuestionInTagPattern(tagNode.getElementId());
			Set<List<Neo4jNode>> results = assertTimeout(Duration.ofSeconds(30), () -> GPEval.evaluate(new Neo4jDB(tx), pattern));
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

	private GraphPattern createCommentsToSameQuestionInTagPattern(String tagId) {
		GPNode tag = new GPNode("tag", Neo4JSetup.TAG.name());

		GPNode user1 = new GPNode("user1", Neo4JSetup.USER.name());
		GPNode user2 = new GPNode("user2", Neo4JSetup.USER.name());

		GPNode question1 = new GPNode("question1", Neo4JSetup.QUESTION.name());
		GPNode user1Comment1 = new GPNode("user1Comment1", Neo4JSetup.COMMENT.name());
		GPNode user2Comment1 = new GPNode("user2Comment1", Neo4JSetup.COMMENT.name());

		GPNode question2 = new GPNode("question2", Neo4JSetup.QUESTION.name());
		GPNode user1Comment2 = new GPNode("user1Comment2", Neo4JSetup.COMMENT.name());
		GPNode user2Comment2 = new GPNode("user2Comment2", Neo4JSetup.COMMENT.name());

		GPGraph graph = new GPGraph(
				List.of(
						tag, user1, user2, question1, question2, user1Comment1, user1Comment2, user2Comment1, user2Comment2
				),
				List.of(
						new GPEdge(question1, tag, null, Neo4JSetup.RelType.TAGGED.name()),
						new GPEdge(question2, tag, null, Neo4JSetup.RelType.TAGGED.name()),

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
						tag, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute(tagId)))
				),
				Map.of(),
				List.of(user1Comment1, user2Comment1, user1Comment2, user2Comment2),
				Map.of("firstUser", user1, "secondUser", user2)
		);
	}
	
	@Test
	void testSelfAnswers() {
		try(Transaction tx = database.beginTx()){
			Set<List<Neo4jNode>> expectedResult;
			try(Result testResult = tx.execute(
					"""
							MATCH (t:Tag{name:$tagName})<-[:TAGGED]-(q:Question)
							MATCH (u:User)-[:ASKED]->(q:Question)<-[:ANSWERED]-(a:Answer)<-[:PROVIDED]-(u:User)
							RETURN a
							""",
					Map.of("tagName", "neo4j")
			)){
				expectedResult = testResult
					.stream()
					.map(res -> (Node) res.get("a"))
					.map(Neo4jNode::new)
					.map(List::of)
					.collect(Collectors.toSet());
			}
			GraphPattern pattern = createSelfAnswerPattern(tx.findNode(Neo4JSetup.TAG, "name", "neo4j").getElementId());
			Set<List<Neo4jNode>> result = GPEval.evaluate(new Neo4jDB(tx), pattern);
			assertNotEquals(0, result.size());
			assertEquals(expectedResult, result);
		}
	}
	
	private GraphPattern createSelfAnswerPattern(String elementId) {
		GPNode tagNode = new GPNode("tag", Neo4JSetup.TAG.name());
		GPNode userNode = new GPNode("user", Neo4JSetup.USER.name());
		GPNode questionNode = new GPNode("question", Neo4JSetup.QUESTION.name());
		GPNode answerNode = new GPNode("answer", Neo4JSetup.ANSWER.name());
		GPGraph graph = new GPGraph(
				List.of(tagNode, userNode, questionNode, answerNode),
				List.of(
						new GPEdge(questionNode, tagNode, null, Neo4JSetup.RelType.TAGGED.name()),
						new GPEdge(userNode, questionNode, null, Neo4JSetup.RelType.ASKED.name()),
						new GPEdge(answerNode, questionNode, null, Neo4JSetup.RelType.ANSWERED.name()),
						new GPEdge(userNode, answerNode, null, Neo4JSetup.RelType.PROVIDED.name())
				)
		);
		
		return new GraphPattern(
				graph,
				List.of(),
				Map.of(tagNode, List.of(new AttributeRequirement(ID_KEY, EQUAL, attribute(elementId)))),
				Map.of(),
				List.of(answerNode), Map.of()
		);
	}
	
}

