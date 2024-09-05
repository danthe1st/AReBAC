package io.github.danthe1st.arebac.gpeval.events;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Name;

@Name("io.github.danthe1st.arebac.gpeval.ForwardCheckingEvent")
@Description("running forward checking")
@Category("AReBAC")
public class ForwardCheckingEvent extends Event {
	
	@Name("validNeighbors")
	@Description("the amount of neighbors processed that satisfy edge/attribute requirements")
	private int validNeighborsProcessed;
	
	@Name("totalNeighbors")
	@Description("the total amount of neighbors where edge/attribute requirements were checked")
	private int neighborsTotal;
	
	@Name("relevantEdges")
	@Description("the amount of graph pattern edges (incoming and outgoing edges) checked")
	private int relevantEdges;
	
	@Name("unknownEdges")
	@Description("the amount of unassigned graph pattern edges checked")
	private int unknownEdges;
	
	public void addNeighborsProcessed(int neighbors) {
		this.validNeighborsProcessed += neighbors;
	}
	
	public void addNeighborsTotal(int neighbors) {
		this.neighborsTotal += neighbors;
	}
	
	public void setRelevantEdges(int relevantEdges) {
		this.relevantEdges = relevantEdges;
	}
	
	public void addUnknownEdge() {
		unknownEdges++;
	}
}
