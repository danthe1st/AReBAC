package io.github.danthe1st.arebac.neo4j.tests.airbnb;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;
import io.github.danthe1st.arebac.gpeval.GPEval;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jDB;
import io.github.danthe1st.arebac.neo4j.graph.Neo4jNode;
import org.neo4j.dbms.archive.IncorrectFormat;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Warmup(iterations = 4, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 3, timeUnit = TimeUnit.SECONDS)
public class AirbnbBenchmark {
	
	@Benchmark
	public void scenario1GetReviewsFromHostGPEval(AirbnbState state, Blackhole bh) {
		try(Transaction tx = state.database.beginTx()){
			Neo4jDB db = new Neo4jDB(tx);
			Set<List<Neo4jNode>> result = GPEval.evaluate(db, state.nextHostPattern());
			result.forEach(bh::consume);
		}
	}
	
	@Benchmark
	public void scenario1GetReviewsFromHostNeo4J(AirbnbState state, Blackhole bh) {
		try(Transaction tx = state.database.beginTx()){
			Result result = tx.execute("""
					MATCH (r1:Reviewer)-[: WROTE]->(r2:Review) -[: REVIEWS]->(l: Listing)<-[HOSTS]-(h: Host)
					WHERE h.host_id="$hostId"
					RETURN r1,r2
					""", Map.of("hostId", state.nextHostId()));
			
			result.forEachRemaining(bh::consume);
		}
	}
	
	@Benchmark
	public void scenario1GetReviewsFromReviewerGPEval(AirbnbState state, Blackhole bh) {
		try(Transaction tx = state.database.beginTx()){
			Neo4jDB db = new Neo4jDB(tx);
			Set<List<Neo4jNode>> result = GPEval.evaluate(db, state.nextReviewerPattern());
			result.forEach(bh::consume);
		}
	}
	
	@Benchmark
	public void scenario1GetReviewsFromReviewerNeo4J(AirbnbState state, Blackhole bh) {
		try(Transaction tx = state.database.beginTx()){
			Result result = tx.execute("""
					MATCH (r:Reviewer) -[: WROTE]-> (r2: Review)
					WHERE r.reviewer_id="REVIEWER_ID"
					RETURN r
					""", Map.of("hostId", state.nextReviewerId()));
			
			result.forEachRemaining(bh::consume);
		}
	}
	
	@State(Scope.Thread)
	public static class AirbnbState {
		private final GraphDatabaseService database;
		private final List<String> hostIds = List.of("131304391", "155715332", "404944621");
		private int currentHostIndex = 0;
		private final List<String> reviewerIds = List.of("272671293", "227163707", "268281268", "31292360");
		private int currentReviewerIndex = 0;
		
		public AirbnbState() {
			try{
				database = AirbnbSetup.getDatabase();
			}catch(IOException | IncorrectFormat | InterruptedException | URISyntaxException e){
				throw new RuntimeException("cannot initialize benchmark state", e);
			}
		}
		
		public GraphPattern nextHostPattern() {
			return Scenario1Test.createAuthorizedCetAllReviewsFromHostGraphPattern(nextHostId());
		}
		
		public String nextHostId() {
			int index = currentHostIndex;
			currentHostIndex = (currentHostIndex + 1) % hostIds.size();
			return hostIds.get(index);
		}
		
		public GraphPattern nextReviewerPattern() {
			return Scenario1Test.createAuthorizedGetAllReviewsFromReviewerGraphPattern(nextReviewerId());
		}
		
		public String nextReviewerId() {
			int index = currentReviewerIndex;
			currentReviewerIndex = (currentReviewerIndex + 1) % reviewerIds.size();
			return reviewerIds.get(index);
		}
		
	}
}
