package io.github.danthe1st.arebac.neo4j.graph;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedGraphEdge;
import org.neo4j.graphdb.Relationship;

public class Neo4jEdge implements AttributedGraphEdge<Neo4jNode> {
	private final Relationship relationship;
	
	public Neo4jEdge(Relationship relationship) {
		this.relationship = relationship;
	}
	
	@Override
	public Neo4jNode source() {
		return new Neo4jNode(relationship.getStartNode());
	}
	
	@Override
	public Neo4jNode target() {
		return new Neo4jNode(relationship.getEndNode());
	}
	
	@Override
	public String id() {
		return relationship.getElementId();
	}
	
	@Override
	public String edgeType() {
		return relationship.getType().name();
	}
	
	@Override
	public AttributeValue<?> getAttribute(String key) {
		if(!relationship.hasProperty(key)){
			return null;
		}
		Object property = relationship.getProperty(key);
		return switch(property) {
		case String s -> AttributeValue.attribute(s);
		case Boolean b -> AttributeValue.attribute(b);
		case Integer l -> AttributeValue.attribute(l);
		default -> throw new UnsupportedOperationException("unknown property type");
		};
	}
	
	public Relationship getDBEdge() {
		return relationship;
	}
}
