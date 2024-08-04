package io.github.danthe1st.arebac.neo4j.tests;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.dbms.archive.DumpFormatSelector;
import org.neo4j.dbms.archive.IncorrectFormat;
import org.neo4j.dbms.archive.Loader;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.io.fs.DefaultFileSystemAbstraction;
import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.io.layout.Neo4jLayout;

public class Neo4JSetup {
	private static final Path DB_DIRECTORY = Path.of("db");
	private static final String DB_NAME = "neo4j";

	public static Label QUESTION = Label.label("Question");
	public static Label USER = Label.label("User");
	public static Label TAG = Label.label("Tag");
	public static Label ANSWER = Label.label("Answer");
	public static Label COMMENT = Label.label("Comment");

	private static GraphDatabaseService graphDb;

	public static synchronized GraphDatabaseService getDatabase() throws IOException, IncorrectFormat, InterruptedException, URISyntaxException {
		if(graphDb != null){
			return graphDb;
		}
		boolean databaseExists = Files.exists(DB_DIRECTORY);
		DatabaseManagementService databaseManagementService = createManagementService();
		if(!databaseExists){
			databaseManagementService.shutdown();
			loadDB();
			databaseManagementService = createManagementService();
		}
		return createDB(databaseManagementService);
	}

	private static DatabaseManagementService createManagementService() {
		return new DatabaseManagementServiceBuilder(DB_DIRECTORY)
			.setConfig(GraphDatabaseSettings.read_only_database_default, true)
			.build();
	}

	private static GraphDatabaseService createDB(DatabaseManagementService databaseManagementService) {
		final DatabaseManagementService service = databaseManagementService;
		GraphDatabaseService graphDb = service.database(DB_NAME);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			service.shutdown();
		}));
		Neo4JSetup.graphDb = graphDb;
		return graphDb;
	}

	private static void loadDB() throws IOException, IncorrectFormat, InterruptedException, URISyntaxException {
		DatabaseLayout layout = DatabaseLayout.of(Neo4jLayout.of(DB_DIRECTORY), DB_NAME);
		deleteRecursively(layout.databaseDirectory());
		
		Path download = Files.createTempFile("neo4j", ".dump");

		try(PrintStream nullPrintStream = new PrintStream(OutputStream.nullOutputStream())){
			Loader loader = new Loader(new DefaultFileSystemAbstraction(), nullPrintStream);
			HttpResponse<Path> res = HttpClient
				.newBuilder()
				.followRedirects(Redirect.ALWAYS)
				.build()
				.send(
						HttpRequest.newBuilder(new URI("https://github.com/neo4j-graph-examples/stackoverflow/raw/main/data/stackoverflow-50.dump"))
							.build(),
						BodyHandlers.ofFile(download)
				);
			if(res.statusCode() != 200){
				throw new IllegalStateException("invalid status code: " + res.statusCode());
			}
			loader.load(res.body(), layout, true, false, DumpFormatSelector::decompress);
		}finally{
			Files.delete(download);
		}
	}

	private static void deleteRecursively(Path databaseDirectory) throws IOException {
		try(Stream<Path> stream = Files.walk(databaseDirectory)){
			stream
				.sorted(Comparator.reverseOrder())
				.forEach(f -> {
					try{
						Files.delete(f);
					}catch(IOException e){
						throw new UncheckedIOException(e);
					}
				});
		}catch(UncheckedIOException e){
			throw e.getCause();
		}
	}

	public enum RelType implements RelationshipType {
		ASKED, TAGGED, ANSWERED, PROVIDED, COMMENTED, COMMENTED_ON
	}

}
