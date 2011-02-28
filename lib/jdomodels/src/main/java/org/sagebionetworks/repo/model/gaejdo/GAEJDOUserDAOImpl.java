package org.sagebionetworks.repo.model.gaejdo;

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

import com.google.appengine.api.datastore.KeyFactory;

public class GAEJDOUserDAOImpl extends GAEJDOBaseDAOImpl<User,GAEJDOUser> implements UserDAO {

	public GAEJDOUserDAOImpl(String userId) {super(userId);}
	
	public Collection<String> getPrimaryFields() {
		return Arrays.asList(new String[] { "userId" });
	}

	protected User newDTO() {
		return new User();
	}

	protected GAEJDOUser newJDO() {
		return new GAEJDOUser();
	}

	protected void copyToDto(GAEJDOUser jdo, User dto)
			throws DatastoreException {
		dto.setId(jdo.getId() == null ? null : KeyFactory.keyToString(jdo
				.getId()));
		dto.setCreationDate(jdo.getCreationDate());
		dto.setUserId(jdo.getUserId());
	}

	protected void copyFromDto(User dto, GAEJDOUser jdo)
			throws InvalidModelException {
		jdo.setUserId(dto.getUserId());
		jdo.setCreationDate(dto.getCreationDate());
	}

	@Override
	protected Class getJdoClass() {
		return GAEJDOUser.class;
	}
	
	protected GAEJDOUser getUser(PersistenceManager pm) {
		if (userId==null) return null;
		Query query = pm.newQuery(GAEJDOUser.class);
		query.setFilter("userId==pUserId");
		query.declareParameters(String.class.getName()+" pUserId");
		@SuppressWarnings("unchecked")
		Collection<GAEJDOUser> users = (Collection<GAEJDOUser>)query.execute(userId);
		if (users.size()>1) throw new IllegalStateException("Expected 0-1 but found "+users.size()+" users for "+userId);
		if (users.size()==0) return null;
		return users.iterator().next();
	}

}
