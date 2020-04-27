package org.sagebionetworks.repo.model.table;

import java.util.Objects;

import org.sagebionetworks.repo.model.ObjectType;

/**
 * DTO object that includes both the object type for the view and its subtype mask
 * 
 * @author Marco Marasca
 *
 */
public class ViewScopeType {

	private ObjectType objectType;
	private Long typeMask;
	
	public ViewScopeType(ObjectType objectType, Long typeMask) {
		this.objectType = objectType;
		this.typeMask = typeMask;
	}

	public ObjectType getObjectType() {
		return objectType;
	}
	
	public Long getTypeMask() {
		return typeMask;
	}

	@Override
	public int hashCode() {
		return Objects.hash(objectType, typeMask);
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
		ViewScopeType other = (ViewScopeType) obj;
		return objectType == other.objectType && Objects.equals(typeMask, other.typeMask);
	}
	
	
	
}
