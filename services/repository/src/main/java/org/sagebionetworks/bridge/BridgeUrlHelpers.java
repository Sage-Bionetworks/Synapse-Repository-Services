package org.sagebionetworks.bridge;

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

	public static final String COMMUNITY = "/community";
	public static final String COMMUNITY_ID = "/community/{id}";
	public static final String USER_COMMUNITIES = "/community/joined";
	public static final String COMMUNITY_MEMBERS = "/community/{id}/member";

	public static final String JOIN_COMMUNITY = "/community/{id}/join";
	public static final String LEAVE_COMMUNITY = "/community/{id}/leave";

	public static final String ADD_COMMUNITY_ADMIN = "/community/{id}/member/{principalId}/addadmin";
	public static final String REMOVE_COMMUNITY_ADMIN = "/community/{id}/member/{principalId}/removeadmin";

	public static final String PARTICIPANT_DATA = "/participantData";
	public static final String PARTICIPANT_DATA_ID = "/participantData/{participantDataDescriptorId}";
	public static final String PARTICIPANT_CURRENT_DATA_ID = "/currentParticipantData/{participantDataDescriptorId}";
	public static final String PARTICIPANT_DATA_ROW_ID = "/participantData/{participantDataDescriptorId}/row/{rowId}";
	public static final String PARTICIPANT_DATA_DELETE_ROWS = "/participantData/{participantDataDescriptorId}/deleteRows";
	public static final String APPEND_FOR_PARTICIPANT_DATA = "/participantData/{participantDataId}/participant/{participantId}";
	public static final String SEND_PARTICIPANT_DATA_UPDATES = "/participantData/sendupdates";

	public static final String PARTICIPANT_DATA_DESCRIPTOR = "/participantDataDescriptor";
	public static final String PARTICIPANT_DATA_DESCRIPTOR_ID = "/participantDataDescriptor/{participantDataDescriptorId}";
	public static final String PARTICIPANT_DATA_DESCRIPTOR_WITH_COLUMNS = "/participantDataDescriptorWithColumns";
	public static final String PARTICIPANT_DATA_DESCRIPTOR_WITH_COLUMNS_ID = "/participantDataDescriptorWithColumns/{participantDataDescriptorId}";
	public static final String PARTICIPANT_DATA_COLUMN_DESCRIPTORS = "/participantDataColumnDescriptor";
	public static final String PARTICIPANT_DATA_COLUMN_DESCRIPTORS_ID = "/participantDataColumnDescriptor/{participantDataDescriptorId}";

	public static final String TIME_SERIES = "/timeSeries/{participantDataDescriptorId}";
	public static final String TIME_SERIES_COLUMN_ALIGNED = "/timeSeries/{participantDataDescriptorId}/alignBy/{alignBy}";

	public static final String COLUMNT_NAME = "columnName";
}
