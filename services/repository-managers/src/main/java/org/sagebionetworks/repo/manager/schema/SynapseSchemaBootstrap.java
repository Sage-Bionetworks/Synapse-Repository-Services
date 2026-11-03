package org.sagebionetworks.repo.manager.schema;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * Bootstrap the schemas for Synapse objects into the JSON schema repository.
 *
 */
public interface SynapseSchemaBootstrap  {
	
	/**
	 * Start the singleton process to bootstrap all Synapse JSON schemas.
	 * @throws RecoverableMessageException
	 */
	public void bootstrapSynapseSchemas() throws RecoverableMessageException;

	/**
	 * Create the 'org.sagebionetworks' organization if it does not already exist.
	 */
	public Organization createOrganizationIfDoesNotExist(UserInfo adminUser);

	/**
	 * Register the base schema that every access requirement schema extends, along with the
	 * organization the ACT authors those schemas under. Does nothing if both already exist.
	 * <p>
	 * This runs on its own rather than only as part of {@link #bootstrapSynapseSchemas()} because a
	 * form template cannot be created until the base schema is registered, so the window between a
	 * stack starting and the bootstrap worker first firing must not include it.
	 *
	 * @param adminUser
	 */
	public void bootstrapAccessRequirementBaseSchema(UserInfo adminUser);

}
