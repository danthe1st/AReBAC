package io.github.danthe1st.arebac.data.commongraph;

public interface CommonEdge<T extends CommonNode> {
	T source();
	T target();
	
	String id();
	String edgeType();
}
