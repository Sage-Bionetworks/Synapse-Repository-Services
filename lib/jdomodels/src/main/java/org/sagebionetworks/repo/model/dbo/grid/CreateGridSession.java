package org.sagebionetworks.repo.model.dbo.grid;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.AuthorizationMode;

public class CreateGridSession {
	private Long userId;
	private String sourceId;
	private Long sourceVersion;
	private String schemaId;
	private Long owner;
	private AuthorizationMode authorizationMode;

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

	public Long getSourceVersion() {
		return sourceVersion;
	}

	public CreateGridSession setSourceVersion(Long sourceVersion) {
		this.sourceVersion = sourceVersion;
		return this;
	}

	public String getSchemaId() {
		return schemaId;
	}

	public CreateGridSession setSchemaId(String schemaId) {
		this.schemaId = schemaId;
		return this;
	}

	public Long getOwner() {
		return owner;
	}

	
	public CreateGridSession setOwner(String ownerString) {
		this.owner = ownerString != null? Long.parseLong(ownerString):null;
		return this;
	}
	
	public CreateGridSession setOwner(Long owner) {
		this.owner = owner;
		return this;
	}

	public AuthorizationMode getAuthorizationMode() {
		return authorizationMode;
	}

	public CreateGridSession setAuthorizationMode(AuthorizationMode authorizationMode) {
		this.authorizationMode = authorizationMode;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(authorizationMode, owner, schemaId, sourceId, sourceVersion, userId);
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
		return Objects.equals(authorizationMode, other.authorizationMode) && Objects.equals(owner, other.owner)
				&& Objects.equals(schemaId, other.schemaId) && Objects.equals(sourceId, other.sourceId)
				&& Objects.equals(sourceVersion, other.sourceVersion) && Objects.equals(userId, other.userId);
	}

	@Override
	public String toString() {
		return "CreateGridSession [userId=" + userId + ", sourceId=" + sourceId + ", sourceVersion=" + sourceVersion
				+ ", schemaId=" + schemaId + ", owner=" + owner + ", authorizationMode=" + authorizationMode + "]";
	}

}
