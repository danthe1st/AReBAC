package io.github.danthe1st.arebac.neo4j.graph;

import java.util.Collection;
import java.util.stream.Stream;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedGraph;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

public class Neo4jDB implements AttributedGraph<Neo4jNode, Neo4jEdge> {
	private final Transaction tx;
	
	public Neo4jDB(Transaction tx) {
		this.tx = tx;
	}
	
	@Override
	public Neo4jNode findNodeById(String id) {
		return new Neo4jNode(tx.getNodeByElementId(id));
	}
	
	@Override
	public Collection<Neo4jEdge> findOutgoingEdges(Neo4jNode node) {
		return findEdges(node, Direction.OUTGOING);
	}

	@Override
	public Collection<Neo4jEdge> findIncomingEdges(Neo4jNode node) {
		return findEdges(node, Direction.INCOMING);
	}
	
	private Collection<Neo4jEdge> findEdges(Neo4jNode node, Direction direction) {
		try(Stream<Relationship> stream = node.getDBNode().getRelationships(direction).stream()){
			return stream.map(Neo4jEdge::new).toList();
		}
	}
	
}
