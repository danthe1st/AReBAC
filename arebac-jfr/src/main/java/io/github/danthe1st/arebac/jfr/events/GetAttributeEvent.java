package io.github.danthe1st.arebac.jfr.events;

import java.util.Objects;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Name(GetAttributeEvent.NAME)
@Category(JFREventConstants.CATEGORY)
@Description("get an attribute of a node or edge")
public class GetAttributeEvent extends Event {
	static final String NAME = "io.github.danthe1st.arebac.jfr.events.GetAttributeEvent";

	@Label("element ID")
	@Description("ID of the node or edge")
	String elementId;

	@Label("attribute name")
	String attributeName;
	
	@Label("element is node")
	@Description("true iff this event represents getting the attribute of a node")
	boolean isNode;
	@Label("element is edge")
	@Description("true iff this event represents getting the attribute of a edge")
	boolean isEdge;
	
	public GetAttributeEvent(String elementId, String attributeName, ElementType type) {
		this.elementId = elementId;
		this.attributeName = attributeName;
		Objects.requireNonNull(type);
		isNode = type == ElementType.NODE;
		isEdge = type == ElementType.EDGE;
	}
	
	public enum ElementType {
		NODE, EDGE;
	}
}
