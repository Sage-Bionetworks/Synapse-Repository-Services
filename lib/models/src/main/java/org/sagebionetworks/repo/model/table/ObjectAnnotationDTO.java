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
	private Long objectVersion;
	private String key;
	private AnnotationType type;
	private List<String> value;
	private boolean isDerived;
	
	public ObjectAnnotationDTO() {}

	public ObjectAnnotationDTO(ObjectDataDTO object) {
		this.objectId = object.getId();
		this.objectVersion = object.getVersion();
	}

	/**
	 * All fields constructor.
	 * 
	 * @param key
	 * @param type
	 * @param value
	 */
	public ObjectAnnotationDTO(Long objectId, Long objectVersion, String key, AnnotationType type, List<String> value, boolean isDerived) {
		this.objectId = objectId;
		this.objectVersion = objectVersion;
		this.key = key;
		this.type = type;
		this.value = value;
		this.isDerived = isDerived;
	}
	
	public ObjectAnnotationDTO(Long objectId, Long objectVersion, String key, AnnotationType type, List<String> value) {
		this(objectId, objectVersion, key, type, value, false);
	}

	public Long getObjectId() {
		return objectId;
	}

	/**
	 * @return the objectVersion
	 */
	public Long getObjectVersion() {
		return objectVersion;
	}

	/**
	 * @param objectVersion the objectVersion to set
	 */
	public void setObjectVersion(Long objectVersion) {
		this.objectVersion = objectVersion;
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

	public boolean isDerived() {
		return isDerived;
	}

	public void setDerived(boolean isDerived) {
		this.isDerived = isDerived;
	}

	@Override
	public int hashCode() {
		return Objects.hash(isDerived, key, objectId, objectVersion, type, value);
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
		return isDerived == other.isDerived && Objects.equals(key, other.key) && Objects.equals(objectId, other.objectId)
				&& Objects.equals(objectVersion, other.objectVersion) && type == other.type && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "ObjectAnnotationDTO [objectId=" + objectId + ", objectVersion=" + objectVersion + ", key=" + key + ", type=" + type
				+ ", value=" + value + ", isDerived=" + isDerived + "]";
	}

}
