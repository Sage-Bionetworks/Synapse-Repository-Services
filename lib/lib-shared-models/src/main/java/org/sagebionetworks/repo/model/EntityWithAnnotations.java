package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.Entity;

/**
 * Allows methods to return an entity with its annotations.
 * @author John
 *
 * @param <T>
 */
public class EntityWithAnnotations <T extends Entity>{
	
	private T entity;
	private Annotations annotations;
	public T getEntity() {
		return entity;
	}
	public void setEntity(T entity) {
		this.entity = entity;
	}
	public Annotations getAnnotations() {
		return annotations;
	}
	public void setAnnotations(Annotations annotations) {
		this.annotations = annotations;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result + ((entity == null) ? 0 : entity.hashCode());
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
		@SuppressWarnings("rawtypes")
		EntityWithAnnotations other = (EntityWithAnnotations) obj;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (entity == null) {
			if (other.entity != null)
				return false;
		} else if (!entity.equals(other.entity))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "EntityWithAnnotations [entity=" + entity + ", annotations="
				+ annotations + "]";
	}
	
}
