
package io.github.danthe1st.arebac.neo4j.graph;
import java.util.Objects;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedNode;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

public class Neo4jNode implements AttributedNode {
	private final Node node;

	public Neo4jNode(Node node) {
		this.node = Objects.requireNonNull(node);
	}

	@Override
	public String id() {
		return node.getElementId();
	}

	@Override
	public boolean hasNodeType(String nodeType) {
		return node.hasLabel(Label.label(nodeType));
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
		case Long l -> AttributeValue.attribute((int) (long) l);
		default -> throw new UnsupportedOperationException("unknown property type");
		};
	}

	public Node getDBNode() {
		return node;
	}

	@Override
	public int hashCode() {
		return (int) node.getId();
//		return Objects.hash(node);
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
		return Objects.equals(node, other.node);
	}

	@Override
	public String toString() {
		return "Neo4jNode [node=" + node + "]";
	}

}
