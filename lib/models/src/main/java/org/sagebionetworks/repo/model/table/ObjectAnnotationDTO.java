package org.sagebionetworks.repo.model.table;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.annotation.v2.Annotations;

/**
 * Data transfer object (DTO) for an annotation of on a synapse object, note
 * that this can contain both {@link Annotations} of an object as well as
 * additional properties that should be added to the index. If additional
 * properties are included make sure the respective {@link ColumnModel} is
 * included in default columns set returned by the service, additionally these
 * custom properties should not end up in the {@link Annotations} of the object
 * when a view is updated.
 * 
 */
public class ObjectAnnotationDTO {

	private Long objectId;
	private String key;
	private AnnotationType type;
	private List<String> value;

	public ObjectAnnotationDTO() {
	}

	/**
	 * All fields constructor.
	 * 
	 * @param key
	 * @param type
	 * @param value
	 */
	public ObjectAnnotationDTO(Long objectId, String key, AnnotationType type, List<String> value) {
		this.objectId = objectId;
		this.key = key;
		this.type = type;
		this.value = value;
	}

	public ObjectAnnotationDTO(Long objectId, String key, AnnotationType type, String value) {
		this(objectId, key, type, Collections.singletonList(value));
	}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public AnnotationType getType() {
		return type;
	}

	public void setType(AnnotationType type) {
		this.type = type;
	}

	public List<String> getValue() {
		return value;
	}

	public void setValue(List<String> value) {
		this.value = value;
	}

	public void setValue(String value) {
		setValue(Collections.singletonList(value));
	}

	@Override
	public int hashCode() {
		return Objects.hash(objectId, key, type, value);
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
		ObjectAnnotationDTO other = (ObjectAnnotationDTO) obj;
		return Objects.equals(objectId, other.objectId) && Objects.equals(key, other.key) && type == other.type
				&& Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "ObjectAnnotationDTO [objectId=" + objectId + ", key=" + key + ", type=" + type + ", value=" + value
				+ "]";
	}

}
