package io.github.danthe1st.arebac.jfr.events;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Name(FindNodeByUniqueAttributeEvent.NAME)
@Category(JFREventConstants.CATEGORY)
@Description("find a node by a unique attribute")
public class FindNodeByUniqueAttributeEvent extends Event {
	static final String NAME = "io.github.danthe1st.arebac.jfr.events.FindNodeByUniqueAttributeEvent";

	@Label("node type")
	private String nodeType;
	
	@Label("attribute name")
	private String attributeName;
	
	@Label("attribute value")
	private String attributeValue;
	
	@Label("node found")
	private boolean found;
	
	public FindNodeByUniqueAttributeEvent(String nodeType, String attributeName, String attributeValue) {
		this.nodeType = nodeType;
		this.attributeName = attributeName;
		this.attributeValue = attributeValue;
	}

	public void setFound(boolean found) {
		this.found = found;
	}
}
