package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_AND_COMPLIANCE_TEAM_NAME;

import java.util.Collection;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public class ACTUtils {
	public static boolean isACTTeamMembershipOrAdmin(UserInfo userInfo, UserGroupDAO userGroupDAO) throws DatastoreException, UnauthorizedException {
		if (userInfo.isAdmin()) return true;
		UserGroup actTeam = userGroupDAO.findGroup(ACCESS_AND_COMPLIANCE_TEAM_NAME, false);
		return userInfo.getGroups().contains(actTeam);
	}

	public static boolean isACTTeamMemberOrCanCreateOrEdit(UserInfo userInfo, Collection<String> entityIds,
			UserGroupDAO userGroupDAO,
			AuthorizationManager authorizationManager) 
	throws DatastoreException, UnauthorizedException, NotFoundException, InvalidModelException {
		if (isACTTeamMembershipOrAdmin(userInfo, userGroupDAO)) {
			return true;
		}
		if (entityIds.size()==0) return false;
		if (entityIds.size()>1) return false;
		String entityId = entityIds.iterator().next();
		if (!authorizationManager.canAccess(userInfo, entityId, ACCESS_TYPE.CREATE) &&
				!authorizationManager.canAccess(userInfo, entityId, ACCESS_TYPE.UPDATE)) return false;
		return true;
	}
}
