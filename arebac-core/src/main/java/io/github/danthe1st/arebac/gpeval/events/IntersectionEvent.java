package io.github.danthe1st.arebac.gpeval.events;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Name;

@Name("io.github.danthe1st.arebac.gpeval.events.IntersectionEvent")
@Description("intersection computation during forward checking")
@Category("AReBAC")
public class IntersectionEvent extends Event {
	@Name("receiverCountBefore")
	@Description("element count of the receiver before the retainAll() call")
	private int candidatesBefore;
	@Name("receiverCountAfter")
	@Description("element count of the receiver after the retainAll() call")
	private int candidateCountAfter;
	@Name("argumentCount")
	@Description("element count of the collection passed to retainAll()")
	private int neighborsCount;
	
	public void setCandidatesCountBefore(int receiverCountBefore) {
		this.candidatesBefore = receiverCountBefore;
	}
	
	public void setCandidateCountAfter(int receiverCountAfter) {
		this.candidateCountAfter = receiverCountAfter;
	}
	
	public void setNeighborsCount(int argumentCount) {
		this.neighborsCount = argumentCount;
	}
}
