package org.sagebionetworks.tool.migration.dao;

import java.util.List;

import org.json.JSONException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

/**
 * By making this an interface, we can have a real implementation that is well
 * tested with integration tests, an stub implementation to unit test all 
 * dependencies.
 * 
 * 
 * @author jmhill
 *
 */
public interface QueryRunner {
	
	/**
	 * Get the root entity.
	 * @return
	 * @throws SynapseException
	 * @throws JSONException
	 */
	public EntityData getRootEntity() throws SynapseException, JSONException;
	
	/**
	 * Get all entity data from a given repository.
	 * @return
	 * @throws SynapseException
	 * @throws IllegalAccessException
	 * @throws JSONException 
	 * @throws InterruptedException 
	 * @throws JSONObjectAdapterException 
	 */
	public List<EntityData> getAllEntityData(BasicProgress progress) throws SynapseException, JSONException, InterruptedException, JSONObjectAdapterException;
	
	/**
	 * Get all entity data for entities of a given type.
	 * @param type
	 * @param progress
	 * @return
	 * @throws SynapseException
	 * @throws JSONException
	 * @throws InterruptedException
	 */
	public List<EntityData> getAllEntityDataOfType(EntityType type, BasicProgress progress) throws SynapseException, JSONException, InterruptedException;
	
	/**
	 * Get all child entities of a given parent.
	 * @param parentId
	 * @return
	 * @throws SynapseException
	 * @throws IllegalAccessException
	 * @throws JSONException 
	 * @throws InterruptedException 
	 */
	public List<EntityData> getAllAllChildrenOfEntity(String parentId) throws SynapseException, IllegalAccessException, JSONException, InterruptedException;
	
	/**
	 * Get the total entity count.
	 * @return
	 * @throws SynapseException 
	 * @throws JSONException 
	 * @throws JSONObjectAdapterException 
	 */
	public long getTotalEntityCount() throws SynapseException, JSONException, JSONObjectAdapterException;
	
	/**
	 * Get a count for an entity type
	 * @param type
	 * @return
	 * @throws SynapseException
	 * @throws JSONException
	 */
	public long getCountForType(EntityType type) throws SynapseException, JSONException;

}
