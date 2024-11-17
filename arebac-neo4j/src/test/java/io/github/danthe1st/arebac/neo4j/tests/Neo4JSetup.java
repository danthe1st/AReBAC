package io.github.danthe1st.arebac.neo4j.tests;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
	private static final String DB_NAME = "neo4j";

	public static Label QUESTION = Label.label("Question");
	public static Label USER = Label.label("User");
	public static Label TAG = Label.label("Tag");
	public static Label ANSWER = Label.label("Answer");
	public static Label COMMENT = Label.label("Comment");

	public static synchronized GraphDatabaseService createDatabase(Path dbDirectory, URL dumpLocation) throws IOException, IncorrectFormat {
		boolean databaseExists = Files.exists(dbDirectory);
		DatabaseManagementService databaseManagementService = createManagementService(dbDirectory);
		if(!databaseExists){
			databaseManagementService.shutdown();
			loadDB(dbDirectory, dumpLocation);
			databaseManagementService = createManagementService(dbDirectory);
		}
		return createDB(databaseManagementService);
	}

	private static DatabaseManagementService createManagementService(Path dbDirectory) {
		return new DatabaseManagementServiceBuilder(dbDirectory)
			.setConfig(GraphDatabaseSettings.read_only_database_default, true)
			.build();
	}

	private static GraphDatabaseService createDB(DatabaseManagementService databaseManagementService) {
		final DatabaseManagementService service = databaseManagementService;
		GraphDatabaseService graphDb = service.database(DB_NAME);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			service.shutdown();
		}));
		return graphDb;
	}

	private static void loadDB(Path dbDirectory, URL dumpLocation) throws IOException, IncorrectFormat {
		DatabaseLayout layout = DatabaseLayout.of(Neo4jLayout.of(dbDirectory), DB_NAME);
		deleteRecursively(layout.databaseDirectory());

		Path download = Files.createTempFile("neo4j", ".dump");

		try(PrintStream nullPrintStream = new PrintStream(OutputStream.nullOutputStream());
				InputStream dumpInput = new BufferedInputStream(dumpLocation.openStream())){
			Loader loader = new Loader(new DefaultFileSystemAbstraction(), nullPrintStream);
			
			Files.copy(dumpInput, download, StandardCopyOption.REPLACE_EXISTING);
			loader.load(download, layout, true, false, DumpFormatSelector::decompress);
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
