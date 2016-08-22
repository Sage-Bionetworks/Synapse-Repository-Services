package org.sagebionetworks.repo.model;

/**
 * Data transfer object (DTO) for an annotation of an entity.
 * 
 */
public class AnnotationDTO {
	
	public enum Type{
		STRING,
		LONG,
		DOUBLE,
		DATE,
	}
	
	String key;
	Type type;
	String value;
	
	public AnnotationDTO(){
	}
	
	/**
	 * All fields constructor.
	 * 
	 * @param key
	 * @param type
	 * @param value
	 */
	public AnnotationDTO(String key, Type type, String value) {
		super();
		this.key = key;
		this.type = type;
		this.value = value;
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		AnnotationDTO other = (AnnotationDTO) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (type != other.type)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AnnotationDTO [key=" + key + ", type=" + type + ", value="
				+ value + "]";
	}

}
