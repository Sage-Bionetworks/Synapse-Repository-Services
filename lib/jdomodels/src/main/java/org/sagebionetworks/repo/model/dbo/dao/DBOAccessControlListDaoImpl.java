package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_CONTROL_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS_TYPE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccess;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccessType;
import org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOAccessControlListDaoImpl implements AccessControlListDAO {

	private static final String SELECT_ACCESS_TYPES_FOR_RESOURCE = "SELECT "+COL_RESOURCE_ACCESS_TYPE_ELEMENT+" FROM "+TABLE_RESOURCE_ACCESS_TYPE+" WHERE "+COL_RESOURCE_ACCESS_TYPE_ID+" = ?";

	private static final String DELETE_RESOURCE_ACCESS_SQL = "DELETE FROM "+TABLE_RESOURCE_ACCESS+" WHERE "+COL_RESOURCE_ACCESS_OWNER+" = ?";

	private static final String SELECT_ALL_RESOURCE_ACCESS = "SELECT * FROM "+TABLE_RESOURCE_ACCESS+" WHERE "+COL_RESOURCE_ACCESS_OWNER+" = ?";

	private static final String SELECT_ETAG_FOR_UPDATE = "SELECT "+COL_ACL_ETAG+" FROM "+TABLE_ACCESS_CONTROL_LIST+" WHERE "+COL_ACL_ID+" = :" + COL_ACL_ID+" FOR UPDATE";

	/**
	 * Keep a copy of the row mapper.
	 */
	private static final RowMapper<DBOResourceAccess> accessMapper = new DBOResourceAccess().getTableMapping();

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;	
	@Autowired
	private DBOBasicDao dboBasicDao;
	@Autowired
	private IdGenerator idGenerator;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String create(AccessControlList acl) throws DatastoreException, NotFoundException {

		if (acl == null) {
			throw new IllegalArgumentException("ACL cannot be null.");
		}

		acl.setEtag(UUID.randomUUID().toString());

		AccessControlListUtils.validateACL(acl);

		DBOAccessControlList dbo = AccessControlListUtils.createDBO(acl);
		dboBasicDao.createNew(dbo);
		populateResourceAccess(acl);

		return acl.getId(); // This preserves the "syn" prefix
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
			// assign an id
			dboRa.setId(idGenerator.generateNewId(TYPE.ACL_RES_ACC_ID));
			dboRa.setOwner(owner);
			if (ra.getPrincipalId()==null) {
				throw new IllegalArgumentException("ResourceAccess cannot have null principalID");
			} else {
				dboRa.setUserGroupId(ra.getPrincipalId());
			}
			dboRa = dboBasicDao.createNew(dboRa);
			// Now add all of the access
			Set<ACCESS_TYPE> access = ra.getAccessType();
			List<DBOResourceAccessType> batch = AccessControlListUtils.createResourceAccessTypeBatch(dboRa.getId(), owner, access);
			// Add the batch
			dboBasicDao.createBatch(batch);
		}
	}

	@Override
	public AccessControlList get(final String ownerId, final ObjectType objectType)
			throws DatastoreException, NotFoundException {
		final Long ownerKey = KeyFactory.stringToKey(ownerId);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("id", ownerKey);
		DBOAccessControlList dboAcl = dboBasicDao.getObjectByPrimaryKey(DBOAccessControlList.class, param);
		AccessControlList acl = AccessControlListUtils.createAcl(dboAcl, objectType);
		// Now fetch the rest of the data for this ACL
		List<DBOResourceAccess> raList = simpleJdbcTemplate.query(SELECT_ALL_RESOURCE_ACCESS, accessMapper, ownerKey);
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		acl.setResourceAccess(raSet);
		for(DBOResourceAccess raDbo: raList){
			List<String> typeList = simpleJdbcTemplate.query(SELECT_ACCESS_TYPES_FOR_RESOURCE, new RowMapper<String>(){
				@Override
				public String mapRow(ResultSet rs, int rowNum)throws SQLException {
					return rs.getString(COL_RESOURCE_ACCESS_TYPE_ELEMENT);
				}}, raDbo.getId());
			// build up this type
			ResourceAccess ra = new ResourceAccess();
			ra.setPrincipalId(raDbo.getUserGroupId());
			ra.setAccessType(new HashSet<ACCESS_TYPE>());
			for(String typeString: typeList){
				ra.getAccessType().add(ACCESS_TYPE.valueOf(typeString));
			}
			raSet.add(ra);
		}
		return acl;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void update(AccessControlList acl) throws DatastoreException, NotFoundException {

		AccessControlListUtils.validateACL(acl);

		// Check e-tags before update
		final Long ownerKey = KeyFactory.stringToKey(acl.getId());
		String etag = selectEtagForUpdate(ownerKey);
		if (!acl.getEtag().equals(etag)) {
			throw new ConflictingUpdateException("E-tags do not match.");
		}
		acl.setEtag(UUID.randomUUID().toString());

		DBOAccessControlList dbo = AccessControlListUtils.createDBO(acl);
		dboBasicDao.update(dbo);
		// Now delete the resource access
		simpleJdbcTemplate.update(DELETE_RESOURCE_ACCESS_SQL, ownerKey);
		// Now recreate it from the passed data.
		populateResourceAccess(acl);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String ownerId) throws DatastoreException {
		final Long ownerKey = KeyFactory.stringToKey(ownerId);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", ownerKey);
		dboBasicDao.deleteObjectByPrimaryKey(DBOAccessControlList.class, params);
	}

	@Override
	public boolean canAccess(Collection<UserGroup> groups, String resourceId,
			ACCESS_TYPE accessType) throws DatastoreException {
		// Build up the parameters
		Map<String,Object> parameters = new HashMap<String,Object>();
		int i=0;
		for (UserGroup gId : groups) {
			parameters.put(AuthorizationSqlUtil.BIND_VAR_PREFIX+(i++), gId.getId());
		}
		// Bind the type
		parameters.put(AuthorizationSqlUtil.ACCESS_TYPE_BIND_VAR, accessType.name());
		// Bind the node id
		parameters.put(AuthorizationSqlUtil.RESOURCE_ID_BIND_VAR, KeyFactory.stringToKey(resourceId));
		String sql = AuthorizationSqlUtil.authorizationCanAccessSQL(groups.size());
		try{
			long count = simpleJdbcTemplate.queryForLong(sql, parameters);
			return count > 0;
		}catch (DataAccessException e){
			throw new DatastoreException(e);
		}
	}

	// To avoid potential race conditions, we do "SELECT ... FOR UPDATE" on etags.
	private String selectEtagForUpdate(final Long id) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACL_ID, id);
		String etag = simpleJdbcTemplate.queryForObject(
				SELECT_ETAG_FOR_UPDATE, String.class, param);
		return etag;
	}
}
