package io.github.danthe1st.arebac.neo4j.tests.airbnb;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
	public void scenario1GetReviewsFromHostGPEvalWithWeaving(AirbnbState state, Blackhole bh) {
		try(Transaction tx = state.database.beginTx()){
			Neo4jDB db = new Neo4jDB(tx);
			Set<List<Neo4jNode>> result = GPEval.evaluate(db, state.hostPatternInfo.computeNextPattern());
			result.forEach(bh::consume);
		}
	}
	
	@Benchmark
	public void scenario1GetReviewsFromHostGPEvalWithoutWeaving(AirbnbState state, Blackhole bh) {
		try(Transaction tx = state.database.beginTx()){
			Neo4jDB db = new Neo4jDB(tx);
			Set<List<Neo4jNode>> result = GPEval.evaluate(db, state.hostPatternInfo.nextLoadedPattern());
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
					""", Map.of("hostId", state.hostPatternInfo.nextId()));
			
			result.forEachRemaining(bh::consume);
		}
	}
	
	@Benchmark
	public void scenario1GetReviewsFromReviewerGPEvalWithWeaving(AirbnbState state, Blackhole bh) {
		try(Transaction tx = state.database.beginTx()){
			Neo4jDB db = new Neo4jDB(tx);
			Set<List<Neo4jNode>> result = GPEval.evaluate(db, state.reviewerPatternInfo.computeNextPattern());
			result.forEach(bh::consume);
		}
	}
	
	@Benchmark
	public void scenario1GetReviewsFromReviewerGPEvalWithoutWeaving(AirbnbState state, Blackhole bh) {
		try(Transaction tx = state.database.beginTx()){
			Neo4jDB db = new Neo4jDB(tx);
			Set<List<Neo4jNode>> result = GPEval.evaluate(db, state.reviewerPatternInfo.nextLoadedPattern());
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
					""", Map.of("hostId", state.reviewerPatternInfo.nextId()));
			
			result.forEachRemaining(bh::consume);
		}
	}
	
	@State(Scope.Thread)
	public static class AirbnbState {
		private final GraphDatabaseService database;
		private final PatternInfo hostPatternInfo;
		private final PatternInfo reviewerPatternInfo;
		
		public AirbnbState() {
			try{
				database = AirbnbSetup.getDatabase();
				hostPatternInfo = new PatternInfo(List.of("131304391", "155715332", "404944621"), Scenario1Test::createAuthorizedCetAllReviewsFromHostGraphPattern);
				reviewerPatternInfo = new PatternInfo(List.of("272671293", "227163707", "268281268", "31292360"), Scenario1Test::createAuthorizedGetAllReviewsFromReviewerGraphPattern);
			}catch(IOException | IncorrectFormat | InterruptedException | URISyntaxException e){
				throw new RuntimeException("cannot initialize benchmark state", e);
			}
		}
		
		public PatternInfo getHostPatternInfo() {
			return hostPatternInfo;
		}
		
		public PatternInfo getReviewerPatternInfo() {
			return reviewerPatternInfo;
		}
		
	}
	
	private static class PatternInfo {
		private final List<String> ids;
		private final Function<String, GraphPattern> patternGenerator;
		private final List<GraphPattern> patterns;
		private int currentIndex = 0;
		
		public PatternInfo(List<String> ids, Function<String, GraphPattern> patternGenerator) {
			this.ids = ids;
			this.patternGenerator = patternGenerator;
			List<GraphPattern> patterns = new ArrayList<>();
			for(String id : ids){
				patterns.add(patternGenerator.apply(id));
			}
			this.patterns = List.copyOf(patterns);
		}
		
		public GraphPattern nextLoadedPattern() {
			return patterns.get(nextIndex());
		}
		
		public GraphPattern computeNextPattern() {
			return patternGenerator.apply(nextId());
		}
		
		public String nextId() {
			return ids.get(nextIndex());
		}
		
		private int nextIndex() {
			int index = currentIndex;
			currentIndex = (currentIndex + 1) % ids.size();
			return index;
		}
	}
}
