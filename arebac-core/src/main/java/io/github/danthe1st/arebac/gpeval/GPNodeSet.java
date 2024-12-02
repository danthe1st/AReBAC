package io.github.danthe1st.arebac.gpeval;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import io.github.danthe1st.arebac.data.graph_pattern.GPNode;
import io.github.danthe1st.arebac.data.graph_pattern.GraphPattern;

class GPNodeSet implements Iterable<GPNode> {

	static class GPNodeSetFactory {
		private final Map<String, Integer> nodeToIndexMapping;
		private final GPNode[] nodes;

		public GPNodeSetFactory(GraphPattern graphPattern) {
			Collection<GPNode> allowedNodes = graphPattern.graph().nodes().values();
			nodes = new GPNode[allowedNodes.size()];
			Map<String, Integer> mapping = new HashMap<>();
			int i = 0;
			for(GPNode node : allowedNodes){
				nodes[i] = node;
				mapping.put(node.id(), i);
				i++;
			}
			nodeToIndexMapping = Map.copyOf(mapping);
		}

		private GPNode getByIndex(int index) {
			return nodes[index];
		}

		private int getIndexFromNode(GPNode node) {
			return nodeToIndexMapping.get(node.id());
		}

		public GPNodeSet createEmpty() {
			return new GPNodeSet(this, new BitSet(nodes.length));
		}

		public GPNodeSet copyOf(Collection<GPNode> data) {
			GPNodeSet nodeSet = createEmpty();
			nodeSet.addAll(data);
			return nodeSet;
		}

		public GPNodeSet createWithSingleElement(GPNode node) {
			GPNodeSet nodeSet = createEmpty();
			nodeSet.add(node);
			return nodeSet;
		}
	}

	private GPNodeSetFactory factory;
	private BitSet data;

	private GPNodeSet(GPNodeSetFactory factory, BitSet data) {
		this.factory = factory;
		this.data = data;
	}

	public boolean contains(GPNode node) {
		return data.get(factory.getIndexFromNode(node));
	}
	
	public GPNodeSet copy() {
		return new GPNodeSet(factory, (BitSet) data.clone());
	}

	public void add(GPNode node) {
		data.set(factory.getIndexFromNode(node));
	}

	public void addAll(GPNodeSet other) {
		data.or(other.data);
	}

	public void addAll(Collection<GPNode> nodes) {
		for(GPNode node : nodes){
			add(node);
		}
	}

	public boolean isEmpty() {
		return data.isEmpty();
	}

	@Override
	public Iterator<GPNode> iterator() {
		return new Iterator<>() {

			int nextElementIndex = 0;

			@Override
			public GPNode next() {
				int index = data.nextSetBit(nextElementIndex);
				if(index == -1){
					throw new NoSuchElementException();
				}
				GPNode value = factory.getByIndex(index);
				nextElementIndex = index + 1;
				return value;
			}

			@Override
			public boolean hasNext() {
				return data.nextSetBit(nextElementIndex) != -1;
			}
		};
	}
}
