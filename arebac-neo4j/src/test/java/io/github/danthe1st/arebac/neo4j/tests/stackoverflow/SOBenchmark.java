package io.github.danthe1st.arebac.neo4j.tests.stackoverflow;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;
import static io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirementOperator.EQUAL;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.github.danthe1st.arebac.data.graph_pattern.GPEdge;
import io.github.danthe1st.arebac.data.graph_pattern.GPGraph;
import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.data.graph_pattern.constraints.AttributeRequirement;
import io.github.danthe1st.arebac.gpeval.GPEval;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jAccess;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jNode;
import org.neo4j.dbms.archive.IncorrectFormat;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Warmup(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 6, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class SOBenchmark {
	
	@Benchmark
	public void usersCommentingTogetherGPEval(SOBenchmarkState state, Blackhole bh) {
		Set<List<Neo4jNode>> result = GPEval.evaluate(state.neo4jDB, SOTest.createCommentsToSameQuestionInTagPattern("name", state.nextTagName()));
		result.forEach(bh::consume);
	}
	
	@Benchmark
	public void usersCommentingTogetherNeo4j(SOBenchmarkState state, Blackhole bh) {
		try(Result result = state.transaction.execute(
				"""
						MATCH (t:Tag{name:$tagName})<-[:TAGGED]-(q1:Question)<-[:COMMENTED_ON]-(u1c1:Comment)<-[:COMMENTED]-(u1:User)
						MATCH                                   (q1:Question)<-[:COMMENTED_ON]-(u2c1:Comment)<-[:COMMENTED]-(u2:User)
						MATCH                (t:Tag)<-[:TAGGED]-(q2:Question)<-[:COMMENTED_ON]-(u1c2:Comment)<-[:COMMENTED]-(u1:User)
						MATCH                                   (q2:Question)<-[:COMMENTED_ON]-(u2c2:Comment)<-[:COMMENTED]-(u2:User)
						WHERE q1 <> q2 AND u1 <> u2
						RETURN u1c1, u2c1, u1c2, u2c2
						""",
				Map.of("tagName", state.nextTagName())
		)){
			result.forEachRemaining(bh::consume);
		}
	}
	
	@Benchmark
	public void selfAnswersInTagGPEval(SOBenchmarkState state, Blackhole bh) {
		Set<List<Neo4jNode>> result = GPEval.evaluate(state.neo4jDB, SOTest.createSelfAnswerPatternWithTagName(state.nextTagName()));
		result.forEach(bh::consume);
	}
	
	@Benchmark
	public void selfAnswersInTagNeo4j(SOBenchmarkState state, Blackhole bh) {
		try(Result result = state.transaction.execute(
				"""
						MATCH (t:Tag{name:$tagName})<-[:TAGGED]-(q:Question)
						MATCH (u:User)-[:ASKED]->(q:Question)<-[:ANSWERED]-(a:Answer)<-[:PROVIDED]-(u:User)
						RETURN a
						""",
				Map.of("tagName", state.nextTagName())
		)){
			result.forEachRemaining(bh::consume);
		}
	}
	
	@Benchmark
	public void selfAnswersByUserGPEval(SOBenchmarkUUIDState state, Blackhole bh) {
		Set<List<Neo4jNode>> result = GPEval.evaluate(state.neo4jDB, SOTest.createSelfAnswerPatternWithUUID(state.nextUUID()));
		result.forEach(bh::consume);
	}
	
	@Benchmark
	public void selfAnswersByUserNeo4j(SOBenchmarkUUIDState state, Blackhole bh) {
		try(Result result = state.transaction.execute(
				"""
						MATCH (u:User)-[:ASKED]->(q:Question)<-[:ANSWERED]-(a:Answer)<-[:PROVIDED]-(u:User)
						WHERE u.uuid=$uuid
						RETURN a
						""",
				Map.of("uuid", state.nextUUID())
		)){
			result.forEachRemaining(bh::consume);
		}
	}
	
	@Benchmark
	public void multipleSelfConnectionsbetweenUserAndQuestionNeo4J(SOBenchmarkUUIDState state, Blackhole bh) {
		try(Result result = state.transaction.execute(
				"""
						MATCH (u:User)-[:ASKED]->(q:Question)
						MATCH (u:User)-[:ASKED]->(q:Question)
						MATCH (u:User)-[:ASKED]->(q:Question)
						MATCH (u:User)-[:ASKED]->(q:Question)
						MATCH (u:User)-[:ASKED]->(q:Question)
						MATCH (u:User)-[:ASKED]->(q:Question)
						MATCH (u:User)-[:ASKED]->(q:Question)
						WHERE u.uuid=$uuid
						RETURN q
						""",
				Map.of("uuid", state.nextUUID())
		)){
			result.forEachRemaining(bh::consume);
		}
	}
	
	@Benchmark
	public void multipleSelfConnectionsbetweenUserAndQuestionGPEval(SOBenchmarkUUIDState state, Blackhole bh) {
		GPNode user = new GPNode("u", "User");
		GPNode question = new GPNode("q", "Question");
		GraphPattern pattern = new GraphPattern(
				new GPGraph(
						List.of(user, question),
						List.of(
								new GPEdge(user, question, null, "ASKED"),
								new GPEdge(user, question, null, "ASKED"),
								new GPEdge(user, question, null, "ASKED"),
								new GPEdge(user, question, null, "ASKED"),
								new GPEdge(user, question, null, "ASKED"),
								new GPEdge(user, question, null, "ASKED"),
								new GPEdge(user, question, null, "ASKED")
						)
				),
				List.of(),
				Map.of(user, List.of(new AttributeRequirement("uuid", EQUAL, attribute(state.nextUUID())))),
				Map.of(), List.of(question), Map.of()
		);
		GPEval.evaluate(state.neo4jDB, pattern);
	}
	
	@State(Scope.Thread)
	public static class SOBenchmarkState {
		private final List<String> tagNames = List.of("neo4j", "cypher", "java");
		private int currentTagIndex = 0;
		private final GraphDatabaseService database;
		private final Transaction transaction;
		private final Neo4jAccess neo4jDB;
		
		public SOBenchmarkState() {
			try{
				database = SOSetup.getDatabase();
				transaction = database.beginTx();
				neo4jDB = new Neo4jAccess(transaction);
			}catch(IOException | IncorrectFormat | InterruptedException | URISyntaxException e){
				throw new RuntimeException(e);
			}
		}
		
		private String nextTagName() {
			int index = currentTagIndex;
			currentTagIndex = (currentTagIndex + 1) % tagNames.size();
			return tagNames.get(index);
		}
		
		@TearDown
		public void closeTransaction() {
			transaction.close();
		}
	}
	
	@State(Scope.Thread)
	public static class SOBenchmarkUUIDState {
		private final List<Integer> uuids = List.of(6554121, 12334270, 394071, 15127452);
		private int currentTagIndex = 0;
		private final GraphDatabaseService database;
		private final Transaction transaction;
		private final Neo4jAccess neo4jDB;
		
		public SOBenchmarkUUIDState() {
			try{
				database = SOSetup.getDatabase();
				transaction = database.beginTx();
				neo4jDB = new Neo4jAccess(transaction);
			}catch(IOException | IncorrectFormat | InterruptedException | URISyntaxException e){
				throw new RuntimeException(e);
			}
		}
		
		private int nextUUID() {
			int index = currentTagIndex;
			currentTagIndex = (currentTagIndex + 1) % uuids.size();
			return uuids.get(index);
		}
		
		@TearDown
		public void closeTransaction() {
			transaction.close();
		}
	}
}
