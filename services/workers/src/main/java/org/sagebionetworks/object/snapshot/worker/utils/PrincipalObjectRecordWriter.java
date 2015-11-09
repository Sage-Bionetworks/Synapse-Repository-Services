package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
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

	private final long LIMIT = 100;
	
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
			captureAllTeams(principalId, LIMIT, message.getTimestamp().getTime());

			if(userGroup.getIsIndividual()){
				// User
				try {
					UserProfile profile = userProfileDAO.get(message.getObjectId());
					profile.setSummary(null);
					ObjectRecord upRecord = ObjectRecordBuilderUtils.buildObjectRecord(profile, message.getTimestamp().getTime());
					objectRecordDAO.saveBatch(Arrays.asList(upRecord), upRecord.getJsonClassName());
				} catch (NotFoundException e) {
					log.warn("UserProfile not found: "+principalId);
				}
			} else {
				// Team
				try {
					Team team = teamDAO.get(message.getObjectId());
					ObjectRecord teamRecord = ObjectRecordBuilderUtils.buildObjectRecord(team, message.getTimestamp().getTime());
					objectRecordDAO.saveBatch(Arrays.asList(teamRecord), teamRecord.getJsonClassName());
				} catch (NotFoundException e) {
					log.warn("Team not found: "+principalId);
				}
			}

		} catch (NotFoundException e) {
			log.warn("Principal not found: "+principalId);
		}
	}

	/**
	 * Log all teams that this principal belongs to
	 * @param principalId
	 * @param limit - the max number of teams will be written in to a log file at a time
	 * @param timestamp - the timestamp of the change message
	 * @throws IOException 
	 */
	public void captureAllTeams(Long principalId, long limit, long timestamp) throws IOException {
		long offset = 0;
		long numberOfTeams = teamDAO.getCountForMember(principalId.toString());
		
		while (offset < numberOfTeams) {
			List<Team> teams = teamDAO.getForMemberInRange(principalId.toString(), limit, offset);
			List<ObjectRecord> records = new ArrayList<ObjectRecord>();
			for (Team team : teams) {
				TeamMember teamMember = teamDAO.getMember(team.getId(), principalId.toString());
				records.add(ObjectRecordBuilderUtils.buildObjectRecord(teamMember, timestamp));
			}
			if (records.size() > 0) {
				objectRecordDAO.saveBatch(records, records.get(0).getJsonClassName());
			}
			offset += limit;
		}
	}
}
