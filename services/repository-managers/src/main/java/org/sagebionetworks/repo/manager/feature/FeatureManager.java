package org.sagebionetworks.repo.manager.feature;

import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.feature.Feature;

/**
 * Manager for feature that can be selectively enabled or disabled
 * 
 * @author Marco Marasca
 */
public interface FeatureManager {

	/**
	 * Global check to test if a feature is enabled in this stack
	 * 
	 * @param feature The type of feature
	 * @return True if a record is found for this feature and the enabled flag is
	 *         true, false otherwise
	 */
	boolean isFeatureEnabled(Feature feature);

	/**
	 * Checks if the user is part of the special {@link BOOTSTRAP_PRINCIPAL#SYNAPSE_TESTING_GROUP} group.
	 * 
	 * @param user The user to check
	 * @return True if the user is part of the {@link BOOTSTRAP_PRINCIPAL#SYNAPSE_TESTING_GROUP}, false otherwise
	 */
	boolean isUserInTestingGroup(UserInfo user);

}
