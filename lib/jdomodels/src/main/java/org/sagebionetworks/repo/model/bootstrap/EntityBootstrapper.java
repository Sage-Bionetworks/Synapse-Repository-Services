package org.sagebionetworks.repo.model.bootstrap;

import java.util.List;

import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;

/**
 * An implementation of this interface will bootstrap all entities in the system.
 * 
 * @author jmhill
 *
 */
public interface EntityBootstrapper {
	
	/**
	 * The list of entities to be bootstrapped.
	 * @return
	 */
	public List<EntityBootstrapData> getBootstrapEntities();
	
	
	/**
	 * What is ACL Scheme should be use for child entities of a given path.
	 * @param path
	 * @return
	 */
	public ACL_SCHEME getChildAclSchemeForPath(String path);
	
	/**
	 * Bootstrap all data.
	 * @throws Exception 
	 */
	public void bootstrapAll() throws Exception;

}
