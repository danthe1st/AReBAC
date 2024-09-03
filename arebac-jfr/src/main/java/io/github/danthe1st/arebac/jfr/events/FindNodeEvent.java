package io.github.danthe1st.arebac.jfr.events;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Name(FindNodeEvent.NAME)
@Category(JFREventConstants.CATEGORY)
@Description("find a node by its ID")
public class FindNodeEvent extends Event {
	static final String NAME = "io.github.danthe1st.arebac.jfr.events.FindNodeEvent";

	@Label("node ID")
	private String nodeId;

	public FindNodeEvent(String nodeId) {
		this.nodeId = nodeId;
	}
}
