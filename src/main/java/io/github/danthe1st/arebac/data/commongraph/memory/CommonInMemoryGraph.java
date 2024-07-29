package io.github.danthe1st.arebac.data.commongraph.memory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.github.danthe1st.arebac.data.commongraph.CommonEdge;
import io.github.danthe1st.arebac.data.commongraph.CommonNode;

/**
 * Interface for graphs fully known in memory.
 *
 * @param <N> The type of the nodes
 * @param <E> The type of the edges
 */
public interface CommonInMemoryGraph<N extends CommonNode, E extends CommonEdge<N>> {
	Map<String, N> nodes();

	Map<N, ? extends Collection<E>> outgoingEdges();
	
	Map<N, ? extends Collection<E>> incomingEdges();

	static <N extends CommonNode, E extends CommonEdge<N>> void validate(Map<String, N> nodes, Map<N, ? extends Collection<E>> outgoingEdges, Map<N, ? extends Collection<E>> incomingEdges) {
		nodes.forEach((k, v) -> {
			Objects.requireNonNull(k);
			Objects.requireNonNull(v);
			if(!k.equals(v.id())){
				throw new IllegalArgumentException();
			}
		});
		Set<E> allEdges = new HashSet<>();
		for(N node : outgoingEdges.keySet()){
			Collection<E> edges = outgoingEdges.get(node);
			allEdges.addAll(edges);
			for(E edge : edges){
				if(!node.equals(edge.source())){
					throw new IllegalArgumentException("outgoing edge node doesn't match its key");
				}
			}
		}
		int incomingEdgeCount = 0;
		for(N node : incomingEdges.keySet()){
			Collection<E> edges = incomingEdges.get(node);
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
