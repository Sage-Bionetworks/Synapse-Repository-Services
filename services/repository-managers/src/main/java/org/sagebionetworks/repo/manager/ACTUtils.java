package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_AND_COMPLIANCE_TEAM_NAME;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;

public class ACTUtils {
	public static void verifyACTTeamMembershipOrIsAdmin(UserInfo userInfo, UserGroupDAO userGroupDAO) throws DatastoreException, ForbiddenException {
		if (userInfo.isAdmin()) return;
		UserGroup actTeam = userGroupDAO.findGroup(ACCESS_AND_COMPLIANCE_TEAM_NAME, false);
		if (!userInfo.getGroups().contains(actTeam)) {
			throw new ForbiddenException("You are not a member of the Synapse Access and Compliance Team.");
		}
	}

	public static void verifyACTTeamMembershipOrCanCreateOrEdit(UserInfo userInfo, List<String> entityIds,
			UserGroupDAO userGroupDAO,
			AuthorizationManager authorizationManager) throws DatastoreException, ForbiddenException, NotFoundException {
		UserGroup actTeam = userGroupDAO.findGroup(ACCESS_AND_COMPLIANCE_TEAM_NAME, false);
		if (userInfo.getGroups().contains(actTeam)) {
			return;
		}
		List<String> unauthorized = new ArrayList<String>();
		for (String entityId : entityIds) {
			if (!authorizationManager.canAccess(userInfo, entityId, ACCESS_TYPE.CREATE) &&
					!authorizationManager.canAccess(userInfo, entityId, ACCESS_TYPE.UPDATE)) {
				unauthorized.add(entityId);
			}
		}
		if (!unauthorized.isEmpty()) {
			throw new ForbiddenException(
				"You are not a member of the Synapse Access and Compliance Team "+
				"and you lack access to "+unauthorized+".");
		}
	}


}
