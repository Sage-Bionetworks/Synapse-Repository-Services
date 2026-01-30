package org.sagebionetworks.repo.model.grid.node;

import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * An ArrayNode represents a container node that holds an ordered list of values. The Replicated Growable Array (RGA)
 * algorithm is used to manage the contents of the array. The actual elements/contents of the array are
 * represented by {@link RGANode}s.
 */
public class ArrayNode implements Node {

	private LogicalTimestamp nodeId; // the ID of this node.
	private List<RGANode> elements; // the elements contained in this array.

	@Override
	public LogicalTimestamp getId() {
		return nodeId;
	}

	public ArrayNode setId(LogicalTimestamp nodeId) {
		this.nodeId = nodeId;
		return this;
	}

	public List<RGANode> getElements() {
		return elements;
	}

	public ArrayNode setElements(List<RGANode> elements) {
		this.elements = elements;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodeId, elements);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArrayNode other = (ArrayNode) obj;
		return Objects.equals(nodeId, other.nodeId) && Objects.equals(elements, other.elements);
	}

	@Override
	public String toString() {
		return "ArrayNode [nodeId=" + nodeId + ", elements=" + elements + "]";
	}

}
