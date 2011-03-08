package org.sagebionetworks.repo.model.jdo;

import java.util.Arrays;
import java.util.Collection;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroup;
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
	}

	protected void copyFromDto(User dto, JDOUser jdo)
			throws InvalidModelException {
		jdo.setUserId(dto.getUserId());
		jdo.setCreationDate(dto.getCreationDate());
	}

	@Override
	protected Class getJdoClass() {
		return JDOUser.class;
	}
	
	protected JDOUser getUser(PersistenceManager pm) {
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

}
