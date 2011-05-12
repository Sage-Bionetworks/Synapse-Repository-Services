package org.sagebionetworks.repo.model.jdo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jdo.JDOObjectNotFoundException;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.Authorizable;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class JDOUserGroupDAOImpl extends JDOBaseDAOImpl<UserGroup,JDOUserGroup> implements UserGroupDAOInitializingBean {
	
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void addUser(UserGroup userGroup, Long user) throws DatastoreException {
		Long key = KeyFactory.stringToKey(userGroup.getId());

		JDOUserGroup jdo = (JDOUserGroup) jdoTemplate.getObjectById(getJdoClass(), key);

		if (!jdo.getUsers().contains(user)) jdo.getUsers().add(user);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeUser(UserGroup userGroup, Long user) throws DatastoreException {
		Long key = KeyFactory.stringToKey(userGroup.getId());

		JDOUserGroup jdo = (JDOUserGroup) jdoTemplate.getObjectById(getJdoClass(), key);

		jdo.getUsers().remove(user);
	}
	
	@Transactional
	public Collection<Long> getUsers(UserGroup userGroup) throws DatastoreException  {
		Long key = KeyFactory.stringToKey(userGroup.getId());

		JDOUserGroup jdo = (JDOUserGroup) jdoTemplate.getObjectById(getJdoClass(), key);
		return jdo.getUsers();
	}

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
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserGroup createPublicGroup()  throws DatastoreException {
		Set<String> creatableTypes = new HashSet<String>();
		creatableTypes.add(JDOUser.class.getName());
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
		g.setUsers(new HashSet<Long>());
		jdoTemplate.makePersistent(g);
		// now give the group members read-access to the group itself
		try {
			addResourceIntern(g.getId(), new AuthorizableImpl(g.getId().toString(), JDOUserGroup.class.getName()), selfAccess);
		} catch (NotFoundException nfe) {
			// shouldn't happen, since we just created the object that's not found!
			throw new DatastoreException(nfe);
		}
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
	public void addResource(UserGroup userGroup, Authorizable resource, 
			Collection<AuthorizationConstants.ACCESS_TYPE> accessTypes) 
				throws NotFoundException, DatastoreException {
		Long userGroupId = KeyFactory.stringToKey(userGroup.getId());
		addResourceIntern(userGroupId, resource, accessTypes);
	}
	
	private void addResourceIntern(Long userGroupId, Authorizable resource, 
				Collection<AuthorizationConstants.ACCESS_TYPE> accessTypes) 
					throws NotFoundException, DatastoreException {

		Long resourceKey = KeyFactory.stringToKey(resource.getId());
		String type = resource.getType();
		try {
			JDOUserGroup jdo = (JDOUserGroup) jdoTemplate.getObjectById(JDOUserGroup.class, userGroupId);
			Set<JDOResourceAccess> ras = jdo.getResourceAccess();
			boolean foundit = false;
			// if you can find the reference resource, then update it...
			for (JDOResourceAccess ra: ras) {
				if (type.equals(ra.getResourceType()) && resourceKey.equals(ra.getResourceId())) {
					foundit = true;
					ra.setAccessTypeByEnum(new HashSet<AuthorizationConstants.ACCESS_TYPE>(accessTypes));
					break;
				}
			}
			// ... else add a new record for the resource, with the specified access types.
			if (!foundit) {
				JDOResourceAccess ra = new JDOResourceAccess();
				ra.setResourceType(type);
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
	public void removeResource(UserGroup userGroup, Authorizable resource) 
				throws NotFoundException, DatastoreException {

		Long key = KeyFactory.stringToKey(userGroup.getId());
		Long resourceKey = KeyFactory.stringToKey(resource.getId());
		String type = resource.getType();
		try {
			JDOUserGroup jdo = (JDOUserGroup) jdoTemplate.getObjectById(JDOUserGroup.class, key);
			Set<JDOResourceAccess> ras = jdo.getResourceAccess();
			for (JDOResourceAccess ra: ras) {
				if (type.equals(ra.getResourceType()) && resourceKey.equals(ra.getResourceId())) {
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
	public Collection<AuthorizationConstants.ACCESS_TYPE> getAccessTypes(UserGroup userGroup, Authorizable resource) 
		throws NotFoundException, DatastoreException {

		Long key = KeyFactory.stringToKey(userGroup.getId());
		Long resourceKey = KeyFactory.stringToKey(resource.getId());
		String type = resource.getType();
		try {
			JDOUserGroup jdo = (JDOUserGroup) jdoTemplate.getObjectById(JDOUserGroup.class, key);
			Set<JDOResourceAccess> ras = jdo.getResourceAccess();
			for (JDOResourceAccess ra: ras) {
				if (type.equals(ra.getResourceType()) && resourceKey.equals(ra.getResourceId())) {
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
		// ensure admin group is created, and that 'admin' is a member
		UserGroup ag = getAdminGroup();
		if (ag==null) {
			createAdminGroup();
			ag = getAdminGroup();
		}

	}
	
	
}

