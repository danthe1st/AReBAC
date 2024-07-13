package io.github.danthe1st.arebac.data.graph_pattern.constraints;

import java.util.Objects;

import io.github.danthe1st.arebac.data.graph_pattern.GPNode;

public record MutualExclusionConstraint(GPNode first, GPNode second) {
	public MutualExclusionConstraint {
		Objects.requireNonNull(first);
		Objects.requireNonNull(second);
	}
}
