package org.sagebionetworks.repo.model.ses;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public class SESJsonObject {

	private Map<String, Object> otherProperties;

	@JsonAnySetter
	public void setOtherProperty(String property, Object value) {
		if (otherProperties == null) {
			otherProperties = new LinkedHashMap<>();
		}
		otherProperties.put(property, value);
	}

	public Map<String, Object> getOtherProperties() {
		return otherProperties;
	}
	
	public void setOtherProperties(Map<String, Object> otherProperties) {
		this.otherProperties = otherProperties;
	}

	@Override
	public int hashCode() {
		return Objects.hash(otherProperties);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SESJsonObject other = (SESJsonObject) obj;
		return Objects.equals(otherProperties, other.otherProperties);
	}

	@Override
	public String toString() {
		return "SESJsonObject [otherProperties=" + otherProperties + "]";
	}

}
