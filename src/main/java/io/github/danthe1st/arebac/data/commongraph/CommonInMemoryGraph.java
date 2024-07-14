package io.github.danthe1st.arebac.data.commongraph;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public interface CommonInMemoryGraph<N extends CommonNode, E extends CommonEdge<N>> {
	Map<String, N> nodes();
	
	Map<N, List<E>> outgoingEdges();
	Map<N, List<E>> incomingEdges();
	
	static <N extends CommonNode, E extends CommonEdge<N>> void validate(Map<String, N> nodes, Map<N, List<E>> outgoingEdges, Map<N, List<E>> incomingEdges) {
		nodes.forEach((k, v) -> {
			Objects.requireNonNull(k);
			Objects.requireNonNull(v);
			if(!k.equals(v.id())){
				throw new IllegalArgumentException();
			}
		});
		Set<E> allEdges = new HashSet<>();
		for(N node : outgoingEdges.keySet()){
			List<E> edges = outgoingEdges.get(node);
			allEdges.addAll(edges);
			for(E edge : edges){
				if(!node.equals(edge.source())){
					throw new IllegalArgumentException("outgoing edge node doesn't match its key");
				}
			}
		}
		int incomingEdgeCount = 0;
		for(N node : incomingEdges.keySet()){
			List<E> edges = incomingEdges.get(node);
			incomingEdgeCount += edges.size();
			for(E edge : edges){
				if(!allEdges.contains(edge)){
					throw new IllegalArgumentException("incoming edge doesn't have corresponding outgoing edge");
				}
				if(!node.equals(edge.target())){
					throw new IllegalArgumentException("incoming edge node doesn't match its key");
				}
			}
		}
		if(incomingEdgeCount != allEdges.size()){
			throw new IllegalArgumentException("incoming edge count doesn't match outgoing edge count");
		}
	}
}
