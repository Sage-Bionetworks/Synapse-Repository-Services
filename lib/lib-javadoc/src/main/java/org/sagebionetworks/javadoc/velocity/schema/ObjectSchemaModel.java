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
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "ObjectSchemaModel [name=" + name + ", id=" + id
				+ ", description=" + description + ", fields=" + fields
				+ ", effectiveSchema=" + effectiveSchema + ", enumValues="
				+ enumValues + "]";
	}

}
