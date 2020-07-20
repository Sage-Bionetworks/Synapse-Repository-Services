package org.sagebionetworks.repo.manager.feature;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.feature.Feature;

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
	 * Checks if the user has access to this feature
	 * 
	 * @param feature The type of feature
	 * @param user    The user to check for
	 * @return True if the user is part of the special testing group or if the feature is enabled (See
	 *         {@link #isFeatureEnabled(Feature)})
	 */
	boolean isFeatureEnabledForUser(Feature feature, UserInfo user);

}
