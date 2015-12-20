package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupHeader;
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
	private GroupMembersDAO groupMembersDAO;
	@Autowired
	private ObjectRecordDAO objectRecordDAO;
	
	PrincipalObjectRecordWriter(){}
	
	// for unit test only
	PrincipalObjectRecordWriter(UserGroupDAO userGroupDAO, UserProfileDAO userProfileDAO, 
			TeamDAO teamDAO, GroupMembersDAO groupMembersDAO, ObjectRecordDAO objectRecordDAO) {
		this.userGroupDAO = userGroupDAO;
		this.userProfileDAO = userProfileDAO;
		this.teamDAO = teamDAO;
		this.objectRecordDAO = objectRecordDAO;
		this.groupMembersDAO = groupMembersDAO;
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
				// Group
				captureAllMembers(message.getObjectId(), message.getTimestamp().getTime());
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
	 * Log all members that belongs to this group
	 * 
	 * @param groupId
	 * @param timestamp - the timestamp of the change message
	 * @throws IOException 
	 */
	public void captureAllMembers(String groupId, long timestamp) throws IOException {
		List<UserGroup> members = groupMembersDAO.getMembers(groupId);
		List<ObjectRecord> records = new ArrayList<ObjectRecord>();
		for (UserGroup member : members) {
			TeamMember teamMember = new TeamMember();
			teamMember.setTeamId(groupId);
			UserGroupHeader ugh = new UserGroupHeader();
			ugh.setOwnerId(member.getId());
			teamMember.setMember(ugh);
			teamMember.setIsAdmin(false);
			records.add(ObjectRecordBuilderUtils.buildObjectRecord(teamMember, timestamp));
		}
		if (records.size() > 0) {
			objectRecordDAO.saveBatch(records, records.get(0).getJsonClassName());
		}
	}
}
