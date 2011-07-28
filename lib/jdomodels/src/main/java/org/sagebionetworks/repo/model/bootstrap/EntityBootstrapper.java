package org.sagebionetworks.repo.model.bootstrap;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.springframework.beans.factory.InitializingBean;

/**
 * An implementation of this interface will bootstrap all entities in the system.
 * 
 * @author jmhill
 *
 */
public interface EntityBootstrapper extends InitializingBean {
	
	/**
	 * The list of entities to be bootstrapped.
	 * @return
	 */
	public List<EntityBootstrapData> getBootstrapEntities();
	
	/**
	 * Builds a map of the user groups.
	 * @return
	 * @throws DatastoreException
	 */
	public Map<DEFAULT_GROUPS, String> buildGroupMap() throws DatastoreException;
	
	/**
	 * What is ACL Scheme should be use for child entities of a given path.
	 * @param path
	 * @return
	 */
	public ACL_SCHEME getChildAclSchemeForPath(String path);

}
