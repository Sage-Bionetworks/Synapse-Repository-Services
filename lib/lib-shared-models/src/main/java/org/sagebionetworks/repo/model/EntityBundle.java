package org.sagebionetworks.repo.model;

import java.security.acl.Permission;

import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class EntityBundle implements JSONEntity {
	
	private static final String JSON_ENTITY = "entity";
	private static final String JSON_ENTITY_TYPE = "entityType";
	private static final String JSON_ANNOTATIONS = "annotations";
	private static final String JSON_PERMISSIONS = "permissions";
	private static final String JSON_PATH = "path";
	private static final String JSON_REFERENCED_BY = "referencedBy";
	private static final String JSON_CHILD_COUNT = "childCount";
	private static final String JSON_ACL = "accessControlList";
	private static final String JSON_USERS = "users";
	private static final String JSON_GROUPS = "groups";
	
	private Entity entity;
	private String entityType;
	private Annotations annotations;
	private UserEntityPermissions permissions;
	private EntityPath path;
	private PaginatedResults<EntityHeader> referencedBy;
	private Long childCount;
	private AccessControlList acl;
	private PaginatedResults<UserProfile> users;
	private PaginatedResults<UserGroup> groups;
	
	private AutoGenFactory agf;
	
	public EntityBundle() {
		initialize();
	}
	
	public void initialize() {
		
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(
			JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		if (toInitFrom == null) {
            throw new IllegalArgumentException("org.sagebionetworks.schema.adapter.JSONObjectAdapter cannot be null");
        }
		if (agf == null) {
			agf = new AutoGenFactory();
		}		
		if (toInitFrom.has(JSON_ENTITY)) {
			entityType = toInitFrom.getString(JSON_ENTITY_TYPE);
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.get(JSON_ENTITY);
			System.out.println(entityType);
			entity = (Entity) agf.newInstance(entityType);
			entity.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_ANNOTATIONS)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.get(JSON_ANNOTATIONS);
			if (annotations == null)
				annotations = new Annotations();
			annotations.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_PERMISSIONS)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.get(JSON_PERMISSIONS);
			if (permissions == null)
				permissions = (UserEntityPermissions) agf.newInstance(UserEntityPermissions.class.getName());
			permissions.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_PATH)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.get(JSON_PATH);
			if (path == null)
				path = (EntityPath) agf.newInstance(EntityPath.class.getName());
			path.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_REFERENCED_BY)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.get(JSON_REFERENCED_BY);
			if (referencedBy == null)
				referencedBy = new PaginatedResults<EntityHeader>(EntityHeader.class);
			referencedBy.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_CHILD_COUNT)) {
			childCount = toInitFrom.getLong(JSON_CHILD_COUNT);
		}
		if (toInitFrom.has(JSON_ACL)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.get(JSON_ACL);
			if (acl == null)
				acl = (AccessControlList) agf.newInstance(AccessControlList.class.getName());
			acl.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_USERS)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.get(JSON_USERS);
			if (users == null)
				users = new PaginatedResults<UserProfile>(UserProfile.class);
			users.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_GROUPS)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.get(JSON_GROUPS);
			if (groups == null)
				groups = new PaginatedResults<UserGroup>(UserGroup.class);
			groups.initializeFromJSONObject(joa);
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
			entity.writeToJSONObject(joa).toJSONString();
			writeTo.put(JSON_ENTITY, joa);
			writeTo.put(JSON_ENTITY_TYPE, entityType);
		}
		if (annotations != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			annotations.writeToJSONObject(joa);
			writeTo.put(JSON_ANNOTATIONS, joa);
		}
		if (permissions != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			permissions.writeToJSONObject(joa);
			writeTo.put(JSON_PERMISSIONS, joa);
		}
		if (path != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			path.writeToJSONObject(joa);
			writeTo.put(JSON_PATH, joa);
		}
		if (referencedBy != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			referencedBy.writeToJSONObject(joa);
			writeTo.put(JSON_REFERENCED_BY, joa);
		}
		if (childCount != null) {
			writeTo.put(JSON_CHILD_COUNT, childCount);
		}
		if (acl != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			acl.writeToJSONObject(joa);
			writeTo.put(JSON_ACL, joa);
		}
		if (users != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			users.writeToJSONObject(joa);
			writeTo.put(JSON_USERS, joa);
		}
		if (groups != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			groups.writeToJSONObject(joa);
			writeTo.put(JSON_GROUPS, joa);
		}
		return writeTo;
	}

	@Override
	public String getJSONSchema() {
		// Auto-generated method stub
		return null;
	}

	public Entity getEntity() {
		return entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
		String s = entity.getClass().toString();
		// trim "Class " from the above String
		entityType = s.substring(s.lastIndexOf(" ") + 1);
	}

	public Annotations getAnnotations() {
		return annotations;
	}

	public void setAnnotations(Annotations annotations) {
		this.annotations = annotations;
	}

	public UserEntityPermissions getPermissions() {
		return permissions;
	}

	public void setPermissions(UserEntityPermissions permissions) {
		this.permissions = permissions;
	}

	public EntityPath getPath() {
		return path;
	}

	public void setPath(EntityPath path) {
		this.path = path;
	}

	public PaginatedResults<EntityHeader> getReferencedBy() {
		return referencedBy;
	}

	public void setReferencedBy(PaginatedResults<EntityHeader> referencedBy) {
		this.referencedBy = referencedBy;
	}

	public Long getChildCount() {
		return childCount;
	}

	public void setChildCount(Long childCount) {
		this.childCount = childCount;
	}

	public AccessControlList getAccessControlList() {
		return acl;
	}

	public void setAccessControlList(AccessControlList acl) {
		this.acl = acl;
	}

	public PaginatedResults<UserProfile> getUsers() {
		return users;
	}

	public void setUsers(PaginatedResults<UserProfile> users) {
		this.users = users;
	}

	public PaginatedResults<UserGroup> getGroups() {
		return groups;
	}

	public void setGroups(PaginatedResults<UserGroup> groups) {
		this.groups = groups;
	}

	@Override
	public String toString() {
		if (entity == null)
			return "EntityBundle (empty)";
		StringBuilder sb = new StringBuilder();
		sb.append("EntityBundle (" + entity.getName() + ") contains [");
		if (annotations != null)
			sb.append("ANNOTATIONS, ");
		if (permissions != null)
			sb.append("PERMISSIONS, ");
		if (path != null)
			sb.append("ENTITY_PATH, ");
		if (referencedBy != null)
			sb.append("ENTITY_REFERENCEDBY, ");
		if (childCount != 0)
			sb.append("CHILD_COUNT, ");
		if (acl != null)
			sb.append("ACCESS_CONTROL_LIST, ");
		if (users != null)
			sb.append("USERS, ");
		if (groups != null)
			sb.append("GROUPS, ");
		if (sb.lastIndexOf(",") >= 0)
			sb.delete(sb.length()-2, sb.length());
		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result	+ ((entity == null) ? 0 : entity.hashCode());
		result = prime * result	+ ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result	+ ((permissions == null) ? 0 : permissions.hashCode());
		result = prime * result	+ ((path == null) ? 0 : path.hashCode());
		result = prime * result	+ ((referencedBy == null) ? 0 : referencedBy.hashCode());
		result = prime * result + childCount.hashCode();
		result = prime * result + ((acl == null) ? 0 : acl.hashCode());
		result = prime * result	+ ((users == null) ? 0 : users.hashCode());
		result = prime * result	+ ((groups == null) ? 0 : groups.hashCode());
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
		EntityBundle other = (EntityBundle) obj;
		if (entity == null) {
			if (other.entity != null)
				return false;
		} else if (!entity.equals(other.entity))
			return false;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (permissions == null) {
			if (other.permissions != null)
				return false;
		} else if (!permissions.equals(other.permissions))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (referencedBy == null) {
			if (other.referencedBy != null)
				return false;
		} else if (!referencedBy.equals(other.referencedBy))
			return false;
		if (childCount != other.childCount)
			return false;
		if (acl == null) {
			if (other.acl != null)
				return false;
		} else if (!acl.equals(other.acl))
			return false;
		if (users == null) {
			if (other.users != null)
				return false;
		} else if (!users.equals(other.users))
			return false;
		if (groups == null) {
			if (other.groups != null)
				return false;
		} else if (!groups.equals(other.groups))
			return false;
		return true;
	}
}
