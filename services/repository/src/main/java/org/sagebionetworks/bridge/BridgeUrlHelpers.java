package org.sagebionetworks.bridge;

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

	public static final String BASE_V1 = "bridge/v1";
	
	public static final String VERSION = "/version";

	public static final String COMMUNITY = BridgePrefixConst.COMMUNITY;
	public static final String COMMUNITY_ID = COMMUNITY + ID;
	public static final String USER_COMMUNITIES = COMMUNITY + "/joined";
	public static final String COMMUNITY_MEMBERS = COMMUNITY + ID + "/members";

	public static final String JOIN_COMMUNITY = COMMUNITY + ID + "/join";
	public static final String LEAVE_COMMUNITY = COMMUNITY + ID + "/leave";
}
