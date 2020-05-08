package org.sagebionetworks.repo.model.table;

import java.util.Objects;

/**
 * DTO object that includes both the object type for the view and its subtype mask
 * 
 * @author Marco Marasca
 *
 */
public class ViewScopeType implements HasViewObjectType {

	private ViewObjectType objectType;
	private Long typeMask;
	
	public ViewScopeType(ViewObjectType objectType, Long typeMask) {
		this.objectType = objectType;
		this.typeMask = typeMask;
	}

	@Override
	public ViewObjectType getObjectType() {
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
