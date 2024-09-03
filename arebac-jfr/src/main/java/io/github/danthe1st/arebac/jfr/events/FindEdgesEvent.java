package io.github.danthe1st.arebac.jfr.events;

import java.util.Objects;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Name(FindEdgesEvent.NAME)
@Category(JFREventConstants.CATEGORY)
@Description("find edges of a specified node")
public class FindEdgesEvent extends Event {
	static final String NAME = "io.github.danthe1st.arebac.jfr.events.FindEdgesEvent";

	@Label("node ID")
	String nodeId;

	@Label("outgoing")
	boolean outgoing;
	
	@Label("incoming")
	boolean incoming;
	
	@Label("foundEdgesCount")
	int foundEdgesCount;
	
	public FindEdgesEvent(String nodeId, Direction direction) {
		this.nodeId = nodeId;
		Objects.requireNonNull(direction);
		this.outgoing = direction == Direction.OUTGOING;
		this.incoming = direction == Direction.INCOMING;
	}
	
	public void setFoundEdgesCount(int foundEdgesCount) {
		this.foundEdgesCount = foundEdgesCount;
	}
	
	public enum Direction {
		INCOMING, OUTGOING
	}
}
