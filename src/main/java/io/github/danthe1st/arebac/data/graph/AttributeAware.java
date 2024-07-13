package io.github.danthe1st.arebac.data.graph;

import java.util.Map;

import io.github.danthe1st.arebac.data.graph_pattern.AttributeValue;

public interface AttributeAware {
	String id();
	Map<String, AttributeValue<?>> attributes();
}
