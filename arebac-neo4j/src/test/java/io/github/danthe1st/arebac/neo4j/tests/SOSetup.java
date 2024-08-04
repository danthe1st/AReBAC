package io.github.danthe1st.arebac.neo4j.tests;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.neo4j.dbms.archive.IncorrectFormat;
import org.neo4j.graphdb.GraphDatabaseService;

public class SOSetup {
	private static GraphDatabaseService graphDb;
	
	public static synchronized GraphDatabaseService getDatabase() throws IOException, IncorrectFormat, InterruptedException, URISyntaxException {
		if(graphDb == null){
			graphDb = Neo4JSetup.createDatabase(Path.of("testdb", "so"), new URI("https://github.com/neo4j-graph-examples/stackoverflow/raw/main/data/stackoverflow-50.dump"));
		}
		return graphDb;
	}
}
