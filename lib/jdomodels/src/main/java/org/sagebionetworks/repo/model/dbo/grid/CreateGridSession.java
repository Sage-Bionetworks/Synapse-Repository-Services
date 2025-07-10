package org.sagebionetworks.repo.model.dbo.grid;

import java.util.Objects;

public class CreateGridSession {
	private Long userId;
	private String sourceId;
	private String schemaId;

	public Long getUserId() {
		return userId;
	}

	public CreateGridSession setUserId(Long userId) {
		this.userId = userId;
		return this;
	}

	public String getSourceId() {
		return sourceId;
	}

	public CreateGridSession setSourceId(String sourceId) {
		this.sourceId = sourceId;
		return this;
	}

	public String getSchemaId() {
		return schemaId;
	}

	public CreateGridSession setSchemaId(String schemaId) {
		this.schemaId = schemaId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(schemaId, sourceId, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CreateGridSession other = (CreateGridSession) obj;
		return Objects.equals(schemaId, other.schemaId) && Objects.equals(sourceId, other.sourceId)
				&& Objects.equals(userId, other.userId);
	}

	@Override
	public String toString() {
		return "CreateGridSession [userId=" + userId + ", sourceId=" + sourceId + ", schemaId=" + schemaId + "]";
	}

}
