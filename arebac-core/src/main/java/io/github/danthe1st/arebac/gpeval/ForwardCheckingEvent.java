package io.github.danthe1st.arebac.gpeval;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Name;

@Name("io.github.danthe1st.arebac.gpeval.ForwardCheckingEvent")
@Description("running forward checking")
@Category("AReBAC")
class ForwardCheckingEvent extends Event {
	
	@Name("valid neighbors")
	@Description("the amount of neighbors processed that satisfy edge/attribute requirements")
	private int validNeighborsProcessed;
	
	@Name("total neighbors")
	@Description("the total amount of neighbors where edge/attribute requirements were checked")
	private int neighborsTotal;
	
	void addNeighborsProcessed(int neighbors) {
		this.validNeighborsProcessed += neighbors;
	}
	
	void addNeighborsTotal(int neighbors) {
		this.neighborsTotal += neighbors;
	}
}
