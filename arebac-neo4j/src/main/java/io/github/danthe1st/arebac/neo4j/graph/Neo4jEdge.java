package io.github.danthe1st.arebac.neo4j.graph;

import java.util.Objects;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedEdge;
import org.neo4j.graphdb.Relationship;

public class Neo4jEdge implements AttributedEdge<Neo4jNode> {
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
	public boolean hasEdgeType(String edgeType) {
		return relationship.getType().name().equals(edgeType);
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
	
	@Override
	public int hashCode() {
		return Objects.hash(relationship.getElementId());
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj){
			return true;
		}
		if((obj == null) || (getClass() != obj.getClass())){
			return false;
		}
		Neo4jEdge other = (Neo4jEdge) obj;
		return Objects.equals(relationship.getElementId(), other.relationship.getElementId());
	}
}
