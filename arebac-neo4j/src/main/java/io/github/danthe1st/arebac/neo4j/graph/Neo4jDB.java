package io.github.danthe1st.arebac.neo4j.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedGraph;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;

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
	public Collection<Neo4jEdge> findOutgoingEdges(Neo4jNode node, String edgeType) {
		return findEdges(node, edgeType, Direction.OUTGOING);
	}

	@Override
	public Collection<Neo4jEdge> findIncomingEdges(Neo4jNode node, String edgeType) {
		return findEdges(node, edgeType, Direction.INCOMING);
	}
	
	private Collection<Neo4jEdge> findEdges(Neo4jNode node, String edgeType, Direction direction) {
		List<Neo4jEdge> edges = new ArrayList<>();
		try(ResourceIterable<Relationship> relationships = node.getDBNode().getRelationships(direction, RelationshipType.withName(edgeType))){
			for(Relationship relationship : relationships){
				edges.add(new Neo4jEdge(relationship));
			}
		}
		return edges;
	}
	
	@Override
	public boolean isAttributeUniqueForNodeType(String key, String nodeType) {
		for(ConstraintDefinition constraint : tx.schema().getConstraints(Label.label(nodeType))){
			if(constraint.isConstraintType(ConstraintType.UNIQUENESS)){
				for(String constraintKey : constraint.getPropertyKeys()){
					if(constraintKey.equals(key)){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	@Override
	public Neo4jNode getNodeByUniqueAttribute(String nodeType, String key, AttributeValue<?> value) {
		Node node = tx.findNode(Label.label(nodeType), key, value.value());
		return new Neo4jNode(node);
	}
}
