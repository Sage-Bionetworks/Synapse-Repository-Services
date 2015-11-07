package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
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

public class PrincipalObjectRecordWriter implements ObjectRecordWriter {

	static private Logger log = LogManager.getLogger(PrincipalObjectRecordWriter.class);
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private UserProfileDAO userProfileDAO;
	@Autowired
	private TeamDAO teamDAO;
	@Autowired
	private ObjectRecordDAO objectRecordDAO;
	
	PrincipalObjectRecordWriter(){}
	
	// for unit test only
	PrincipalObjectRecordWriter(UserGroupDAO userGroupDAO, UserProfileDAO userProfileDAO, 
			TeamDAO teamDAO, ObjectRecordDAO objectRecordDAO) {
		this.userGroupDAO = userGroupDAO;
		this.userProfileDAO = userProfileDAO;
		this.teamDAO = teamDAO;
		this.objectRecordDAO = objectRecordDAO;
	}

	@Override
	public void buildAndWriteRecord(ChangeMessage message) throws IOException {
		if (message.getObjectType() != ObjectType.PRINCIPAL || message.getChangeType() == ChangeType.DELETE) {
			throw new IllegalArgumentException();
		}
		Long principalId = Long.parseLong(message.getObjectId());
		UserGroup userGroup = null;
		try {
			userGroup = userGroupDAO.get(principalId);
			ObjectRecord objectRecord = ObjectRecordBuilderUtils.buildObjectRecord(userGroup, message.getTimestamp().getTime());
			objectRecordDAO.saveBatch(Arrays.asList(objectRecord), objectRecord.getJsonClassName());
			// TODO: log all teams this principal belongs to
		} catch (NotFoundException e) {
			log.warn("Principal not found: "+principalId);
		}
		if(userGroup.getIsIndividual()){
			// User
			try {
				UserProfile profile = userProfileDAO.get(message.getObjectId());
				profile.setSummary(null);
				ObjectRecord objectRecord = ObjectRecordBuilderUtils.buildObjectRecord(profile, message.getTimestamp().getTime());
				objectRecordDAO.saveBatch(Arrays.asList(objectRecord), objectRecord.getJsonClassName());
			} catch (NotFoundException e) {
				log.warn("UserProfile not found: "+principalId);
			}
		} else {
			// Team
			try {
				Team team = teamDAO.get(message.getObjectId());
				ObjectRecord objectRecord = ObjectRecordBuilderUtils.buildObjectRecord(team, message.getTimestamp().getTime());
				objectRecordDAO.saveBatch(Arrays.asList(objectRecord), objectRecord.getJsonClassName());
			} catch (NotFoundException e) {
				log.warn("Team not found: "+principalId);
			}
		}
	}
}
