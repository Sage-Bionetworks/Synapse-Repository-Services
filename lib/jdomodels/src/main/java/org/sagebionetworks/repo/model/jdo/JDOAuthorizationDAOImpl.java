package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoTemplate;

public class JDOAuthorizationDAOImpl implements AuthorizationDAO {
	
	@Autowired
	private JdoTemplate jdoTemplate;

}
