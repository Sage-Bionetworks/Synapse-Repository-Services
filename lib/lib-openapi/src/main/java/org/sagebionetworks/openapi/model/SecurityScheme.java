package org.sagebionetworks.openapi.model;

import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import java.util.Objects;

/**
 * Models an OpenAPI Security Scheme Object.
 * 
 * @author pharvey
 *
 */
public class SecurityScheme implements JSONEntity {
	private String type;
	private String description;
	private String scheme;
	private String bearerFormat;


	public String getType() {
		return type;
	}
	
	public SecurityScheme withType(String type) {
		this.type = type;
		return this;
	}
	
	public String getDescription() {
		return description;
	}
	
	public SecurityScheme withDescription(String description) {
		this.description = description;
		return this;
	}

	public String getScheme() {
		return scheme;
	}

	public SecurityScheme withScheme(String scheme) {
		this.scheme = scheme;
		return this;
	}

	public String getBearerFormat() {
		return bearerFormat;
	}

	public SecurityScheme withBearerFormat(String bearerFormat) {
		this.bearerFormat = bearerFormat;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, description, scheme, bearerFormat);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SecurityScheme other = (SecurityScheme) obj;
		return Objects.equals(type, other.type)
				&& Objects.equals(description, other.description)
				&& Objects.equals(scheme, other.scheme)
				&& Objects.equals(bearerFormat, other.bearerFormat);
	}

	@Override
	public String toString() {
		return "SecurityScheme [type=" + type + ", description=" + description + ", scheme=" + scheme + ", bearerFormat=" + bearerFormat + "]";
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if (type == null) {
			throw new IllegalArgumentException("The 'type' is a required attribute of SecurityScheme.");
		}
		writeTo.put("type", type);

		if (description != null) {
			writeTo.put("description", description);
		}

		if (scheme != null) {
			writeTo.put("scheme", scheme);
		}

		if (bearerFormat != null) {
			writeTo.put("bearerFormat", bearerFormat);
		}

		return writeTo;
	}
}
