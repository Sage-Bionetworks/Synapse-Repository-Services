package org.sagebionetworks.repo.model.jdo;

import java.util.Arrays;
import java.util.Collection;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.sagebionetworks.repo.web.NotFoundException;



public class JDOUserDAOImpl extends JDOBaseDAOImpl<User,JDOUser> implements UserDAO {

	public JDOUserDAOImpl(String userId) {super(userId);}
	
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
	
	protected JDOUser getUser(PersistenceManager pm) {
		return getUser(userId, pm);
	}
	
	protected JDOUser getUser(String userId, PersistenceManager pm) {
		if (userId==null) return null;
		Query query = pm.newQuery(JDOUser.class);
		query.setFilter("userId==pUserId");
		query.declareParameters(String.class.getName()+" pUserId");
		@SuppressWarnings("unchecked")
		Collection<JDOUser> users = (Collection<JDOUser>)query.execute(userId);
		if (users.size()>1) throw new IllegalStateException("Expected 0-1 but found "+users.size()+" users for "+userId);
		if (users.size()==0) return null;
		return users.iterator().next();
	}
	
	/**
	 * Overrides the parent class to create the user's 'individual group' and add the user to it.
	 * 
	 * @param dto
	 * @return the ID of the created object
	 * @throws InvalidModelException
	 */
	public String create(User dto) throws InvalidModelException,
			DatastoreException, UnauthorizedException {
		String sid = super.create(dto);
		
		Long id = KeyFactory.stringToKey(dto.getId()); 
		PersistenceManager pm = PMF.get();
		JDOUser jdoUser = (JDOUser) pm.getObjectById(getJdoClass(), id);

		(new JDOUserGroupDAOImpl(null)).createIndividualGroup(pm, jdoUser);

		return sid;
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
	public void update(PersistenceManager pm, User dto) throws DatastoreException,
			InvalidModelException, NotFoundException, UnauthorizedException {
		Long id = KeyFactory.stringToKey(dto.getId()); 
		JDOUser jdo = (JDOUser) pm.getObjectById(getJdoClass(), id);
		if (null == dto.getUserId()) {
			throw new InvalidModelException(
					"'userId' is a required property for User");
		}
		if (!dto.getUserId().equals(jdo.getUserId()))
				throw new InvalidModelException("May not change a user's userId");
		super.update(pm, dto);
	}

	/**
	 * Delete the specified object
	 * Overrides the parent method to first remove the user from all groups to which it belongs and to delete the 
	 * user's 'individual group'
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException,
			UnauthorizedException {
		PersistenceManager pm = PMF.get();
		JDOUser jdoUser = pm.getObjectById(JDOUser.class, KeyFactory.stringToKey(id));
		// we use the target users credentials when getting the DAO for the 'getIndividualGroup' method call, below
		JDOUserGroupDAOImpl adminGroupDAO = new JDOUserGroupDAOImpl(AuthUtilConstants.ADMIN_USER_ID);
		adminGroupDAO.removeUserFromAllGroups(jdoUser);
		JDOUserGroup individualGroup = (new JDOUserGroupDAOImpl(jdoUser.getUserId())).getIndividualGroup(pm);
		adminGroupDAO.delete(KeyFactory.keyToString(individualGroup.getId()));
		super.delete(id);
	}
		
}
