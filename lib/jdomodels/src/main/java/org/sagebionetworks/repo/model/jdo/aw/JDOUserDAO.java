package org.sagebionetworks.repo.model.jdo.aw;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.User;

public interface JDOUserDAO extends JDOBaseDAO<User> {

	User getUser(String userName) throws DatastoreException;

}
