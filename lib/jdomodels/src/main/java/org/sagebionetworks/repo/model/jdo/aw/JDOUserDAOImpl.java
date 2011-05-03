package org.sagebionetworks.repo.model.jdo.aw;

import java.util.Arrays;
import java.util.Collection;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.jdo.JDOExecutor;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.web.NotFoundException;

public class JDOUserDAOImpl extends JDOBaseDAOImpl<User,JDOUser> implements JDOUserDAO {
	
	public Collection<String> getPrimaryFields() {
		return Arrays.asList(new String[] { "userId" });
	}

	protected User newDTO() {
		return new User();
	}

	protected JDOUser newJDO() {
		return new JDOUser();
	}

	protected void copyToDto(JDOUser jdo, User dto)
			throws DatastoreException {
		dto.setId(jdo.getId() == null ? null : KeyFactory.keyToString(jdo
				.getId()));
		dto.setCreationDate(jdo.getCreationDate());
		dto.setUserId(jdo.getUserId());
		dto.setIamAccessId(jdo.getIamAccessId());
		dto.setIamSecretKey(jdo.getIamSecretKey());
	}

	protected void copyFromDto(User dto, JDOUser jdo)
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
	protected Class<JDOUser> getJdoClass() {
		return JDOUser.class;
	}
		
	public JDOUser getUser(String userName) throws DatastoreException {
		JDOExecutor<JDOUser> exec = new JDOExecutor<JDOUser>(jdoTemplate, JDOUser.class);
		Collection<JDOUser> u = exec.execute("userId==pUserId", String.class.getName()+" pUserId", null, userName);
		if (u.size()>1) throw new DatastoreException("Expected one user named "+userName+" but found "+u.size());
		if (u.size()==0) return null;
		return u.iterator().next();
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
