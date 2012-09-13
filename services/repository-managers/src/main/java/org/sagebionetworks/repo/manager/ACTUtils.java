package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_AND_COMPLIANCE_TEAM_NAME;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ForbiddenException;

public class ACTUtils {
	public static void verifyACTTeamMembershipOrIsAdmin(UserInfo userInfo, UserGroupDAO userGroupDAO) throws DatastoreException, ForbiddenException {
		if (userInfo.isAdmin()) return;
		UserGroup actTeam = userGroupDAO.findGroup(ACCESS_AND_COMPLIANCE_TEAM_NAME, false);
		if (!userInfo.getGroups().contains(actTeam)) {
			throw new ForbiddenException("You are not a member of the Synapse Access and Compliance Team.");
		}
	}


}
