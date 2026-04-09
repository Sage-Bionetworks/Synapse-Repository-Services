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

}
