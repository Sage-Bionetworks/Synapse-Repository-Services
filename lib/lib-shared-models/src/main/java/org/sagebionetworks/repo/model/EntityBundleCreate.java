package org.sagebionetworks.repo.model;

import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Low-level bundle to transport an Entity and related data objects between the 
 * Synapse platform and external clients.
 * 
 * This bundle should be used for the creation of Entities; it only includes the
 * entity and its annotations and ACL.
 * 
 * @author bkng
 *
 */
public class EntityBundleCreate implements JSONEntity {
	
	/**
	 * Masks for requesting what should be included in the bundle.
	 */
	public static int ENTITY 		      	= EntityBundle.ENTITY;
	public static int ANNOTATIONS	      	= EntityBundle.ANNOTATIONS;
	public static int ACL					= EntityBundle.ACL;;
	
	private static AutoGenFactory autoGenFactory = new AutoGenFactory();
	
	private static final String JSON_ENTITY = EntityBundle.JSON_ENTITY;
	private static final String JSON_ENTITY_TYPE = EntityBundle.JSON_ENTITY_TYPE;
	private static final String JSON_ANNOTATIONS = EntityBundle.JSON_ANNOTATIONS;
	private static final String JSON_ACL = EntityBundle.JSON_ACL;

	private Entity entity;
	private String entityType;
	private Annotations annotations;
	private AccessControlList acl;

	/**
	 * Create a new EntityBundle
	 */
	public EntityBundleCreate() {}
	
	/**
	 * Create a new EntityBundle and initialize from a JSONObjectAdapter.
	 * 
	 * @param initializeFrom
	 * @throws JSONObjectAdapterException
	 */
	public EntityBundleCreate(JSONObjectAdapter initializeFrom) throws JSONObjectAdapterException {
		this();
		initializeFromJSONObject(initializeFrom);
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(
			JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		if (toInitFrom == null) {
            throw new IllegalArgumentException("org.sagebionetworks.schema.adapter.JSONObjectAdapter cannot be null");
        }	
		if (toInitFrom.has(JSON_ENTITY)) {
			entityType = toInitFrom.getString(JSON_ENTITY_TYPE);
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_ENTITY);
			entity = (Entity) autoGenFactory.newInstance(entityType);
			entity.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_ANNOTATIONS)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_ANNOTATIONS);
			if (annotations == null)
				annotations = new Annotations();
			annotations.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_ACL)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_ACL);
			if (acl == null)
				acl = (AccessControlList) autoGenFactory.newInstance(AccessControlList.class.getName());
			acl.initializeFromJSONObject(joa);
		}
		return toInitFrom;
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo)
			throws JSONObjectAdapterException {
		if (writeTo == null) {
		        throw new IllegalArgumentException("JSONObjectAdapter cannot be null");
		}
		if (entity != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			entity.writeToJSONObject(joa);
			writeTo.put(JSON_ENTITY, joa);
			writeTo.put(JSON_ENTITY_TYPE, entityType);
		}
		if (annotations != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			annotations.writeToJSONObject(joa);
			writeTo.put(JSON_ANNOTATIONS, joa);
		}
		if (acl != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			acl.writeToJSONObject(joa);
			writeTo.put(JSON_ACL, joa);
		}
		return writeTo;
	}

	@Override
	public String getJSONSchema() {
		// Auto-generated method stub
		return null;
	}

	/**
	 * Get the Entity in this bundle.
	 */
	public Entity getEntity() {
		return entity;
	}

	/**
	 * Set the Entity in this bundle.
	 */
	public void setEntity(Entity entity) {
		this.entity = entity;
		String s = entity.getClass().toString();
		// trim "Class " from the above String
		entityType = s.substring(s.lastIndexOf(" ") + 1);
	}

	/**
	 * Get the Annotations for the Entity in this bundle.
	 */
	public Annotations getAnnotations() {
		return annotations;
	}

	/**
	 * Set the Annotations for this bundle. Should correspond to the Entity in
	 * the bundle.
	 */
	public void setAnnotations(Annotations annotations) {
		this.annotations = annotations;
	}

	/**
	 * Get the AccessControlList for the Entity in this bundle.
	 */
	public AccessControlList getAccessControlList() {
		return acl;
	}

	/**
	 * Set the AccessControlList for this bundle. Should correspond to the
	 * Entity in this bundle.
	 */
	public void setAccessControlList(AccessControlList acl) {
		this.acl = acl;
	}

	@Override
	public String toString() {
		if (entity == null)
			return "EntityBundle (empty)";
		StringBuilder sb = new StringBuilder();
		sb.append("EntityBundle (" + entity.getName() + ") contains [");
		if (entity != null)
			sb.append("ENTITY");
		if (annotations != null)
			sb.append("ANNOTATIONS, ");
		if (acl != null)
			sb.append("ACCESS_CONTROL_LIST, ");
		if (sb.lastIndexOf(",") >= 0)
			sb.delete(sb.length()-2, sb.length());
		sb.append("]");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((acl == null) ? 0 : acl.hashCode());
		result = prime * result
				+ ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result + ((entity == null) ? 0 : entity.hashCode());
		result = prime * result
				+ ((entityType == null) ? 0 : entityType.hashCode());
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
		EntityBundleCreate other = (EntityBundleCreate) obj;
		if (acl == null) {
			if (other.acl != null)
				return false;
		} else if (!acl.equals(other.acl))
			return false;
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
		if (entityType == null) {
			if (other.entityType != null)
				return false;
		} else if (!entityType.equals(other.entityType))
			return false;
		return true;
	}

	

}
