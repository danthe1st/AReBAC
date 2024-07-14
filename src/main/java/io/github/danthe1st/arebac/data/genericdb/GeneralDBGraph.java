package io.github.danthe1st.arebac.data.genericdb;

import java.util.List;

public interface GeneralDBGraph<N extends GeneralDBNode, E extends GeneralDBEdge<N>> {
	N findNodeById(String id);
	
	List<E> findOutgoingEdges(N node);
	List<E> findIncomingEdges(N node);
}
