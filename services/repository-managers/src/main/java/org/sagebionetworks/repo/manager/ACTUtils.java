package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_AND_COMPLIANCE_TEAM_NAME;

import java.util.Collection;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
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

	public static void verifyACTTeamMembershipOrCanCreateOrEdit(UserInfo userInfo, Collection<String> entityIds,
			UserGroupDAO userGroupDAO,
			AuthorizationManager authorizationManager) 
	throws DatastoreException, ForbiddenException, NotFoundException, InvalidModelException {
		UserGroup actTeam = userGroupDAO.findGroup(ACCESS_AND_COMPLIANCE_TEAM_NAME, false);
		if (userInfo.isAdmin() || userInfo.getGroups().contains(actTeam)) {
			return;
		}
		if (entityIds.size()==0) throw new InvalidModelException("Entity Id required");
		if (entityIds.size()>1) throw new ForbiddenException(
				"You are not a member of the Synapse Access and Compliance Team and cannot set access requirements on multiple entities.");
		String entityId = entityIds.iterator().next();
		if (!authorizationManager.canAccess(userInfo, entityId, ACCESS_TYPE.CREATE) &&
				!authorizationManager.canAccess(userInfo, entityId, ACCESS_TYPE.UPDATE)) {
			throw new ForbiddenException(
					"You are not a member of the Synapse Access and Compliance Team "+
					"and you lack access to "+entityId+".");
		}
	}


}
