package org.sagebionetworks.doi;

/**
 * Constants for EZID REST APIs.
 */
public class EzidConstants {

	/**
	 * DOI prefix plus the separator (/).
	 */
	public static final String DOI_PREFIX = "doi:10.5072/";

	/**
	 * Synapse web portal URL with protocol and host name. Path not included.
	 */
	public static final String TARGET_URL_PREFIX ="https://synapse.prod.sagebase.org/";

	/**
	 * Publisher is always Sage Bionetworks.
	 */
	public static final String PUBLISHER = "Sage Bionetworks";

	/**
	 * Publisher is always Sage Bionetworks.
	 */
	public static final String DEFAULT_CREATOR = "(author name not available)";
}
