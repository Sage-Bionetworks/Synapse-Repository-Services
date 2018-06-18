package org.sagebionetworks.doi;

import org.sagebionetworks.StackConfigurationSingleton;

/**
 * Constants for EZID REST APIs.
 */
public class EzidConstants {

	/**
	 * DOI prefix plus the separator (/).
	 */
	public static final String DOI_PREFIX = StackConfigurationSingleton.singleton().getEzidDoiPrefix();

	/**
	 * Synapse web portal URL with protocol and host name and the path prefix '#!Synapse:'.
	 */
	public static final String TARGET_URL_PREFIX = StackConfigurationSingleton.singleton().getEzidTargetUrlPrefix() + "/#!Synapse:";

	/**
	 * DOI Publisher is always Sage Bionetworks.
	 */
	public static final String PUBLISHER = "Synapse";

	/**
	 * Default DOI creator (author) when the corresponding information is missing in Synapse.
	 */
	public static final String DEFAULT_CREATOR = "(author name not available)";

	/**
	 * Base URL (with the trailing slash) for the EZID REST APIs.
	 */
	public static final String EZID_URL = StackConfigurationSingleton.singleton().getEzidUrl();

	/**
	 * EZID account user name.
	 */
	public static final String EZID_USERNAME = StackConfigurationSingleton.singleton().getEzidUsername();

	/**
	 * EZID account password.
	 */
	public static final String EZID_PASSWORD = StackConfigurationSingleton.singleton().getEzidPassword();

	/**
	 * URL (with the trailing slash) for the DOI name resolution service.
	 */
	public static final String DX_URL = "http://dx.doi.org/";
}
