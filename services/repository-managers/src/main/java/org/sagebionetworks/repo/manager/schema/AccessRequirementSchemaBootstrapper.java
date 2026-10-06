package org.sagebionetworks.repo.manager.schema;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

/**
 * Registers the access requirement base schema as the context starts.
 */
@Service
@DependsOn({ "realmDao", "teamManager" })
public class AccessRequirementSchemaBootstrapper {

	public AccessRequirementSchemaBootstrapper(SynapseSchemaBootstrap bootstrap, UserManager userManager) {
		// The bootstrap is a write, so it must run against the bootstrap admin rather than the caller
		// that happens to trigger the first form template request.
		bootstrap.bootstrapAccessRequirementBaseSchema(
				userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()));
	}
}
