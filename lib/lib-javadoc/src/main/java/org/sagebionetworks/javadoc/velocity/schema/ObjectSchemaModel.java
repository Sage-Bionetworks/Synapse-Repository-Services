package org.sagebionetworks.javadoc.velocity.schema;

import java.util.List;
import java.util.Objects;

import org.sagebionetworks.schema.EnumValue;

/**
 * This is the model object that contains the context data that will be rendered to the schema view.
 * 
 * @author John
 *
 */
public class ObjectSchemaModel {

	String name;
	String id;
	String description;
	List<SchemaFields> fields;
	String effectiveSchema;
	List<EnumValue> enumValues;
	String sample;
	boolean isInterface;
	List<TypeReference> knownImplementations;
	TypeReference defaultImplementation;

	public List<EnumValue> getEnumValues() {
		return enumValues;
	}

	public void setEnumValues(List<EnumValue> enumValues) {
		this.enumValues = enumValues;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<SchemaFields> getFields() {
		return fields;
	}

	public void setFields(List<SchemaFields> fields) {
		this.fields = fields;
	}

	public String getEffectiveSchema() {
		return effectiveSchema;
	}

	public void setEffectiveSchema(String effectiveSchema) {
		this.effectiveSchema = effectiveSchema;
	}

	public String getSample() {
		return sample;
	}

	public void setSample(String sample) {
		this.sample = sample;
	}

	public boolean getIsInterface() {
		return isInterface;
	}

	public void setIsInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

	public List<TypeReference> getKnownImplementations() {
		return knownImplementations;
	}

	public void setKnownImplementations(List<TypeReference> knownImplementations) {
		this.knownImplementations = knownImplementations;
	}

	public TypeReference getDefaultImplementation() {
		return defaultImplementation;
	}

	public void setDefaultImplementation(TypeReference defaultImplementation) {
		this.defaultImplementation = defaultImplementation;
	}

	@Override
	public int hashCode() {
		return Objects.hash(defaultImplementation, description, effectiveSchema, enumValues, fields, id, isInterface,
				knownImplementations, name, sample);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ObjectSchemaModel other = (ObjectSchemaModel) obj;
		return Objects.equals(defaultImplementation, other.defaultImplementation)
				&& Objects.equals(description, other.description)
				&& Objects.equals(effectiveSchema, other.effectiveSchema)
				&& Objects.equals(enumValues, other.enumValues) && Objects.equals(fields, other.fields)
				&& Objects.equals(id, other.id) && isInterface == other.isInterface
				&& Objects.equals(knownImplementations, other.knownImplementations) && Objects.equals(name, other.name)
				&& Objects.equals(sample, other.sample);
	}

	@Override
	public String toString() {
		return "ObjectSchemaModel [name=" + name + ", id=" + id + ", description=" + description + ", fields=" + fields
				+ ", effectiveSchema=" + effectiveSchema + ", enumValues=" + enumValues + ", sample=" + sample
				+ ", isInterface=" + isInterface + ", knownImplementations=" + knownImplementations
				+ ", defaultImplementation=" + defaultImplementation + "]";
	}

}
