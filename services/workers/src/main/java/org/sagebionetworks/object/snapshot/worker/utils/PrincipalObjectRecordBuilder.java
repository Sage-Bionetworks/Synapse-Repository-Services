package org.sagebionetworks.object.snapshot.worker.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class PrincipalObjectRecordBuilder implements ObjectRecordBuilder {

	static private Logger log = LogManager.getLogger(PrincipalObjectRecordBuilder.class);
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private UserProfileDAO userProfileDAO;
	@Autowired
	private TeamDAO teamDAO;
	
	PrincipalObjectRecordBuilder(){}
	
	// for unit test only
	PrincipalObjectRecordBuilder(UserGroupDAO userGroupDAO, UserProfileDAO userProfileDAO, TeamDAO teamDAO) {
		this.userGroupDAO = userGroupDAO;
		this.userProfileDAO = userProfileDAO;
		this.teamDAO = teamDAO;
	}

	@Override
	public List<ObjectRecord> build(ChangeMessage message) {
		if (message.getObjectType() != ObjectType.PRINCIPAL || message.getChangeType() == ChangeType.DELETE) {
			throw new IllegalArgumentException();
		}
		Long principalId = Long.parseLong(message.getObjectId());
		UserGroup userGroup = null;
		List<ObjectRecord> list = new ArrayList<ObjectRecord>();
		try {
			userGroup = userGroupDAO.get(principalId);
			list.add(ObjectRecordBuilderUtils.buildObjectRecord(userGroup, message.getTimestamp().getTime()));
		} catch (NotFoundException e) {
			log.warn("Principal not found: "+principalId);
		}
		if(userGroup.getIsIndividual()){
			// User
			try {
				UserProfile profile = userProfileDAO.get(message.getObjectId());
				profile.setSummary(null);
				list.add(ObjectRecordBuilderUtils.buildObjectRecord(profile, message.getTimestamp().getTime()));
			} catch (NotFoundException e) {
				log.warn("UserProfile not found: "+principalId);
			}
		} else {
			// Team
			try {
				Team team = teamDAO.get(message.getObjectId());
				list.add(ObjectRecordBuilderUtils.buildObjectRecord(team, message.getTimestamp().getTime()));
			} catch (NotFoundException e) {
				log.warn("Team not found: "+principalId);
			}
		}
		return list;
	}
}
