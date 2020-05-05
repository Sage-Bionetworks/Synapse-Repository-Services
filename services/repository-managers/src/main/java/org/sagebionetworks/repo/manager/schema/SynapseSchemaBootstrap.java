package org.sagebionetworks.repo.manager.schema;

/**
 * Bootstrap the schemas for Synapse objects into the JSON schema repository.
 *
 */
public interface SynapseSchemaBootstrap  {
	
	/**
	 * Start the singleton process to bootstrap all Synapse JSON schemas.
	 */
	public void bootstrapSynapseSchemas();

}
