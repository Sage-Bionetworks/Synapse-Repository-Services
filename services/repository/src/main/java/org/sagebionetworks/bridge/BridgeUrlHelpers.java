package org.sagebionetworks.bridge;

import org.sagebionetworks.bridge.model.BridgePrefixConst;
import org.sagebionetworks.repo.web.UrlHelpers;

/**
 * UrlHelpers is responsible for the formatting of all URLs exposed by the service.
 * 
 * The various controllers should not be formatting URLs. They should instead call methods in this helper. Its important
 * to keep URL formulation logic in one place to ensure consistency across the space of URLs that this service supports.
 * 
 */
public class BridgeUrlHelpers extends UrlHelpers {

	public static final String BASE_V1 = "bridge/v1";

	public static final String VERSION = "/version";

	public static final String MEMBER_PATH_VARIABLE = "principalId";
	public static final String MEMBER_ID = "/{" + MEMBER_PATH_VARIABLE + "}";

	public static final String COMMUNITY = BridgePrefixConst.COMMUNITY;
	public static final String COMMUNITY_ID = BridgePrefixConst.COMMUNITY + ID;
	public static final String USER_COMMUNITIES = BridgePrefixConst.COMMUNITY + "/joined";
	public static final String COMMUNITY_MEMBERS = BridgePrefixConst.COMMUNITY + ID + BridgePrefixConst.MEMBER;

	public static final String JOIN_COMMUNITY = BridgePrefixConst.COMMUNITY + ID + "/join";
	public static final String LEAVE_COMMUNITY = BridgePrefixConst.COMMUNITY + ID + "/leave";

	public static final String ADD_COMMUNITY_ADMIN = BridgePrefixConst.COMMUNITY + ID + BridgePrefixConst.MEMBER + MEMBER_ID + "/addadmin";
	public static final String REMOVE_COMMUNITY_ADMIN = BridgePrefixConst.COMMUNITY + ID + BridgePrefixConst.MEMBER + MEMBER_ID
			+ "/removeadmin";

	public static final String PARTICIPANT_DATA_ID_NAME = "/{participantDataId}";
	public static final String PARTICIPANT_DATA_ROW_ID_NAME = "/{rowId}";
	public static final String PARTICIPANT_ID_NAME = "/{participantId}";

	public static final String PARTICIPANT_DATA = BridgePrefixConst.PARTICIPANT_DATA;
	public static final String PARTICIPANT_DATA_ID = BridgePrefixConst.PARTICIPANT_DATA + PARTICIPANT_DATA_ID_NAME;
	public static final String PARTICIPANT_CURRENT_DATA_ID = BridgePrefixConst.PARTICIPANT_DATA_CURRENT + PARTICIPANT_DATA_ID_NAME;
	public static final String PARTICIPANT_DATA_ROW_ID = BridgePrefixConst.PARTICIPANT_DATA + PARTICIPANT_DATA_ID_NAME
			+ BridgePrefixConst.PARTICIPANT_DATA_ROW + PARTICIPANT_DATA_ROW_ID_NAME;
	public static final String APPEND_FOR_PARTICIPANT_DATA = PARTICIPANT_DATA_ID + BridgePrefixConst.PARTICIPANT + PARTICIPANT_ID_NAME;
	public static final String DELETE_FOR_PARTICIPANT_DATA = BridgePrefixConst.PARTICIPANT_DATA + PARTICIPANT_DATA_ID_NAME + "/deleteRows";
	public static final String SEND_PARTICIPANT_DATA_DESCRIPTORS_UPDATES = BridgePrefixConst.PARTICIPANT_DATA_DESCRIPTOR + "/sendupdates";

	public static final String PARTICIPANT_DATA_DESCRIPTOR = BridgePrefixConst.PARTICIPANT_DATA_DESCRIPTOR;
	public static final String PARTICIPANT_DATA_DESCRIPTOR_ID = BridgePrefixConst.PARTICIPANT_DATA_DESCRIPTOR + PARTICIPANT_DATA_ID_NAME;
	public static final String PARTICIPANT_DATA_COLUMN_DESCRIPTORS = BridgePrefixConst.PARTICIPANT_DATA_COLUMN_DESCRIPTOR;
	public static final String PARTICIPANT_DATA_COLUMN_DESCRIPTORS_FOR_PARTICIPANT_DATA_ID = BridgePrefixConst.PARTICIPANT_DATA_COLUMN_DESCRIPTOR
			+ PARTICIPANT_DATA_ID_NAME;
}
