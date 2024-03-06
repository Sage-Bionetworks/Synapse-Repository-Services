package org.sagebionetworks.openapi.model;

import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import java.util.Map;
import java.util.Objects;

/**
 * Models the components section of an OpenAPI specification.
 * 
 * @author pharvey
 *
 */
public class Components implements JSONEntity {
	private Map<String, JsonSchema> schemas;
	private Map<String, SecurityScheme> securitySchemes;

	public Map<String, JsonSchema> getSchemas() {
		return schemas;
	}
	
	public Components withSchemas(Map<String, JsonSchema> schemas) {
		this.schemas = schemas;
		return this;
	}
	
	public Map<String, SecurityScheme> getSecuritySchemes() {
		return securitySchemes;
	}
	
	public Components withSecuritySchemes(Map<String, SecurityScheme> securitySchemes) {
		this.securitySchemes = securitySchemes;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(schemas, securitySchemes);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Components other = (Components) obj;
		return Objects.equals(schemas, other.schemas)
				&& Objects.equals(securitySchemes, other.securitySchemes);
	}

	@Override
	public String toString() {
		return "Components [schemas=" + schemas + ", securitySchemes=" + securitySchemes + "]";
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if (schemas != null) {
			JSONObjectAdapter schemas = writeTo.createNew();
			for (Map.Entry<String, JsonSchema> schema : this.schemas.entrySet()) {
				JsonSchema s = schema.getValue();
				JSONObjectAdapter newAdapter = writeTo.createNew();
				JSONObjectAdapter schemaAdapter = s.writeToJSONObject(newAdapter);
				schemas.put(schema.getKey(), schemaAdapter);
			}
			writeTo.put("schemas", schemas);
		}

		if (securitySchemes != null) {
			JSONObjectAdapter securitySchemes = writeTo.createNew();
			for (Map.Entry<String, SecurityScheme> securityScheme : this.securitySchemes.entrySet()) {
				securitySchemes.put(securityScheme.getKey(), securityScheme.getValue().writeToJSONObject(writeTo.createNew()));
			}
			writeTo.put("securitySchemes", securitySchemes);
		}

		return writeTo;
	}
}
