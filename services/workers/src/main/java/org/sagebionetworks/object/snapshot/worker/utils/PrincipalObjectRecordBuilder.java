package org.sagebionetworks.object.snapshot.worker.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
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
	public ObjectRecord build(ChangeMessage message) throws JSONObjectAdapterException {
		if (message.getObjectType() != ObjectType.PRINCIPAL || message.getChangeType() == ChangeType.DELETE) {
			throw new IllegalArgumentException();
		}
		Long principalId = Long.parseLong(message.getObjectId());
		UserGroup userGroup = null;
		try {
			userGroup = userGroupDAO.get(principalId);
		} catch (NotFoundException e) {
			log.warn("Principal not found: "+principalId);
			return null;
		}
		if(userGroup.getIsIndividual()){
			// User
			try {
				UserProfile profile = userProfileDAO.get(message.getObjectId());
				return ObjectRecordBuilderUtils.buildObjectRecord(profile, message);
			} catch (NotFoundException e) {
				log.warn("UserProfile not found: "+principalId);
				return null;
			}
		} else {
			// Team
			try {
				Team team = teamDAO.get(message.getObjectId());
				return ObjectRecordBuilderUtils.buildObjectRecord(team, message);
			} catch (NotFoundException e) {
				log.warn("Team not found: "+principalId);
				return null;
			}
		}
	}
}
