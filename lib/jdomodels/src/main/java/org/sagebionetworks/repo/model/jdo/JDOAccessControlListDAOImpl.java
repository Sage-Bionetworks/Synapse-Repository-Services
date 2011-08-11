package org.sagebionetworks.repo.model.jdo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAccessControlList;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoObjectRetrievalFailureException;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class JDOAccessControlListDAOImpl implements AccessControlListDAO {
	
	@Autowired
	private UserGroupCache userGroupCache;
	
	@Autowired
	JdoTemplate jdoTemplate;

	/**
	 * Find the access control list for the given resource
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	@Transactional(readOnly = true)
	public AccessControlList getForResource(String rId) throws DatastoreException, NotFoundException {
		return get(rId);
	}
	
	AccessControlList newDTO() {
		AccessControlList dto = new AccessControlList();
		dto.setResourceAccess(new HashSet<ResourceAccess>());
		return dto;
	}

	JDOAccessControlList newJDO() {
		JDOAccessControlList jdo = new JDOAccessControlList();
		jdo.setResourceAccess(new HashSet<JDOResourceAccess>());
		return jdo;
	}

	void copyToDto(JDOAccessControlList jdo, String eTag, AccessControlList dto)
			throws DatastoreException, NotFoundException {
		AccessControlListUtil.updateDtoFromJdo(jdo, dto, eTag, userGroupCache);
	}

	void copyFromDto(AccessControlList dto, JDOAccessControlList jdo)
			throws InvalidModelException, DatastoreException, NotFoundException {
		if(dto.getId() == null) throw new InvalidModelException("Cannot set a ResourceAccess owner to null");
		JDONode owner = jdoTemplate.getObjectById(JDONode.class, KeyFactory.stringToKey(dto.getId()));
		AccessControlListUtil.updateJdoFromDto(jdo, dto, owner, userGroupCache);
	}

	Class<JDOAccessControlList> getJdoClass() {
		return JDOAccessControlList.class;
	}
	
	String defaultSortField() {
		return "id";
	}



	/**
	 * @return true iff some group in 'groups' has explicit permission to access 'resourceId' using access type 'accessType'
	 */
	@Transactional(readOnly = true)
	public boolean canAccess(Collection<UserGroup> groups, 
			String resourceId, 
			AuthorizationConstants.ACCESS_TYPE accessType) throws DatastoreException {
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
					
		Collection<Long> groupIds = new HashSet<Long>();
		for (UserGroup g : groups) groupIds.add(KeyFactory.stringToKey(g.getId()));
		
		List<JDOAccessControlList> result = exec.execute(JDOAccessControlList.class, 
				CAN_ACCESS_FILTER,
			Long.class.getName()+" pResourceId, "+
			Collection.class.getName()+" pGroups, "+
			String.class.getName()+" pAccessType",
			JDOResourceAccess.class.getName()+" vResourceAccess",
			KeyFactory.stringToKey(resourceId),
			groupIds,
			accessType.name());
		
		int resultSize = result.size();
		
		return result.size()>0;
	}

	private static final String CAN_ACCESS_FILTER = 
		"this.resource.id==pResourceId && this.resourceAccess.contains(vResourceAccess) "+
		"&& pGroups.contains(vResourceAccess.userGroupId) "+
		"&& vResourceAccess.accessType.contains(pAccessType)";

	/**
	 * @return the SQL to find the root-accessible nodes that a specified user-group list can access
	 * using a specified access type
	 * 
	 * Can't bind a collection to a variable in the string, so we have to create n bind variables 
	 * for a collection of length n.  :^(
	 */
	public String authorizationSQL(int n) {
		return AuthorizationSqlUtil.authorizationSQL(n);
	}
	

	@Transactional(readOnly = true)
	public Collection<Object> execAuthorizationSQL(Collection<Long> groupIds, AuthorizationConstants.ACCESS_TYPE type) {
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
//		System.out.println(authorizationSQL(groupIds.size()));
		Map<String,Object> parameters = new HashMap<String,Object>();
		int i=0;
		for (Long gId : groupIds) {
			parameters.put("g"+(i++), gId);
		}
		parameters.put("type", type.name());
		return exec.executeSingleCol(authorizationSQL(groupIds.size()), parameters);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String create(AccessControlList dto) throws DatastoreException,
			InvalidModelException, NotFoundException {
		// Create a jdo
		JDOAccessControlList jdo = newJDO();
		copyFromDto(dto, jdo);
		// Save it
		jdo = jdoTemplate.makePersistent(jdo);
		return KeyFactory.keyToString(jdo.getId());
	}

	@Transactional(readOnly = true)
	@Override
	public AccessControlList get(String id) throws DatastoreException,
			NotFoundException {
		try{
			JDOAccessControlList jdo = jdoTemplate.getObjectById(JDOAccessControlList.class, KeyFactory.stringToKey(id));
			AccessControlList dto = newDTO();
			copyToDto(jdo, KeyFactory.keyToString(jdo.getResource().geteTag()), dto);
			return dto;
		}catch (JdoObjectRetrievalFailureException e){
			throw new NotFoundException("Cannot find an ACL with ID = "+id);
		}
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void update(AccessControlList dto) throws DatastoreException,
			InvalidModelException, NotFoundException,
			ConflictingUpdateException {
		JDOAccessControlList jdo = jdoTemplate.getObjectById(JDOAccessControlList.class, KeyFactory.stringToKey(dto.getId()));
		copyFromDto(dto, jdo);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		JDOAccessControlList jdo = jdoTemplate.getObjectById(JDOAccessControlList.class, KeyFactory.stringToKey(id));
		jdoTemplate.deletePersistent(jdo);
	}

}
