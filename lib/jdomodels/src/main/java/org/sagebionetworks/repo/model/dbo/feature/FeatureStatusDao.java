package org.sagebionetworks.repo.model.dbo.feature;

import java.util.Optional;

public interface FeatureStatusDao {

	/**
	 * @param feature The feature type
	 * @return Check if the given feature has the enabled flag set to true, if not
	 *         record can be found returns an empty optional
	 */
	Optional<Boolean> isFeatureEnabled(Feature feature);

	// For testing

	/**
	 * Enabled/Disable the given feature
	 * 
	 * @param feature
	 * @param enabled
	 */
	void setFeatureEnabled(Feature feature, boolean enabled);

	void clear();

}
