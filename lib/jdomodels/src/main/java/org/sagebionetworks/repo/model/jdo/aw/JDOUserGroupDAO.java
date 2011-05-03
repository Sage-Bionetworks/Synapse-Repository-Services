package org.sagebionetworks.repo.model.jdo.aw;

import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;

public interface JDOUserGroupDAO {

	JDOUserGroup getPublicGroup();

	JDOUserGroup createPublicGroup();

	JDOUserGroup getAdminGroup();

	JDOUserGroup createAdminGroup();

	void addUser(JDOUserGroup ag, JDOUser adminUser);

}
