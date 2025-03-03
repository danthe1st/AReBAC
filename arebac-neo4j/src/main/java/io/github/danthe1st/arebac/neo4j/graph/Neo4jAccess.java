package io.github.danthe1st.arebac.neo4j.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedGraph;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;

/**
 * Represents access to a Neo4J database as an {@link AttributedGraph}.
 *
 * All operations are executed using the {@link Transaction} passed to the constructor.
 *
 * The {@link Node#getElementId() element ID} from Neo4J is used as the identifier of nodes.
 *
 * This implementation supports {@link AttributedGraph#isAttributeUniqueForNodeType(String, String) unique attributes}.
 * A node attribute is considered {@link AttributedGraph#isAttributeUniqueForNodeType(String, String) unique} with respect to a given node type if a unique constraint on that attribute exists for that node type.
 * Information on unique attributes is cached. If unique constraints change after creating the {@link Neo4jAccess} instance, these changes may not be observed.
 */
public class Neo4jAccess implements AttributedGraph<Neo4jNode, Neo4jEdge> {
	private final Transaction tx;
	private final Map<String, Set<String>> uniqueAttributesPerNodeType = new HashMap<>();
	
	public Neo4jAccess(Transaction tx) {
		this.tx = tx;
	}
	
	@Override
	public Neo4jNode findNodeById(String id) {
		try{
			Node dbNode = tx.getNodeByElementId(id);
			return new Neo4jNode(dbNode);
		}catch(NotFoundException | IllegalArgumentException e){
			return null;
		}
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
		Set<String> uniqueAttributeNames = uniqueAttributesPerNodeType.get(nodeType);
		if(uniqueAttributeNames == null){
			uniqueAttributeNames = findUniqueAttributeNames(nodeType);
			uniqueAttributesPerNodeType.put(nodeType, uniqueAttributeNames);
		}
		
		return uniqueAttributeNames.contains(key);
	}

	private Set<String> findUniqueAttributeNames(String nodeType) {
		Set<String> uniqueNodeTypes = new HashSet<>();
		for(ConstraintDefinition constraint : tx.schema().getConstraints(Label.label(nodeType))){
			if(constraint.isConstraintType(ConstraintType.UNIQUENESS)){
				checkConstraint(constraint, uniqueNodeTypes);
			}
		}
		return Set.copyOf(uniqueNodeTypes);
	}

	private void checkConstraint(ConstraintDefinition constraint, Set<String> uniqueNodeTypes) {
		String attributeName = null;
		for(String constraintKey : constraint.getPropertyKeys()){
			if(attributeName != null){
				// we only want unique constraints with a single attribute
				return;
			}
			attributeName = constraintKey;
		}
		if(attributeName != null){
			uniqueNodeTypes.add(attributeName);
		}
	}
	
	@Override
	public Neo4jNode getNodeByUniqueAttribute(String nodeType, String key, AttributeValue<?> value) {
		Node node = tx.findNode(Label.label(nodeType), key, value.value());
		if(node == null){
			return null;
		}
		return new Neo4jNode(node);
	}
}
