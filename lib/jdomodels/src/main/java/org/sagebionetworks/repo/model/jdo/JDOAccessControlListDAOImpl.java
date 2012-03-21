package org.sagebionetworks.repo.model.jdo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.dbo.dao.DBOAccessControlListDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class JDOAccessControlListDAOImpl implements AccessControlListDAO {
	
	
	@Autowired
	DBOAccessControlListDao dboAccessControlListDao;
	
	// This is better suited for simple JDBC query.
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	/**
	 * Find the access control list for the given resource
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	@Transactional(readOnly = true)
	public AccessControlList getForResource(String rId) throws DatastoreException, NotFoundException {
		return dboAccessControlListDao.getACL(KeyFactory.stringToKey(rId));
	}
	
	/**
	 * @return true iff some group in 'groups' has explicit permission to access 'resourceId' using access type 'accessType'
	 */
	@Transactional(readOnly = true)
	public boolean canAccess(Collection<UserGroup> groups, 
			String resourceId, 
			AuthorizationConstants.ACCESS_TYPE accessType) throws DatastoreException {

		// Build up the parameters
		Map<String,Object> parameters = new HashMap<String,Object>();
		int i=0;
		for (UserGroup gId : groups) {
			parameters.put(AuthorizationSqlUtil.BIND_VAR_PREFIX+(i++), KeyFactory.stringToKey(gId.getId()));
		}
		// Bind the type
		parameters.put(AuthorizationSqlUtil.ACCESS_TYPE_BIND_VAR, accessType.name());
		// Bind the node id
		parameters.put(AuthorizationSqlUtil.NODE_ID_BIND_VAR, KeyFactory.stringToKey(resourceId));
		String sql = AuthorizationSqlUtil.authorizationCanAccessSQL(groups.size());
		try{
			Long count = simpleJdbcTemplate.queryForLong(sql, parameters);
			return count.longValue() > 0;
		}catch (DataAccessException e){
			throw new DatastoreException(e);
		}
	}


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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String create(AccessControlList dto) throws DatastoreException,InvalidModelException, NotFoundException {
		// Create a jdo
		dto = dboAccessControlListDao.createACL(dto);
		return dto.getId();
	}

	@Transactional(readOnly = true)
	@Override
	public AccessControlList get(String id) throws DatastoreException,	NotFoundException {
		return dboAccessControlListDao.getACL(KeyFactory.stringToKey(id));
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void update(AccessControlList dto) throws DatastoreException,
			InvalidModelException, NotFoundException,
			ConflictingUpdateException {
		dboAccessControlListDao.update(dto);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		dboAccessControlListDao.delete(KeyFactory.stringToKey(id));
	}

}
