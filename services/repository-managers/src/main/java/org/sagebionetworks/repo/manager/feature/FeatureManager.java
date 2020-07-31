package org.sagebionetworks.repo.manager.feature;

import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.feature.FeatureStatus;

/**
 * Manager for feature that can be selectively enabled or disabled
 * 
 * @author Marco Marasca
 */
public interface FeatureManager {

	/**
	 * Fetches the status of the given feature
	 * 
	 * @param feature The name of the feature
	 * @return The status of the given feature
	 */
	FeatureStatus getFeatureStatus(UserInfo user, Feature feature);

	/**
	 * Sets the status of the given feature
	 * 
	 * @param status The status of the feature
	 * @return The updated status
	 */
	FeatureStatus setFeatureStatus(UserInfo user, Feature feature, FeatureStatus status);
	
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
