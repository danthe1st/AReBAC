package io.github.danthe1st.arebac.data.commongraph.attributed;

import io.github.danthe1st.arebac.data.commongraph.CommonEdge;

/**
 * An edge in an {@link AttributedGraph attributed graph}.
 * @param <N> The type of the nodes
 */
public interface AttributedGraphEdge<N extends AttributedNode> extends CommonEdge<N>, AttributeAware {
	
}
