package io.github.danthe1st.arebac.neo4j.tests;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement.ID_KEY;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator.EQUAL;
import static io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup.ANSWER;
import static io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup.COMMENT;
import static io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup.QUESTION;
import static io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup.TAG;
import static io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup.USER;
import static io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup.RelType.ANSWERED;
import static io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup.RelType.ASKED;
import static io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup.RelType.COMMENTED;
import static io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup.RelType.COMMENTED_ON;
import static io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup.RelType.PROVIDED;
import static io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup.RelType.TAGGED;
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
import io.github.danthe1st.arebac.jfr.JFRRecordedGraphNode;
import io.github.danthe1st.arebac.jfr.JFRRecordedGraphWrapper;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jDB;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jNode;
import io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup.RelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
			GraphPattern pattern = createFindAnswersPattern(6309);
			Set<List<Neo4jNode>> results = GPEval.evaluate(dbAsGraph, pattern);
			assertNotEquals(0, results.size());
			Set<List<Neo4jNode>> expectedAnswers = new HashSet<>();
			ResourceIterable<Relationship> rels = someUserNode.getRelationships(Direction.OUTGOING, PROVIDED);
			for(Relationship relationship : rels){
				Node otherNode = relationship.getOtherNode(someUserNode);
				expectedAnswers.add(List.of(new Neo4jNode(otherNode)));
			}
			assertEquals(expectedAnswers, results);
		}
	}

	private GraphPattern createFindAnswersPattern(int userUUID) {
		GPNode requestor = new GPNode("requestor", USER.name());
		GPNode answer = new GPNode("answer", ANSWER.name());
		return new GraphPattern(
				new GPGraph(
						List.of(requestor, answer),
						List.of(new GPEdge(requestor, answer, null, PROVIDED.name()))
				),
				List.of(),
				Map.of(requestor, List.of(new AttributeRequirement("uuid", EQUAL, attribute(userUUID)))),
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

		Set<List<InMemoryGraphNode>> results = GPEval.evaluate(graph, createCommentsToSameQuestionInTagPattern(ID_KEY, tag.id()));
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

	@ParameterizedTest
	@ValueSource(strings = { "cypher", "neo4j" })
	void testFindCommentsFromSameUsersToQuestionsInTag(String tagName) {
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
					Map.of("tagName", tagName)
			)){
				expectedElementCount = testResult.stream().count();
			}

			GraphPattern pattern = createCommentsToSameQuestionInTagPattern("name", tagName);
			Set<List<JFRRecordedGraphNode<Neo4jNode>>> results = assertTimeout(Duration.ofSeconds(30), () -> GPEval.evaluate(new JFRRecordedGraphWrapper<>(new Neo4jDB(tx)), pattern));
			assertNotEquals(0, results.size());
			assertEquals(expectedElementCount, results.size());
			for(List<JFRRecordedGraphNode<Neo4jNode>> result : results){
				Neo4jNode user1Comment1 = result.get(0).getInternalNode();
				Neo4jNode user2Comment1 = result.get(1).getInternalNode();
				Neo4jNode user1Comment2 = result.get(2).getInternalNode();
				Neo4jNode user2Comment2 = result.get(3).getInternalNode();
				checkHasSingleRelationToSameNode(user1Comment1, user2Comment1, COMMENTED_ON);
				checkHasSingleRelationToSameNode(user1Comment2, user2Comment2, COMMENTED_ON);
				checkHasSingleRelationToSameNode(user1Comment1, user1Comment2, COMMENTED);
				checkHasSingleRelationToSameNode(user2Comment1, user2Comment2, COMMENTED);

				checkHasSingleRelationToDifferentNodes(user1Comment1, user2Comment1, COMMENTED);
				checkHasSingleRelationToDifferentNodes(user1Comment2, user2Comment2, COMMENTED);
				checkHasSingleRelationToDifferentNodes(user1Comment1, user1Comment2, COMMENTED_ON);
				checkHasSingleRelationToDifferentNodes(user2Comment1, user2Comment2, COMMENTED_ON);
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

	static GraphPattern createCommentsToSameQuestionInTagPattern(String tagKey, String tagValue) {
		GPNode tag = new GPNode("tag", TAG.name());

		GPNode user1 = new GPNode("user1", USER.name());
		GPNode user2 = new GPNode("user2", USER.name());

		GPNode question1 = new GPNode("question1", QUESTION.name());
		GPNode user1Comment1 = new GPNode("user1Comment1", COMMENT.name());
		GPNode user2Comment1 = new GPNode("user2Comment1", COMMENT.name());

		GPNode question2 = new GPNode("question2", QUESTION.name());
		GPNode user1Comment2 = new GPNode("user1Comment2", COMMENT.name());
		GPNode user2Comment2 = new GPNode("user2Comment2", COMMENT.name());

		GPGraph graph = new GPGraph(
				List.of(
						tag, user1, user2, question1, question2, user1Comment1, user1Comment2, user2Comment1, user2Comment2
				),
				List.of(
						new GPEdge(question1, tag, null, TAGGED.name()),
						new GPEdge(question2, tag, null, TAGGED.name()),

						new GPEdge(user1, user1Comment1, null, COMMENTED.name()),
						new GPEdge(user1, user1Comment2, null, COMMENTED.name()),
						new GPEdge(user2, user2Comment1, null, COMMENTED.name()),
						new GPEdge(user2, user2Comment2, null, COMMENTED.name()),

						new GPEdge(user1Comment1, question1, null, COMMENTED_ON.name()),
						new GPEdge(user1Comment2, question2, null, COMMENTED_ON.name()),
						new GPEdge(user2Comment1, question1, null, COMMENTED_ON.name()),
						new GPEdge(user2Comment2, question2, null, COMMENTED_ON.name())
				)
		);
		return new GraphPattern(
				graph,
				List.of(
						new MutualExclusionConstraint(user1, user2),
						new MutualExclusionConstraint(question1, question2)
				),
				Map.of(
						tag, List.of(new AttributeRequirement(tagKey, EQUAL, attribute(tagValue)))
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
			GraphPattern pattern = assertTimeout(Duration.ofSeconds(1), () -> createSelfAnswerPattern("neo4j"));
			Set<List<Neo4jNode>> result = GPEval.evaluate(new Neo4jDB(tx), pattern);
			assertNotEquals(0, result.size());
			assertEquals(expectedResult, result);
		}
	}

	private GraphPattern createSelfAnswerPattern(String tagName) {
		GPNode tagNode = new GPNode("tag", TAG.name());
		GPNode userNode = new GPNode("user", USER.name());
		GPNode questionNode = new GPNode("question", QUESTION.name());
		GPNode answerNode = new GPNode("answer", ANSWER.name());
		GPGraph graph = new GPGraph(
				List.of(tagNode, userNode, questionNode, answerNode),
				List.of(
						new GPEdge(questionNode, tagNode, null, TAGGED.name()),
						new GPEdge(userNode, questionNode, null, ASKED.name()),
						new GPEdge(answerNode, questionNode, null, ANSWERED.name()),
						new GPEdge(userNode, answerNode, null, PROVIDED.name())
				)
		);

		return new GraphPattern(
				graph,
				List.of(),
				Map.of(tagNode, List.of(new AttributeRequirement("name", EQUAL, attribute(tagName)))),
				Map.of(),
				List.of(answerNode), Map.of()
		);
	}
}

