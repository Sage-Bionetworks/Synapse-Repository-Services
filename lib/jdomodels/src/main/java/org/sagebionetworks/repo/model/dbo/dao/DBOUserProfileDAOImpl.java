/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * @author brucehoff
 *
 */
public class DBOUserProfileDAOImpl implements UserProfileDAO {
	@Autowired
	private DBOBasicDao basicDao;
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.UserProfileDAO#delete(java.lang.String)
	 */
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOUserProfile.OWNER_ID_FIELD_NAME, id);
		basicDao.deleteObjectById(DBOUserProfile.class, param);
	}


	@Override
	public String create(UserProfile dto, ObjectSchema schema) throws DatastoreException,
			InvalidModelException {
		DBOUserProfile jdo = new DBOUserProfile();
		UserProfileUtils.copyDtoToDbo(dto, jdo, schema);
		if (jdo.geteTag()==null) jdo.seteTag(0L);
		jdo = basicDao.createNew(jdo);
		return KeyFactory.keyToString(jdo.getOwnerId());
	}

	@Override
	public UserProfile get(String id, ObjectSchema schema) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOUserProfile.OWNER_ID_FIELD_NAME, id);
		DBOUserProfile jdo = basicDao.getObjectById(DBOUserProfile.class, param);
		UserProfile dto = new UserProfile();
		UserProfileUtils.copyJdoToDbo(jdo, dto, schema);
		return dto;
	}


	@Override
	public UserProfile update(UserProfile dto, ObjectSchema schema) throws DatastoreException,
			InvalidModelException, NotFoundException,
			ConflictingUpdateException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOUserProfile.OWNER_ID_FIELD_NAME, dto.getOwnerId());
		DBOUserProfile jdo = basicDao.getObjectById(DBOUserProfile.class, param);
		UserProfileUtils.copyDtoToDbo(dto, jdo, schema);
		boolean success = basicDao.update(jdo);
		if (!success) throw new DatastoreException("Unsuccessful updating user profile in database.");
		UserProfileUtils.copyJdoToDbo(jdo,  dto, schema);
		return dto;
	}

}
