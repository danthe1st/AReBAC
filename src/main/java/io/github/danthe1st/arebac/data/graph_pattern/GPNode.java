package io.github.danthe1st.arebac.data.graph_pattern;

import java.util.Objects;

import io.github.danthe1st.arebac.data.commongraph.CommonNode;

public record GPNode(
		String id,
		String nodeType) implements CommonNode {
	public GPNode {
		Objects.requireNonNull(id);
		Objects.requireNonNull(nodeType);
	}
}
