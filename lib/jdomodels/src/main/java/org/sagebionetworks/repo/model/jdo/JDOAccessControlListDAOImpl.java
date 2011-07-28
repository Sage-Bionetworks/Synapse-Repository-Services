package org.sagebionetworks.repo.model.jdo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAccessControlList;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class JDOAccessControlListDAOImpl extends JDOBaseDAOImpl<AccessControlList, JDOAccessControlList> implements AccessControlListDAO {

	/**
	 * Find the access control list for the given resource
	 * @throws DatastoreException 
	 */
	@Transactional
	public AccessControlList getForResource(String rId) throws DatastoreException {
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
		List<JDOAccessControlList> result = exec.execute(JDOAccessControlList.class,
				"resource.id==pResourceId",
				Long.class.getName()+" pResourceId",
				null,
				KeyFactory.stringToKey(rId));
		if (result.size()==0) return null;
		if (result.size()>1) throw new DatastoreException("Expected 0-1 but found "+result.size());
		JDOAccessControlList jdo = result.iterator().next();
		AccessControlList dto = newDTO();
		copyToDto(jdo, dto);
		return dto;
	}
	
	@Override
	AccessControlList newDTO() {
		AccessControlList dto = new AccessControlList();
		dto.setResourceAccess(new HashSet<ResourceAccess>());
		return dto;
	}

	@Override
	JDOAccessControlList newJDO() {
		JDOAccessControlList jdo = new JDOAccessControlList();
		jdo.setResourceAccess(new HashSet<JDOResourceAccess>());
		return jdo;
	}

	@Override
	void copyToDto(JDOAccessControlList jdo, AccessControlList dto)
			throws DatastoreException {
		AccessControlListUtil.updateDtoFromJdo(jdo, dto);
	}

	@Override
	void copyFromDto(AccessControlList dto, JDOAccessControlList jdo)
			throws InvalidModelException, DatastoreException {
		if(dto.getResourceId() == null) throw new InvalidModelException("Cannot set a ResourceAccess owner to null");
		JDONode owner = jdoTemplate.getObjectById(JDONode.class, KeyFactory.stringToKey(dto.getResourceId()));
		AccessControlListUtil.updateJdoFromDto(jdo, dto, owner);
	}

	@Override
	Class<JDOAccessControlList> getJdoClass() {
		return JDOAccessControlList.class;
	}
	
	@Override
	String defaultSortField() {
		return "id";
	}



	/**
	 * @return true iff some group in 'groups' has explicit permission to access 'resourceId' using access type 'accessType'
	 */
	@Transactional
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
	

	@Transactional
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

}
