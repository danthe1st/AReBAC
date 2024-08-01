package io.github.danthe1st.arebac.data.commongraph.attributed;

/**
 * A graph object (node or edge) with attributes.
 */
public interface AttributeAware {
	String id();
	AttributeValue<?> getAttribute(String key);
}
