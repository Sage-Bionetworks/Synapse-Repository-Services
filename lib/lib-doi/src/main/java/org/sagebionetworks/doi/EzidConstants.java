package org.sagebionetworks.doi;

import org.sagebionetworks.StackConfiguration;

/**
 * Constants for EZID REST APIs.
 */
public class EzidConstants {

	/**
	 * DOI prefix plus the separator (/).
	 */
	public static final String DOI_PREFIX = StackConfiguration.getEzidDoiPrefix();

	/**
	 * Synapse web portal URL with protocol and host name and the path prefix '#!Synapse:'.
	 */
	public static final String TARGET_URL_PREFIX = StackConfiguration.getEzidTargetUrlPrefix() + "/#!Synapse:";

	/**
	 * DOI Publisher is always Sage Bionetworks.
	 */
	public static final String PUBLISHER = "Sage Bionetworks";

	/**
	 * Default DOI creator (author) when the corresponding information is missing in Synapse.
	 */
	public static final String DEFAULT_CREATOR = "(author name not available)";

	/**
	 * Base URL (with the trailing slash) for the EZID REST APIs.
	 */
	public static final String EZID_URL = StackConfiguration.getEzidUrl();

	/**
	 * EZID account user name.
	 */
	public static final String EZID_USERNAME = StackConfiguration.getEzidUsername();

	/**
	 * EZID account password.
	 */
	public static final String EZID_PASSWORD = StackConfiguration.getEzidPassword();
}
