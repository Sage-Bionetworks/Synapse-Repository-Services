package org.sagebionetworks.repo.model.schema;

import java.util.Objects;

import org.apache.commons.codec.digest.DigestUtils;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;

/**
 * The normalized representation of a JsonSchema object. Includes both the
 * normalized JSON of the schema and the SHA-256 hash of the normalized JSON.
 *
 */
public final class NormalizedJsonSchema {

	final private String normalizedSchemaJson;
	final private String sha256Hex;

	public NormalizedJsonSchema(JsonSchema schema) {
		ValidateArgument.required(schema, "schema");
		try {
			normalizedSchemaJson = EntityFactory.createJSONStringForEntity(schema);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
		sha256Hex = DigestUtils.sha256Hex(normalizedSchemaJson);
	}

	/**
	 * The normalized JSON string representation of the schema.
	 * 
	 * @return the normalizedSchemaJson
	 */
	public String getNormalizedSchemaJson() {
		return normalizedSchemaJson;
	}

	/**
	 * The SHA-256 hex hash of the normalized JSON string.
	 * 
	 * @return the sha256Hex
	 */
	public String getSha256Hex() {
		return sha256Hex;
	}

	@Override
	public int hashCode() {
		return Objects.hash(normalizedSchemaJson, sha256Hex);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof NormalizedJsonSchema)) {
			return false;
		}
		NormalizedJsonSchema other = (NormalizedJsonSchema) obj;
		return Objects.equals(normalizedSchemaJson, other.normalizedSchemaJson)
				&& Objects.equals(sha256Hex, other.sha256Hex);
	}

	@Override
	public String toString() {
		return "NormalizedJsonSchema [sha256Hex=" + sha256Hex + "]";
	}

}
