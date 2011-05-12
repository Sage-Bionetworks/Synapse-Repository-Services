package org.sagebionetworks.repo.model.jdo;

import java.util.Collection;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.jdo.aw.JDOUserDAO;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class JDOUserDAOImpl extends JDOBaseDAOImpl<User,JDOUser> implements JDOUserDAO {
	
	User newDTO() {
		return new User();
	}

	JDOUser newJDO() {
		return new JDOUser();
	}

	void copyToDto(JDOUser jdo, User dto)
			throws DatastoreException {
		dto.setId(jdo.getId() == null ? null : KeyFactory.keyToString(jdo
				.getId()));
		dto.setCreationDate(jdo.getCreationDate());
		dto.setUserId(jdo.getUserId());
		dto.setIamAccessId(jdo.getIamAccessId());
		dto.setIamSecretKey(jdo.getIamSecretKey());
	}

	void copyFromDto(User dto, JDOUser jdo)
			throws InvalidModelException {
		if (null == dto.getUserId()) {
			throw new InvalidModelException(
					"'userId' is a required property for User");
		}
		jdo.setUserId(dto.getUserId());
		jdo.setCreationDate(dto.getCreationDate());
		jdo.setIamAccessId(dto.getIamAccessId());
		jdo.setIamSecretKey(dto.getIamSecretKey());
	}

	@Override
	Class<JDOUser> getJdoClass() {
		return JDOUser.class;
	}
		
	public User getUser(String userName) throws DatastoreException {
		JDOExecutor exec = new JDOExecutor(jdoTemplate);
		Collection<JDOUser> u = exec.execute(JDOUser.class, "userId==pUserId", String.class.getName()+" pUserId", null, userName);
		if (u.size()>1) throw new DatastoreException("Expected one user named "+userName+" but found "+u.size());
		if (u.size()==0) return null;
		JDOUser jdo = u.iterator().next();
		User dto = newDTO();
		copyToDto(jdo, dto);
		return dto;
	}
	
	
	/**
	 * Overrides the 'update' in BaseDAO to check that the userId is not being changed
	 * 
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 *             if version in dto doesn't match version of object
	 * @throws InvalidModelException
	 */
	@Override
	public void update(User dto) throws DatastoreException,
			InvalidModelException, NotFoundException {
		Long id = KeyFactory.stringToKey(dto.getId()); 
		JDOUser jdo = (JDOUser) jdoTemplate.getObjectById(getJdoClass(), id);
		if (null == dto.getUserId()) {
			throw new InvalidModelException(
					"'userId' is a required property for User");
		}
		if (!dto.getUserId().equals(jdo.getUserId()))
				throw new InvalidModelException("May not change a user's userId");
		super.update(dto);
	}
		
}
