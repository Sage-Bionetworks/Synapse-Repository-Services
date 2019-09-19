package org.sagebionetworks.repo.manager.oauth.claimprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.springframework.beans.factory.annotation.Autowired;

public class TeamClaimProvider implements OIDCClaimProvider {
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Override
	public OIDCClaimName getName() {
		return OIDCClaimName.team;
	}

	@Override
	public String getDescription() {
		return "Your team membership";
	}

	@Override
	public Object getClaim(String userId, OIDCClaimsRequestDetails details) {
		if (details==null) {
			return Collections.EMPTY_LIST;
		}
		Set<String> requestedTeamIds = new HashSet<String>();
		if (StringUtils.isNotEmpty(details.getValue())) {
			requestedTeamIds.add(details.getValue());
		}
		if (details.getValues()!=null) {
			requestedTeamIds.addAll(details.getValues());
		}
		return new ArrayList<String>(getMemberTeamIds(userId, requestedTeamIds));
	}

	/*
	 * return the subset of team Ids in which the given user is a member
	 */
	private Set<String> getMemberTeamIds(String userId, Set<String> requestedTeamIds) {
		List<String> actualTeamIds = groupMembersDAO.queryGroups(userId, new ArrayList<String>(requestedTeamIds));
		Set<String> result = new HashSet<String>(actualTeamIds);
		return result;
	}


}
