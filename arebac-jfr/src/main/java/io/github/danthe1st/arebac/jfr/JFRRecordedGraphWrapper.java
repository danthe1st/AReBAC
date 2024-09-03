package io.github.danthe1st.arebac.jfr;

import java.util.Collection;
import java.util.Objects;

import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedGraph;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedGraphEdge;
import io.github.danthe1st.arebac.data.commongraph.attributed.AttributedNode;
import io.github.danthe1st.arebac.jfr.events.FindEdgesEvent;
import io.github.danthe1st.arebac.jfr.events.FindEdgesEvent.Direction;
import io.github.danthe1st.arebac.jfr.events.FindNodeEvent;

public class JFRRecordedGraphWrapper<N extends AttributedNode, E extends AttributedGraphEdge<N>> implements AttributedGraph<JFRRecordedGraphNode<N>, JFRRecordedGraphEdge<N, E>> {

	private final AttributedGraph<N, E> graph;
	
	public JFRRecordedGraphWrapper(AttributedGraph<N, E> graph) {
		this.graph = graph;
	}
	
	@Override
	public JFRRecordedGraphNode<N> findNodeById(String id) {
		FindNodeEvent event = new FindNodeEvent(id);
		event.begin();
		N node = graph.findNodeById(id);
		event.commit();
		return new JFRRecordedGraphNode<>(node);
	}
	
	@Override
	public Collection<JFRRecordedGraphEdge<N, E>> findOutgoingEdges(JFRRecordedGraphNode<N> node) {
		FindEdgesEvent event = new FindEdgesEvent(node.id(), Direction.OUTGOING);
		event.begin();
		Collection<E> outgoingEdges = graph.findOutgoingEdges(node.getInternalNode());
		event.setFoundEdgesCount(outgoingEdges.size());
		event.commit();
		return outgoingEdges.stream().map(JFRRecordedGraphEdge::new).toList();
	}
	
	@Override
	public Collection<JFRRecordedGraphEdge<N, E>> findIncomingEdges(JFRRecordedGraphNode<N> node) {
		FindEdgesEvent event = new FindEdgesEvent(node.id(), Direction.INCOMING);
		event.begin();
		Collection<E> incomingEdges = graph.findIncomingEdges(node.getInternalNode());
		event.setFoundEdgesCount(incomingEdges.size());
		event.commit();
		return incomingEdges.stream().map(JFRRecordedGraphEdge::new).toList();
	}
	
	@Override
	public String toString() {
		return "JFRRecordedGraphWrapper [graph=" + graph + "]";
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(graph);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj){
			return true;
		}
		if((obj == null) || (getClass() != obj.getClass())){
			return false;
		}
		JFRRecordedGraphWrapper<?, ?> other = (JFRRecordedGraphWrapper<?, ?>) obj;
		return Objects.equals(graph, other.graph);
	}
	
}
