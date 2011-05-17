package org.sagebionetworks.repo.model.jdo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.JDOObjectNotFoundException;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class JDOGroupPermissionsDAOImpl extends JDOBaseDAOImpl<UserGroup,JDOUserGroup> implements GroupPermissionsDAOInitializingBean {
	
	UserGroup newDTO() {
		UserGroup ug = new UserGroup();
		ug.setCreatableTypes(new HashSet<String>());
		return ug;
	}

	JDOUserGroup newJDO() {
		JDOUserGroup g = new JDOUserGroup();
		g.setCreatableTypes(new HashSet<String>());
		return g;
	}

	void copyToDto(JDOUserGroup jdo, UserGroup dto)
			throws DatastoreException {
		dto.setId(jdo.getId() == null ? null : KeyFactory.keyToString(jdo
				.getId()));
		dto.setName(jdo.getName());
		dto.setCreationDate(jdo.getCreationDate());
		Set<String> cts = new HashSet<String>();
		// the following condition should never be false, but it happened in practice
		// so we do this step to prevent passing a null pointer to the HashSet constructor
		if (jdo.getCreatableTypes()!=null) cts.addAll(jdo.getCreatableTypes());
		dto.setCreatableTypes(new HashSet<String>(cts));
	}

	void copyFromDto(UserGroup dto, JDOUserGroup jdo)
		throws InvalidModelException {
		jdo.setName(dto.getName());
		jdo.setCreationDate(dto.getCreationDate());
		Set<String> cts = new HashSet<String>();
		if (dto.getCreatableTypes()!=null) cts.addAll(dto.getCreatableTypes());
		jdo.setCreatableTypes(cts);
	}

	Class<JDOUserGroup> getJdoClass() {
		return JDOUserGroup.class;
	}

//	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
//	public void addUser(UserGroup userGroup, Long user) throws DatastoreException {
//		Long key = KeyFactory.stringToKey(userGroup.getId());
//
//		JDOUserGroup jdo = (JDOUserGroup) jdoTemplate.getObjectById(getJdoClass(), key);
//
//		if (!jdo.getUsers().contains(user)) jdo.getUsers().add(user);
//	}
//	
//	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
//	public void removeUser(UserGroup userGroup, Long user) throws DatastoreException {
//		Long key = KeyFactory.stringToKey(userGroup.getId());
//
//		JDOUserGroup jdo = (JDOUserGroup) jdoTemplate.getObjectById(getJdoClass(), key);
//
//		jdo.getUsers().remove(user);
//	}
//	
//	@Transactional
//	public Collection<Long> getUsers(UserGroup userGroup) throws DatastoreException  {
//		Long key = KeyFactory.stringToKey(userGroup.getId());
//
//		JDOUserGroup jdo = (JDOUserGroup) jdoTemplate.getObjectById(getJdoClass(), key);
//		return jdo.getUsers();
//	}

	@Transactional
	public UserGroup getPublicGroup() throws DatastoreException {
		return getSystemGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false);
	}
	
	@Transactional
	public UserGroup getIndividualGroup(String userName) throws DatastoreException {
		return getSystemGroup(userName, true);
	}
	
	@Transactional
	public UserGroup getAdminGroup() throws DatastoreException {
		return getSystemGroup(AuthorizationConstants.ADMIN_GROUP_NAME, false);
	}


	@Transactional
	public UserGroup getSystemGroup(String name, boolean isIndividualGroup) throws DatastoreException {
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
		Collection<JDOUserGroup> ans = exec.execute(JDOUserGroup.class, 
				"isSystemGroup==true && name==pName && isIndividual==pIsIndividual",
				String.class.getName()+" pName, "+Boolean.class.getName()+" pIsIndividual",
				null,
				name, isIndividualGroup);
		if (ans.size() > 1)
			throw new IllegalStateException("Expected 0-1 but found "
					+ ans.size());
		if (ans.size() == 0)
			return null;
		JDOUserGroup jdo = ans.iterator().next();
		UserGroup dto = newDTO();
		copyToDto(jdo, dto);
		return dto;
	}
	
	/**
	 * @return the NON-system groups and the admin group (if applicable) for the list of group names
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Map<String, UserGroup> getGroupsByNames(Collection<String> groupNames) throws DatastoreException {
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
		Collection<JDOUserGroup> groups = exec.execute(JDOUserGroup.class, 
				"pGroupNames.contains(this.name) && isIndividual==false",
				// the following doesn't work...
//				"pGroupNames.contains(this.name) && ((isSystemGroup==false && isIndividual==false) || "+
//				"(isSystemGroup==true && this.name==pAdminGroupName && isIndividual==false))",
				Collection.class.getName()+" pGroupNames, "+
				String.class.getName()+" pAdminGroupName, ",
				null,
				groupNames, AuthorizationConstants.ADMIN_GROUP_NAME);
		Map<String, UserGroup> ans = new HashMap<String, UserGroup>();
		for (JDOUserGroup jdo : groups) {
			UserGroup dto = new UserGroup();
			copyToDto(jdo, dto);
			ans.put(dto.getName(), dto);
		}
		return ans;
	}


	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserGroup createPublicGroup()  throws DatastoreException {
		Set<String> creatableTypes = new HashSet<String>();
//		creatableTypes.add(JDOUser.class.getName());
		creatableTypes.add(JDOUserGroup.class.getName());
		return createSystemGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false, 
				Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[] {
				AuthorizationConstants.ACCESS_TYPE.READ, 
				AuthorizationConstants.ACCESS_TYPE.CHANGE}),
				creatableTypes);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserGroup createAdminGroup()  throws DatastoreException {
		Set<String> creatableTypes = new HashSet<String>();
		return createSystemGroup(AuthorizationConstants.ADMIN_GROUP_NAME, false,
				Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[] {
						AuthorizationConstants.ACCESS_TYPE.READ}),
				creatableTypes // for admin' group, don't have to explicitly declare types, rather anything can be created
		);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserGroup createIndividualGroup(String userName)  throws DatastoreException {
		Set<String> creatableTypes = new HashSet<String>();
		return createSystemGroup(userName, true,
				Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[] {
						AuthorizationConstants.ACCESS_TYPE.READ}),
				creatableTypes // no specific creatable types...
		);
	}
	
	public UserGroup createSystemGroup(String name, 
			boolean isIndividualGroup, 
			Collection<AuthorizationConstants.ACCESS_TYPE> selfAccess, // the type of access members have on the group itself
			Set<String> creatableTypes) throws DatastoreException {
		JDOUserGroup g = newJDO();
		g.setName(name);
		g.setCreationDate(new Date());
		g.setIsSystemGroup(true);
		g.setIsIndividual(isIndividualGroup);
		g.getCreatableTypes().addAll(creatableTypes);
		g.setResourceAccess(new HashSet<JDOResourceAccess>());
//		g.setUsers(new HashSet<Long>());
		jdoTemplate.makePersistent(g);
//		// now give the group members read-access to the group itself
//		try {
//			addResourceIntern(g.getId(), new AuthorizableImpl(g.getId().toString(), JDOUserGroup.class.getName()), selfAccess);
//		} catch (NotFoundException nfe) {
//			// shouldn't happen, since we just created the object that's not found!
//			throw new DatastoreException(nfe);
//		}
		UserGroup dto = newDTO();
		copyToDto(g, dto);
		return dto;
	}


	
	@Transactional
	public Collection<String> getCreatableTypes(UserGroup userGroup) throws NotFoundException, DatastoreException {
		Long key = KeyFactory.stringToKey(userGroup.getId());
		try {
			JDOUserGroup jdo = (JDOUserGroup) jdoTemplate.getObjectById(JDOUserGroup.class, key);
			return jdo.getCreatableTypes();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setCreatableTypes(UserGroup userGroup, Collection<String> creatableTypes) throws NotFoundException, DatastoreException {
		Long key = KeyFactory.stringToKey(userGroup.getId());
		try {
			JDOUserGroup jdo = (JDOUserGroup) jdoTemplate.getObjectById(JDOUserGroup.class, key);
			jdo.setCreatableTypes(new HashSet<String>(creatableTypes));
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}

	}

	// TODO this could be done more quickly by querying directly against JDOResourceAccess
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void addResource(UserGroup userGroup, String resourceId, 
			Collection<AuthorizationConstants.ACCESS_TYPE> accessTypes) 
				throws NotFoundException, DatastoreException {
		Long userGroupId = KeyFactory.stringToKey(userGroup.getId());
		addResourceIntern(userGroupId, resourceId, accessTypes);
	}
	
	private void addResourceIntern(Long userGroupId, String resourceId, 
				Collection<AuthorizationConstants.ACCESS_TYPE> accessTypes) 
					throws NotFoundException, DatastoreException {

		Long resourceKey = KeyFactory.stringToKey(resourceId);
//		String type = resource.getType();
		try {
			JDOUserGroup jdo = (JDOUserGroup) jdoTemplate.getObjectById(JDOUserGroup.class, userGroupId);
			Set<JDOResourceAccess> ras = jdo.getResourceAccess();
			boolean foundit = false;
			// if you can find the reference resource, then update it...
			for (JDOResourceAccess ra: ras) {
				if (/*type.equals(ra.getResourceType()) && */resourceKey.equals(ra.getResourceId())) {
					foundit = true;
					ra.setAccessTypeByEnum(new HashSet<AuthorizationConstants.ACCESS_TYPE>(accessTypes));
					break;
				}
			}
			// ... else add a new record for the resource, with the specified access types.
			if (!foundit) {
				JDOResourceAccess ra = new JDOResourceAccess();
//				ra.setResourceType(type);
				ra.setResourceId(resourceKey);
				ra.setAccessTypeByEnum(new HashSet<AuthorizationConstants.ACCESS_TYPE>(accessTypes));
				jdo.getResourceAccess().add(ra);
			}
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

	// TODO this could be done more quickly by querying directly against JDOResourceAccess
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeResource(UserGroup userGroup, String resourceId) 
				throws NotFoundException, DatastoreException {

		Long key = KeyFactory.stringToKey(userGroup.getId());
		Long resourceKey = KeyFactory.stringToKey(resourceId);
//		String type = resource.getType();
		try {
			JDOUserGroup jdo = (JDOUserGroup) jdoTemplate.getObjectById(JDOUserGroup.class, key);
			Set<JDOResourceAccess> ras = jdo.getResourceAccess();
			for (JDOResourceAccess ra: ras) {
				if (/*type.equals(ra.getResourceType()) && */resourceKey.equals(ra.getResourceId())) {
					ras.remove(ra);
					break;
				}
			}
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

	// TODO this could be done more quickly by querying directly against JDOResourceAccess
	@Transactional
	public Collection<AuthorizationConstants.ACCESS_TYPE> getAccessTypes(UserGroup userGroup, String resourceId) 
		throws NotFoundException, DatastoreException {

		Long key = KeyFactory.stringToKey(userGroup.getId());
		Long resourceKey = KeyFactory.stringToKey(resourceId);
//		String type = resource.getType();
		try {
			JDOUserGroup jdo = (JDOUserGroup) jdoTemplate.getObjectById(JDOUserGroup.class, key);
			Set<JDOResourceAccess> ras = jdo.getResourceAccess();
			for (JDOResourceAccess ra: ras) {
				if (/*type.equals(ra.getResourceType()) && */resourceKey.equals(ra.getResourceId())) {
					return ra.getAccessTypeAsEnum();
				}
			}
			return new HashSet<AuthorizationConstants.ACCESS_TYPE>();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}
	
	// initialization of UserGroups
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void afterPropertiesSet() throws Exception {
		// ensure public group is created
		UserGroup pg = getPublicGroup();
		if (pg==null) {
			createPublicGroup();				
		}
		// ensure admin group is created
		UserGroup ag = getAdminGroup();
		if (ag==null) {
			createAdminGroup();
			ag = getAdminGroup();
		}

	}
	
	/**
	 * 
	 * @return true iff one of the groups has access to the node, with the given access type
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public boolean canAccess(Collection<UserGroup> groups, String resourceId, AuthorizationConstants.ACCESS_TYPE accessType) throws NotFoundException, DatastoreException {
		
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
		Collection<JDOUserGroup> c = exec.execute(JDOUserGroup.class, 
				":pGroups.contains(this) && "+
				"this.resourceAccess.contains(vra) && "+
				"vra.resourceId==pResourceId && "+
				"vra.accessTypes.contains(pAccessType)",
				Collection.class.getName()+" pGroups, "+
				Long.class.getName() + " pResourceId"+
				String.class.getName() +" pAccessType",
				JDOResourceAccess.class.getName()+" vra",
				groups, KeyFactory.stringToKey(resourceId), accessType.name()
				);
		return c.size()>0;
	}
	
	/**
	 * @return true iff some group in the list has creation privileges to the given type
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public boolean canCreate(Collection<UserGroup> g, String creatableType)  throws NotFoundException, DatastoreException {
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
		Collection<JDOUserGroup> c = exec.execute(
				JDOUserGroup.class, "pGroups.contains(this) && (resourceAccess.contains(pCreateableType))",
				Collection.class.getName()+" pGroups, "+String.class.getName()+" pCreateableType",
				null,
				g, creatableType);
		return c.size()>0;
	}
	
	/**
	 * @param nodeId the resource whose authorization is to be removed
	 * 
	 * Removes all authorization for this resource, e.g. just before deletion.
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeAuthorization(String nodeId) throws DatastoreException {
		/**
		 * This is used, for example before deleting a resource from the system, at which time it is to be removed from all groups to which it belongs.
		 */
			// query for all groups having the resource
			try {
				JDOExecutor exec = new JDOExecutor(jdoTemplate);
				Collection<JDOResourceAccess> ras = exec.execute(JDOResourceAccess.class, "resourceId==pId", 
						java.lang.Long.class.getName()+" pId", 
						null,
						nodeId);
				for (JDOResourceAccess ra : ras) {
					ra.getOwner().getResourceAccess().remove(ra);
				}
			} catch (Exception e) {
				throw new DatastoreException(e);
			}

	}
	
//	private Map<String, String> classTables = null;
//	private String authorizationSQL = null;
	
//	private void initClassTables() {
//		JDOExecutor exec = new JDOExecutor(jdoTemplate);
//		List<Object[]> resultsSet = exec.execute("select class_name, table_name from nucleus_tables".toUpperCase());
//		classTables = new HashMap<String,String>();
//		for (Object[] row : resultsSet) {
//			if (row.length!=2) throw new IllegalStateException("Unexpected number of columns: "+row.length);
//			classTables.put((String)row[0], (String)row[1]);
//		}
//	}
	
	private String getFromClassTables(String key) {
//		if (!classTables.containsKey(key)) throw new IllegalStateException("ClassTables is missing "+key);
		return SqlConstants.getTableForClassName(key);
	}
	
	private void initAuthorizationSQL() {
//		if (classTables==null) initClassTables();
//		authorizationSQL = ("select distinct ra.resource_id from "+
//			getFromClassTables(JDOResourceAccess.class.getName())+" ra, "+
//			getFromClassTables(JDOResourceAccess.class.getName()+".accessType")+" t, "+
//			getFromClassTables(JDOUserGroup.class.getName())+" ug "+
//			"where "+
//			//"ra.owner_id_oid=ug.id and ra.id=t.id_oid and t.string_ele = :accessType and "+
//			"ra.owner_id_oid=ug.id and ra.id=t.id_oid and t.string_ele = 'READ' and "+
//			"( "+
//		//	"ug.id in :groupIdList or"+
//			"ug.id in (0,1,2) or"+
//			"(ug.is_system_group=true and ug.is_individual=false and ug.name='"+AuthorizationConstants.PUBLIC_GROUP_NAME+"')"+
//			")");
	}
	
	public static String sqlCollection(Collection<? extends Object> c) {
		StringBuffer ans = new StringBuffer("(");
		boolean first = true;
		for (Object o : c) {
			if (first) first=false; else ans.append(",");
			ans.append(o.toString());
		}
		ans.append(")");
		return ans.toString();
	}
	
	/**
	 * @return the SQL to find the root-accessible nodes that a specified user-group list can access
	 * using a specified access type
	 */
	public String authorizationSQL(AuthorizationConstants.ACCESS_TYPE accessType, List<String> groupIds) {
//		if (classTables==null) initClassTables();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select distinct ra.");
		sb.append(SqlConstants.COL_RESOURCE_ACCESS_RESOURCE_ID);
		sb.append(" from ");
		sb.append(SqlConstants.TABLE_RESOURCE_ACCESS);
		sb.append(" ra, ");
		sb.append(SqlConstants.TABLE_RESOURCE_ACCESS_TYPE);
		sb.append(" t, ");
		sb.append(SqlConstants.TABLE_USER_GROUP);
		sb.append(" ug where ra.");
		sb.append(SqlConstants.COL_RESOURCE_ACCESS_OWNER); 
		sb.append("=ug.");
		sb.append(SqlConstants.COLUMN_ID);
		sb.append(" and ra.");
		sb.append(SqlConstants.COLUMN_ID);
		sb.append("=t.");
		sb.append(SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID);
		sb.append(" and t.");
		sb.append(SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT);
		sb.append(" = '");
		sb.append(accessType.name());
		sb.append("' and (");
		if (!groupIds.isEmpty()) {
			sb.append("ug.");
			sb.append("id"); // user groud id column
			sb.append(" in ");
			sb.append(sqlCollection(groupIds));
			sb.append(" or ");
		}
		sb.append("(ug.");
		sb.append(SqlConstants.COL_USER_GROUP_IS_SYSTEM_GROUP);
		sb.append("=true and ug.");
		sb.append(SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL);
		sb.append("=false and ug.");
		sb.append(SqlConstants.COL_USER_GROUP_NAME);
		sb.append("='");
		sb.append(AuthorizationConstants.PUBLIC_GROUP_NAME);
		sb.append("'))");
		return sb.toString();
		
	}
	
}

