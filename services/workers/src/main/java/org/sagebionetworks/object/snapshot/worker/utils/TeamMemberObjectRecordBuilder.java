package org.sagebionetworks.object.snapshot.worker.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class TeamMemberObjectRecordBuilder implements ObjectRecordBuilder{
	
	static private Logger log = LogManager.getLogger(TeamMemberObjectRecordBuilder.class);
	@Autowired
	private TeamDAO teamDAO;

	TeamMemberObjectRecordBuilder(){}
	
	// for unit test only
	public TeamMemberObjectRecordBuilder(TeamDAO mockTeamDAO) {
		this.teamDAO = mockTeamDAO;
	}

	@Override
	public ObjectRecord build(ChangeMessage message) {
		if (message.getObjectType() != ObjectType.TEAM_MEMBER || message.getChangeType() == ChangeType.DELETE) {
			throw new IllegalArgumentException();
		}
		try {
			TeamMember teamMember = teamDAO.getMember(message.getParentId(), message.getObjectId());
			return ObjectRecordBuilderUtils.buildObjectRecord(teamMember, message.getTimestamp().getTime());

		} catch (NotFoundException e) {
			log.warn("Team member not found. TeamId = "+message.getParentId()+" principalId = "+message.getObjectId());
			return null;
		}
	}

}
