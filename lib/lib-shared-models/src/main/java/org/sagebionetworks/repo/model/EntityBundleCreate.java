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
	public static int ACL					= EntityBundle.ACL;
	public static int ACCESS_REQUIREMENT	= EntityBundle.ACCESS_REQUIREMENTS;
	
	private static final String JSON_ENTITY = EntityBundle.JSON_ENTITY;
	private static final String JSON_ENTITY_TYPE = EntityBundle.JSON_ENTITY_TYPE;
	private static final String JSON_ANNOTATIONS = EntityBundle.JSON_ANNOTATIONS;
	private static final String JSON_ACL = EntityBundle.JSON_ACL;
	private static final String JSON_ACCESS_REQUIREMENT = EntityBundle.JSON_ACCESS_REQUIREMENTS;

	private Entity entity;
	private String entityType;
	private Annotations annotations;
	private AccessControlList acl;
	private AccessRequirement accessRequirement;

	public AccessRequirement getAccessRequirement() {
		return accessRequirement;
	}

	public void setAccessRequirement(AccessRequirement accessRequirement) {
		this.accessRequirement = accessRequirement;
	}

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
			entity = (Entity) EntityInstanceFactory.singleton().newInstance(entityType);
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
				acl = new AccessControlList();
			acl.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_ACCESS_REQUIREMENT)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_ACCESS_REQUIREMENT);
			String contentType = joa.getString("concreteType");
			accessRequirement = AccessRequirementInstanceFactory.singleton().newInstance(contentType);
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
		if (accessRequirement!=null) {
			JSONObjectAdapter joa = writeTo.createNew();
			accessRequirement.writeToJSONObject(joa);
			writeTo.put(JSON_ACCESS_REQUIREMENT, joa);
		}
		return writeTo;
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
		return "EntityBundleCreate [entity=" + entity + ", entityType="
				+ entityType + ", annotations=" + annotations + ", acl=" + acl
				+ ", accessRequirement=" + accessRequirement + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((accessRequirement == null) ? 0 : accessRequirement
						.hashCode());
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
		if (accessRequirement == null) {
			if (other.accessRequirement != null)
				return false;
		} else if (!accessRequirement.equals(other.accessRequirement))
			return false;
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
