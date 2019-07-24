package org.sagebionetworks.repo.model;

/**
 * Allows methods to return an entity with its annotations.
 * 
 * This object is deprecated. Please use the EntityBundle instead.
 * @author John
 *
 * @param <T>
 */
@Deprecated
public class EntityWithAnnotations <T extends Entity>{
	
	private T entity;

	public T getEntity() {
		return entity;
	}
	public void setEntity(T entity) {
		this.entity = entity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		if (entity == null) {
			if (other.entity != null)
				return false;
		} else if (!entity.equals(other.entity))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "EntityWithAnnotations [entity=" + entity + "]";
	}
	
}
