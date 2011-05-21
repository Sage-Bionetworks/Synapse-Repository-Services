package org.sagebionetworks.repo.model.jdo;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ResourceAccess2;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAccessControlList;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess2;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
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
		dto.setResourceAccess(new HashSet<ResourceAccess2>());
		return dto;
	}

	@Override
	JDOAccessControlList newJDO() {
		JDOAccessControlList jdo = new JDOAccessControlList();
		jdo.setResourceAccess(new HashSet<JDOResourceAccess2>());
		return jdo;
	}
	
	private ResourceAccess2 newRaDTO() {
		ResourceAccess2 raDto = new ResourceAccess2();
		raDto.setAccessType(new HashSet<AuthorizationConstants.ACCESS_TYPE>());
		return raDto;
	}
	
	private void copyToRaDto(JDOResourceAccess2 raJdo, ResourceAccess2 raDto) throws DatastoreException {
		raDto.setUserGroupId(KeyFactory.keyToString(raJdo.getUserGroupId()));
		Set<AuthorizationConstants.ACCESS_TYPE> dtoAccessTypes = new HashSet<AuthorizationConstants.ACCESS_TYPE>();
		for (String jdoAccessType : raJdo.getAccessType()) {
			dtoAccessTypes.add(AuthorizationConstants.ACCESS_TYPE.valueOf(jdoAccessType));
		}
		raDto.setAccessType(dtoAccessTypes);
	}

	@Override
	void copyToDto(JDOAccessControlList jdo, AccessControlList dto)
			throws DatastoreException {
		dto.setCreatedBy(jdo.getCreatedBy());
		dto.setCreationDate(jdo.getCreationDate());
		dto.setEtag(jdo.getEtag()==null ? null : KeyFactory.keyToString(jdo.getEtag()));
		dto.setId(jdo.getId()==null ? null : KeyFactory.keyToString(jdo.getId()));
		dto.setModifiedBy(jdo.getModifiedBy());
		dto.setModifiedOn(new Date(jdo.getModifiedOn()));
		Set<ResourceAccess2> ras = new HashSet<ResourceAccess2>();
		for (JDOResourceAccess2 raJdo: jdo.getResourceAccess()) {
			ResourceAccess2 raDto = newRaDTO();
			copyToRaDto(raJdo, raDto);
			ras.add(raDto);
		}
		dto.setResourceAccess(ras);
		dto.setResourceId(KeyFactory.keyToString(jdo.getResource().getId()));
	}

	private JDOResourceAccess2 newRaJDO() {
		JDOResourceAccess2 raJdo = new JDOResourceAccess2();
		raJdo.setAccessType(new HashSet<String>());
		return raJdo;
	}
	
	private void copyFromRaDto(ResourceAccess2 raDto, JDOResourceAccess2 raJdo) throws DatastoreException {
//		JDOUserGroup g = jdoTemplate.getObjectById(JDOUserGroup.class, KeyFactory.stringToKey(raDto.getUserGroupId()));
		raJdo.setUserGroupId(KeyFactory.stringToKey(raDto.getUserGroupId()));
		Set<String> jdoAccessTypes = new HashSet<String>();
		for (AuthorizationConstants.ACCESS_TYPE jdoAccessType : raDto.getAccessType()) {
			jdoAccessTypes.add(jdoAccessType.name());
		}
		raJdo.setAccessType(jdoAccessTypes);
	}

	@Override
	void copyFromDto(AccessControlList dto, JDOAccessControlList jdo)
			throws InvalidModelException, DatastoreException {
		jdo.setCreatedBy(dto.getCreatedBy());
		jdo.setCreationDate(dto.getCreationDate());
		jdo.setEtag(dto.getEtag()==null ? null : KeyFactory.stringToKey(dto.getEtag()));
		jdo.setId(dto.getId()==null ? null : KeyFactory.stringToKey(dto.getId()));
		jdo.setModifiedBy(dto.getModifiedBy());
		jdo.setModifiedOn(dto.getModifiedOn()==null ? null : dto.getModifiedOn().getTime());
		JDONode node = jdoTemplate.getObjectById(JDONode.class, KeyFactory.stringToKey(dto.getResourceId()));
		jdo.setResource(node);
		Set<JDOResourceAccess2> ras = new HashSet<JDOResourceAccess2>();
		for (ResourceAccess2 raDto : dto.getResourceAccess()) {
			JDOResourceAccess2 raJdo = newRaJDO();
			copyFromRaDto(raDto, raJdo);
			ras.add(raJdo);
		}
		jdo.setResourceAccess(ras);
		
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
			JDOResourceAccess2.class.getName()+" vResourceAccess",
			KeyFactory.stringToKey(resourceId),
			groupIds,
			accessType.name());
		
		return result.size()>0;
	}

	private static final String CAN_ACCESS_FILTER = 
		"this.resource.id==pResourceId && this.resourceAccess.contains(vResourceAccess) "+
		"&& pGroups.contains(vResourceAccess.userGroupId) "+
		"&& vResourceAccess.accessType.contains(pAccessType)";
	
	/*
	 * select acl.id from jdoaccesscontrollist acl, jdoresourceaccess ra, access_type at
	 * where
	 * ra.oid_id=acl.id and ra.groupId in :groups and at.oid_id=ra.id and at.type=:type
	 */
	private static final String AUTHORIZATION_SQL_1 = 
		"select distinct acl."+SqlConstants.COL_ACL_OWNER_ID+" from "+SqlConstants.TABLE_ACCESS_CONTROL_LIST+" acl, "+
		SqlConstants.TABLE_RESOURCE_ACCESS+" ra, "+
		SqlConstants.TABLE_RESOURCE_ACCESS_TYPE+" at where ra."+
//		"RESOURCE_ACCESS_ID_OWN"+
		SqlConstants.COL_RESOURCE_ACCESS_OWNER+
		"=acl."+SqlConstants.COL_ACL_ID+" and (ra."+SqlConstants.COL_USER_GROUP_ID+
		" in (";

	private static final String AUTHORIZATION_SQL_2 = 
		"))"+
		" and at."+SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID+"=ra."+SqlConstants.COL_NODE_ID+
		" and at."+SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT+"=:type";


	/**
	 * @return the SQL to find the root-accessible nodes that a specified user-group list can access
	 * using a specified access type
	 * 
	 * Can't bind a collection to a variable in the string, so we have to create n bind variables 
	 * for a collection of length n.  :^(
	 */
	public String authorizationSQL(int n) {
		StringBuilder sb = new StringBuilder(AUTHORIZATION_SQL_1);
		for (int i=0; i<n; i++) {
			if (i>0) sb.append(",");
			sb.append(":g"+i);
		}
		sb.append(AUTHORIZATION_SQL_2);
		return sb.toString();
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
	
//	public static String sqlCollection(Collection<? extends Object> c) {
//		StringBuffer ans = new StringBuffer("(");
//		boolean first = true;
//		for (Object o : c) {
//			if (first) first=false; else ans.append(",");
//			ans.append(o.toString());
//		}
//		ans.append(")");
//		return ans.toString();
//	}



}
