package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccess;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccessType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.UserGroupCache;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class DBOAccessControlListDaoImpl implements DBOAccessControlListDao {
	
	private static final String SELECT_ACCESS_TYPES_FOR_RESOURCE = "SELECT "+COL_RESOURCE_ACCESS_TYPE_ELEMENT+" FROM "+TABLE_RESOURCE_ACCESS_TYPE+" WHERE "+COL_RESOURCE_ACCESS_TYPE_ID+" = ?";

	private static final String SELECT_OWNER_ETAG = "SELECT "+COL_NODE_ETAG+" FROM "+TABLE_NODE+" WHERE "+COL_NODE_ID+" = ?";

	private static final String DELETE_RESOURCE_ACCESS_SQL = "DELETE FROM "+TABLE_RESOURCE_ACCESS+" WHERE "+COL_RESOURCE_ACCESS_OWNER+" = ?";

	private static final String SELECT_ALL_RESOURCE_ACCESS = "SELECT * FROM "+TABLE_RESOURCE_ACCESS+" WHERE "+COL_RESOURCE_ACCESS_OWNER+" = ?";

	/**
	 * Keep a copy of the row mapper.
	 */
	private static RowMapper<DBOResourceAccess> accessMapper = new DBOResourceAccess().getTableMapping();
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	@Autowired
	private UserGroupCache userGroupCache;

	@Autowired
	DBOBasicDao dboBasicDao;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList createACL(AccessControlList acl) throws DatastoreException, NotFoundException {
		AccessControlListUtils.validateACL(acl);
		// First create the base
		DBOAccessControlList dbo = AccessControlListUtils.createDBO(acl);
		// Create a new ACL 
		dboBasicDao.createNew(dbo);
		populateResourceAccess(acl);
		acl.setEtag(getETag(acl.getId()));
		return acl;
	}


	/**
	 * Populate the resource access table after the ACL table is ready.
	 * @param acl
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	private void populateResourceAccess(AccessControlList acl)
			throws DatastoreException, NotFoundException {
		// Now create each Resource Access.
		Set<ResourceAccess> set = acl.getResourceAccess();
		Long owner = KeyFactory.stringToKey(acl.getId());
		for(ResourceAccess ra: set){
			DBOResourceAccess dboRa = new DBOResourceAccess();
			dboRa.setOwner(owner);
			dboRa.setUserGroupId(userGroupCache.getIdForUserGroupName(ra.getGroupName()));
			dboRa = dboBasicDao.createNew(dboRa);
			// Now add all of the access
			Set<ACCESS_TYPE> access = ra.getAccessType();
			List<DBOResourceAccessType> batch = AccessControlListUtils.createResourceAccessTypeBatch(dboRa.getId(), access);
			// Add the batch
			dboBasicDao.createBatch(batch);
		}
	}


	@Transactional(readOnly = true)
	@Override
	public AccessControlList getACL(Long owner) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("id", owner);
		DBOAccessControlList dboAcl = dboBasicDao.getObjectById(DBOAccessControlList.class, param);
		AccessControlList acl = AccessControlListUtils.createAcl(dboAcl);
		// Now fetch the rest of the data for this ACL
		List<DBOResourceAccess> raList = simpleJdbcTemplate.query(SELECT_ALL_RESOURCE_ACCESS, accessMapper, owner);
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		acl.setResourceAccess(raSet);
		for(DBOResourceAccess raDbo: raList){
			String groupName = userGroupCache.getUserGroupNameForId(raDbo.getUserGroupId());
			List<String> typeList = simpleJdbcTemplate.query(SELECT_ACCESS_TYPES_FOR_RESOURCE, new RowMapper<String>(){
				@Override
				public String mapRow(ResultSet rs, int rowNum)throws SQLException {
					return rs.getString(COL_RESOURCE_ACCESS_TYPE_ELEMENT);
				}}, raDbo.getId());
			// build up this type
			ResourceAccess ra = new ResourceAccess();
			ra.setGroupName(groupName);
			ra.setAccessType(new HashSet<ACCESS_TYPE>());
			for(String typeString: typeList){
				ra.getAccessType().add(ACCESS_TYPE.valueOf(typeString));
			}
			raSet.add(ra);
		}
		acl.setEtag(getETag(acl.getId()));
		return acl;
	}
	
	private String getETag(String ownerString) throws DatastoreException{
		Long owner = KeyFactory.stringToKey(ownerString);
		Long etag = simpleJdbcTemplate.queryForLong(SELECT_OWNER_ETAG, owner);
		return KeyFactory.keyToString(etag);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList update(AccessControlList acl) throws DatastoreException, NotFoundException {
		AccessControlListUtils.validateACL(acl);
		// First convert the ACL to a DBO
		DBOAccessControlList dbo = AccessControlListUtils.createDBO(acl);
		// Create a new ACL 
		dboBasicDao.update(dbo);
		Long owner = KeyFactory.stringToKey(acl.getId());
		// Now delete the resource access
		simpleJdbcTemplate.update(DELETE_RESOURCE_ACCESS_SQL, owner);
		// Now recreate it from the passed data.
		populateResourceAccess(acl);
		return null;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean delete(Long owner) throws DatastoreException {
		// TODO Auto-generated method stub
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", owner);
		return dboBasicDao.deleteObjectById(DBOAccessControlList.class, params);
	}

}
