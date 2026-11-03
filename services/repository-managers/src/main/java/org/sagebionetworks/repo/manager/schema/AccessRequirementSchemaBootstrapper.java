package org.sagebionetworks.repo.manager.schema;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

/**
 * Registers the access requirement base schema as the context starts, so that form templates can
 * be validated against it before {@link SynapseSchemaBootstrap#bootstrapSynapseSchemas()} first
 * runs an hour into the life of the stack.
 */
@Service
@DependsOn({ "realmDao", "teamManager" })
public class AccessRequirementSchemaBootstrapper {

	private static final Logger LOG = LogManager.getLogger(AccessRequirementSchemaBootstrapper.class);

	public AccessRequirementSchemaBootstrapper(SynapseSchemaBootstrap bootstrap, UserManager userManager) {
		try {
			// The bootstrap is a write, so it must run against the bootstrap admin rather than the
			// caller that happens to trigger the first form template request.
			bootstrap.bootstrapAccessRequirementBaseSchema(
					userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()));
		} catch (Exception e) {
			// A throw here would propagate out of the bean's constructor and fail the deployment of
			// the whole stack, which answers every request with a container 404 that names no cause.
			// Only form templates depend on this schema, and the hourly bootstrap worker reconciles
			// it, so the failure is confined to that feature and reported here instead.
			LOG.error("Failed to register the access requirement base schema."
					+ " Form template validation will fail until the schema bootstrap worker succeeds.", e);
		}
	}
}
