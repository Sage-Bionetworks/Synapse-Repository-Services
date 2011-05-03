package org.sagebionetworks.repo.model.jdo.aw;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;

public interface JDOUserDAO {

	JDOUser getUser(String adminUserId) throws DatastoreException;

}
