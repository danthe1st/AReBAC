package io.github.danthe1st.arebac.jfr;

import java.util.Objects;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedEdge;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedNode;
import io.github.danthe1st.arebac.jfr.events.GetAttributeEvent;
import io.github.danthe1st.arebac.jfr.events.GetAttributeEvent.ElementType;

public class JFRRecordedGraphEdge<N extends AttributedNode, E extends AttributedEdge<N>> implements AttributedEdge<JFRRecordedGraphNode<N>> {

	private final E edge;

	public JFRRecordedGraphEdge(E edge) {
		this.edge = edge;
	}

	@Override
	public JFRRecordedGraphNode<N> source() {
		return new JFRRecordedGraphNode<>(edge.source());
	}

	@Override
	public JFRRecordedGraphNode<N> target() {
		return new JFRRecordedGraphNode<>(edge.target());
	}

	@Override
	public String id() {
		return edge.id();
	}

	@Override
	public boolean hasEdgeType(String edgeType) {
		return edge.hasEdgeType(edgeType);
	}

	@Override
	public AttributeValue<?> getAttribute(String key) {
		GetAttributeEvent event = new GetAttributeEvent(edge.id(), key, ElementType.EDGE);
		event.begin();
		AttributeValue<?> attribute = edge.getAttribute(key);
		event.commit();
		return attribute;
	}

	@Override
	public String toString() {
		return "JFRRecordedGraphEdge [edge=" + edge + "]";
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(edge);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj){
			return true;
		}
		if((obj == null) || (getClass() != obj.getClass())){
			return false;
		}
		JFRRecordedGraphEdge<?, ?> other = (JFRRecordedGraphEdge<?, ?>) obj;
		return Objects.equals(edge, other.edge);
	}
	
}
