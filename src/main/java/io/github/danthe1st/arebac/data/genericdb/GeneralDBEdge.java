package io.github.danthe1st.arebac.data.genericdb;

import io.github.danthe1st.arebac.data.commongraph.CommonEdge;
import io.github.danthe1st.arebac.data.graph.AttributeAware;

public interface GeneralDBEdge<N extends GeneralDBNode> extends CommonEdge<N>, AttributeAware {
	
}
