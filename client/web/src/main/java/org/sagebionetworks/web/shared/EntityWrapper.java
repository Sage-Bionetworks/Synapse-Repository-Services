package org.sagebionetworks.web.shared;

import org.sagebionetworks.web.shared.exceptions.RestServiceException;

import com.google.gwt.user.client.rpc.IsSerializable;

public class EntityWrapper implements IsSerializable {

	private String entityJson;
	private String entityMetadata;
	private RestServiceException restServiceException;
	
	public String getEntityJson() {
		return entityJson;
	}
	public void setEntityJson(String entityJson) {
		this.entityJson = entityJson;
	}
	public String getEntityMetadata() {
		return entityMetadata;
	}
	public void setEntityMetadata(String entityMetadata) {
		this.entityMetadata = entityMetadata;
	}
	public RestServiceException getRestServiceException() {
		return restServiceException;
	}
	public void setRestServiceException(RestServiceException restServiceException) {
		this.restServiceException = restServiceException;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((entityJson == null) ? 0 : entityJson.hashCode());
		result = prime * result
				+ ((entityMetadata == null) ? 0 : entityMetadata.hashCode());
		result = prime
				* result
				+ ((restServiceException == null) ? 0 : restServiceException
						.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntityWrapper other = (EntityWrapper) obj;
		if (entityJson == null) {
			if (other.entityJson != null)
				return false;
		} else if (!entityJson.equals(other.entityJson))
			return false;
		if (entityMetadata == null) {
			if (other.entityMetadata != null)
				return false;
		} else if (!entityMetadata.equals(other.entityMetadata))
			return false;
		if (restServiceException == null) {
			if (other.restServiceException != null)
				return false;
		} else if (!restServiceException.equals(other.restServiceException))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "EntityWrapper [entityJson=" + entityJson + ", entityMetadata="
				+ entityMetadata + ", restServiceException="
				+ restServiceException + "]";
	}
	
}
