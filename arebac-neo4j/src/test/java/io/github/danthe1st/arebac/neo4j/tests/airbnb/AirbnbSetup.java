package io.github.danthe1st.arebac.neo4j.tests.airbnb;

import java.io.IOException;
import java.nio.file.Path;

import io.github.danthe1st.arebac.neo4j.tests.Neo4JSetup;
import org.neo4j.dbms.archive.IncorrectFormat;
import org.neo4j.graphdb.GraphDatabaseService;

public class AirbnbSetup {
	private static GraphDatabaseService graphDb;
	
	public static final String REVIEWER = "Reviewer";
	public static final String REVIEW = "Review";
	public static final String HOST = "Host";
	
	public static final String LISTING = "Listing";
	
	public static final String NEIGHBORHOOD = "Neighborhood";
	
	public static synchronized GraphDatabaseService getDatabase() throws IOException, IncorrectFormat {
		if(graphDb == null){
			graphDb = Neo4JSetup.createDatabase(Path.of("testdb", "airbnb"), AirbnbSetup.class.getClassLoader().getResource("testdumps/airbnb/airbnb.dump"));
		}
		return graphDb;
	}
}
