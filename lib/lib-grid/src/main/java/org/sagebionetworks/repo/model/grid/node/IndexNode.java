package org.sagebionetworks.repo.model.grid.node;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class IndexNode implements Node {

	private LogicalTimestamp id;
	private IndexType type;

	@Override
	public LogicalTimestamp getId() {
		return id;
	}

	public IndexType getType() {
		return type;
	}

	public IndexNode setType(IndexType type) {
		this.type = type;
		return this;
	}

	public IndexNode setId(LogicalTimestamp id) {
		this.id = id;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndexNode other = (IndexNode) obj;
		return Objects.equals(id, other.id) && type == other.type;
	}

	@Override
	public String toString() {
		return "IndexNode [id=" + id + ", type=" + type + "]";
	}

}
