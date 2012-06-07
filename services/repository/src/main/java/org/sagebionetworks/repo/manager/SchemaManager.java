package org.sagebionetworks.repo.manager;

import java.io.IOException;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;

/**
 * Abstraction for the schema manager.
 * 
 * @author jmhill
 *
 */
public interface SchemaManager {
	
	/**
	 * Get the full list of all REST resources.
	 * 
	 * @return
	 */
	public RestResourceList getRESTResources() ;
	
	/**
	 * Get the effective schema of a resource.  The full schema of an entity is hierarchical with properties inherited from
	 * base schemas.  This can be difficult to consume as the entire hierarchy must be navigated to determine which properties
	 * an entity possesses. As a convenience, we provide the 'effective schema' which is the schema with all of the hierarchy flattened.
	 * If you just want to know what properties an entity has without worrying about inheritances, the effective schema will provide that.
	 * @param resourceId
	 * @return
	 * @throws NotFoundException
	 * @throws  
	 */
	public ObjectSchema getEffectiveSchema(String resourceId) throws NotFoundException, DatastoreException;
	
	
	/**
	 * Get the full schema for a resource.
	 * @param resourceId
	 * @return
	 * @throws NotFoundException
	 * @throws IOException 
	 * @throws DatastoreException 
	 */
	public ObjectSchema getFullSchema(String resourceId) throws NotFoundException, DatastoreException;
	
	/**
	 * Get the current entity Registry
	 * @return
	 */
	public EntityRegistry getEntityRegistry();

}
