package org.sagebionetworks.repo.manager.schema;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;

public class SynapseSchemaBootstrapImpl implements SynapseSchemaBootstrap {



	@Autowired
	private JsonSchemaManager jsonSchemaManager;

	@Autowired
	private UserManager userManager;
	
	@Autowired
	SchemaTranslator translator;

	@Override
	public void bootstrapSynapseSchemas() {
		// The process is run as the Synapse admin
		UserInfo adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

	}




}
