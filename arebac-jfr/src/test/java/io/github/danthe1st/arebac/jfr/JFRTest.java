package io.github.danthe1st.arebac.jfr;

import static io.github.danthe1st.arebac.data.commongraph.attributed.AttributeValue.attribute;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.github.danthe1st.arebac.data.memory.InMemoryGraph;
import io.github.danthe1st.arebac.data.memory.InMemoryGraphEdge;
import io.github.danthe1st.arebac.data.memory.InMemoryGraphNode;

public class JFRTest {
	// run with -XX:StartFlightRecording:filename=test.jfr
	public static void main(String[] args) {
		InMemoryGraphNode internalNode = new InMemoryGraphNode("someId", "type", Map.of("attr", attribute("some attribute value")));
		InMemoryGraphNode otherInternalNode = new InMemoryGraphNode("otherId", "type", Map.of("attr", attribute("some attribute value")));
		InMemoryGraphEdge internalEdge = new InMemoryGraphEdge(internalNode, otherInternalNode, "someEdge", "eType", Map.of());
		InMemoryGraph internalGraph = new InMemoryGraph(List.of(internalNode), List.of(internalEdge));
		JFRRecordedGraphWrapper<InMemoryGraphNode, InMemoryGraphEdge> jfrGraph = new JFRRecordedGraphWrapper<>(internalGraph);
		JFRRecordedGraphNode<InMemoryGraphNode> foundNode = jfrGraph.findNodeById("someId");
		System.out.println(foundNode);
		System.out.println(jfrGraph.findIncomingEdges(foundNode));
		Collection<JFRRecordedGraphEdge<InMemoryGraphNode, InMemoryGraphEdge>> outgoingEdges = jfrGraph.findOutgoingEdges(foundNode);
		System.out.println(outgoingEdges);
		for(JFRRecordedGraphEdge<InMemoryGraphNode, InMemoryGraphEdge> jfrRecordedGraphEdge : outgoingEdges){
			System.err.println(jfrRecordedGraphEdge.getAttribute("attr"));
		}
		System.out.println(foundNode.getAttribute("attr"));
	}
}
