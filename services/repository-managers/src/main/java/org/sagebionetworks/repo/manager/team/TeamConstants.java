package org.sagebionetworks.repo.manager.team;

import org.sagebionetworks.repo.model.AuthorizationConstants;

public class TeamConstants {
	public static final Long ADMINISTRATORS_TEAM_ID = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ADMINISTRATORS_GROUP
			.getPrincipalId();
	public static final Long ACT_TEAM_ID = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ACCESS_AND_COMPLIANCE_GROUP
			.getPrincipalId();
	public static final long TRUSTED_MESSAGE_SENDER_TEAM_ID = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.TRUSTED_MESSAGE_SENDER_GROUP
			.getPrincipalId();
	public static final Long SYNAPSE_REPORT_TEAM_ID = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.SYNAPSE_REPORT_GROUP
			.getPrincipalId();
}
