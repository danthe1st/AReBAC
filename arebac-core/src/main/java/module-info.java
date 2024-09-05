module arebac.core {
	requires jdk.jfr;
	
	exports io.github.danthe1st.arebac.data.commongraph;
	exports io.github.danthe1st.arebac.data.commongraph.attributed;
	exports io.github.danthe1st.arebac.data.commongraph.memory;
	
	exports io.github.danthe1st.arebac.data.graph_pattern;
	exports io.github.danthe1st.arebac.data.graph_pattern.constraints;
	
	exports io.github.danthe1st.arebac.data.memory;
	
	exports io.github.danthe1st.arebac.gpeval;
	exports io.github.danthe1st.arebac.weaving;
}
