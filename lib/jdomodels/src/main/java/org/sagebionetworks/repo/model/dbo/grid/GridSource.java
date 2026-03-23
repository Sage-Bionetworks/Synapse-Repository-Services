package org.sagebionetworks.repo.model.dbo.grid;

import java.util.Objects;

import org.sagebionetworks.repo.model.EntityType;

public class GridSource {

	private final Long sourceId;
	private final EntityType type;

	public GridSource(Long sourceId, EntityType type) {
		super();
		this.sourceId = sourceId;
		this.type = type;
	}

	public Long getSourceId() {
		return sourceId;
	}

	public EntityType getType() {
		return type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sourceId, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GridSource other = (GridSource) obj;
		return Objects.equals(sourceId, other.sourceId) && type == other.type;
	}

	@Override
	public String toString() {
		return "GridSource [sourceId=" + sourceId + ", type=" + type + "]";
	}

}
