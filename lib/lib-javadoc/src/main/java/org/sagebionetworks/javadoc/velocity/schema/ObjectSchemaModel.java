package org.sagebionetworks.javadoc.velocity.schema;

import java.util.List;

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
	List<String> enumValues;
	String sample;
	boolean isInterface;
	List<TypeReference> knownImplementations;
	
	public List<String> getEnumValues() {
		return enumValues;
	}
	public void setEnumValues(List<String> enumValues) {
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
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result
				+ ((effectiveSchema == null) ? 0 : effectiveSchema.hashCode());
		result = prime * result
				+ ((enumValues == null) ? 0 : enumValues.hashCode());
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + (isInterface ? 1231 : 1237);
		result = prime
				* result
				+ ((knownImplementations == null) ? 0 : knownImplementations
						.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((sample == null) ? 0 : sample.hashCode());
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
		ObjectSchemaModel other = (ObjectSchemaModel) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (effectiveSchema == null) {
			if (other.effectiveSchema != null)
				return false;
		} else if (!effectiveSchema.equals(other.effectiveSchema))
			return false;
		if (enumValues == null) {
			if (other.enumValues != null)
				return false;
		} else if (!enumValues.equals(other.enumValues))
			return false;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (isInterface != other.isInterface)
			return false;
		if (knownImplementations == null) {
			if (other.knownImplementations != null)
				return false;
		} else if (!knownImplementations.equals(other.knownImplementations))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (sample == null) {
			if (other.sample != null)
				return false;
		} else if (!sample.equals(other.sample))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "ObjectSchemaModel [name=" + name + ", id=" + id
				+ ", description=" + description + ", fields=" + fields
				+ ", effectiveSchema=" + effectiveSchema + ", enumValues="
				+ enumValues + ", sample=" + sample + ", isInterface="
				+ isInterface + ", knownImplementations="
				+ knownImplementations + "]";
	}


}
