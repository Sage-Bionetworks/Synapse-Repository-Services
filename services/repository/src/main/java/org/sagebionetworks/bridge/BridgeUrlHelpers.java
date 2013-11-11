package org.sagebionetworks.bridge;

import java.util.logging.Logger;

import org.sagebionetworks.bridge.model.BridgePrefixConst;
import org.sagebionetworks.repo.web.UrlHelpers;

/**
 * UrlHelpers is responsible for the formatting of all URLs exposed by the
 * service.
 *
 * The various controllers should not be formatting URLs. They should instead
 * call methods in this helper. Its important to keep URL formulation logic in
 * one place to ensure consistency across the space of URLs that this service
 * supports.
 *
 * @author deflaux
 */
public class BridgeUrlHelpers extends UrlHelpers {

	private static final Logger log = Logger.getLogger(BridgeUrlHelpers.class.getName());
	
	public static final String VERSION = "/version";

	public static final String COMMUNITY = BridgePrefixConst.COMMUNITY;

}
