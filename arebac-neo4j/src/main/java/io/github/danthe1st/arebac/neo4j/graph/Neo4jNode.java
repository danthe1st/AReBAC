package io.github.danthe1st.arebac.neo4j.graph;

import java.util.Iterator;
import java.util.Objects;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedNode;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

public class Neo4jNode implements AttributedNode {
	private static final String NODE_TYPE = "_NO_LABEL";

	private final Node node;
	
	public Neo4jNode(Node node) {
		this.node = node;
	}
	
	@Override
	public String id() {
		return node.getElementId();
	}

	@Override
	public String nodeType() {
		Iterator<Label> labelIt = node.getLabels().iterator();
		if(labelIt.hasNext()){
			return labelIt.next().name();
		}
		return NODE_TYPE;
	}

	@Override
	public AttributeValue<?> getAttribute(String key) {
		if(!node.hasProperty(key)){
			return null;
		}
		Object property = node.getProperty(key);
		return switch(property) {
		case String s -> AttributeValue.attribute(s);
		case Boolean b -> AttributeValue.attribute(b);
		case Integer l -> AttributeValue.attribute(l);
		default -> throw new UnsupportedOperationException("unknown property type");
		};
	}
	
	public Node getDBNode() {
		return node;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(node.getElementId());
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj){
			return true;
		}
		if((obj == null) || (getClass() != obj.getClass())){
			return false;
		}
		Neo4jNode other = (Neo4jNode) obj;
		return Objects.equals(node.getElementId(), other.node.getElementId());
	}
	
	@Override
	public String toString() {
		return "Neo4jNode [node=" + node + "]";
	}
	
}
