package io.github.danthe1st.arebac.jfr;

import java.util.Objects;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedNode;
import io.github.danthe1st.arebac.jfr.events.GetAttributeEvent;
import io.github.danthe1st.arebac.jfr.events.GetAttributeEvent.ElementType;

public class JFRRecordedGraphNode<N extends AttributedNode> implements AttributedNode {
	
	private final N node;
	
	public JFRRecordedGraphNode(N node) {
		this.node = node;
	}
	
	@Override
	public String id() {
		return node.id();
	}
	
	@Override
	public boolean hasNodeType(String nodeType) {
		return node.hasNodeType(nodeType);
	}
	
	@Override
	public AttributeValue<?> getAttribute(String key) {
		GetAttributeEvent event = new GetAttributeEvent(node.id(), key, ElementType.NODE);
		event.begin();
		AttributeValue<?> attribute = node.getAttribute(key);
		event.commit();
		return attribute;
	}
	
	public N getInternalNode() {
		return node;
	}
	
	@Override
	public String toString() {
		return "JFRRecordedGraphNode [node=" + node + "]";
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(node);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj){
			return true;
		}
		if((obj == null) || (getClass() != obj.getClass())){
			return false;
		}
		JFRRecordedGraphNode<?> other = (JFRRecordedGraphNode<?>) obj;
		return Objects.equals(node, other.node);
	}
	
}
