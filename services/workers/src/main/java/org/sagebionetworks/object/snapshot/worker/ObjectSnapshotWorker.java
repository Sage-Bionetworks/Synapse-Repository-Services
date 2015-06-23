package org.sagebionetworks.object.snapshot.worker;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.model.GroupMembersDAO;
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
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker writes ACL change messages to a file, and put the file to S3 
 */
public class ObjectSnapshotWorker implements MessageDrivenRunner {

	static private Logger log = LogManager.getLogger(ObjectSnapshotWorker.class);
	@Autowired
	private ObjectRecordDAO objectRecordDAO;
	@Autowired
	private UserProfileDAO userProfileDAO;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private TeamDAO teamDAO;
	@Autowired
	private GroupMembersDAO groupMemberDAO;

	// for unit test only
	ObjectSnapshotWorker(ObjectRecordDAO objectRecordDao, UserGroupDAO userGroupDAO,
			UserProfileDAO userProfileDAO, TeamDAO teamDAO, GroupMembersDAO groupMemberDAO) {
		this.objectRecordDAO = objectRecordDao;
		this.userProfileDAO = userProfileDAO;
		this.teamDAO = teamDAO;
		this.userGroupDAO = userGroupDAO;
		this.groupMemberDAO = groupMemberDAO;
	}


	@Override
	public void run(ProgressCallback<Message> progressCallback, Message message) throws IOException{
		// Keep this message invisible
		progressCallback.progressMade(message);

		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		
		if (changeMessage.getChangeType() == ChangeType.DELETE) {
			objectRecordDAO.saveBatch(Arrays.asList(buildObjectRecord(changeMessage, changeMessage)));
			return;
		}
		
		switch (changeMessage.getObjectType()) {
			case PRINCIPAL: 
				processPrincipalRecords(changeMessage);
				return;
			default: 
				return;
		}
	}

	private void processPrincipalRecords(ChangeMessage changeMessage) throws IOException {
		Long principalId = Long.parseLong(changeMessage.getObjectId());
		UserGroup userGroup = null;
		try {
			userGroup = userGroupDAO.get(principalId);
		} catch (NotFoundException e1) {
			log.warn("Principal not found: "+principalId);
			return;
		}
		if(userGroup.getIsIndividual()){
			// User
			UserProfile profile = userProfileDAO.get(changeMessage.getObjectId());
			// what to do in case of exception?
			objectRecordDAO.saveBatch(Arrays.asList(buildObjectRecord(profile, changeMessage)));
		}else{
			// Team
			Team team = teamDAO.get(changeMessage.getObjectId());
			// what to do in case of exception?
			objectRecordDAO.saveBatch(Arrays.asList(buildObjectRecord(team, changeMessage)));
			List<UserGroup> members = groupMemberDAO.getMembers(changeMessage.getObjectId());
			for (UserGroup member : members) {
				TeamMember teamMember = teamDAO.getMember(team.getId(), member.getId());
				objectRecordDAO.saveBatch(Arrays.asList(buildObjectRecord(teamMember, changeMessage)));
			}
		}
	}
	
	public static ObjectRecord buildObjectRecord(JSONEntity entity, ChangeMessage changeMessage) {
		ObjectRecord record = new ObjectRecord();
		record.setChangeNumber(changeMessage.getChangeNumber());
		record.setTimestamp(changeMessage.getTimestamp().getTime());
		record.setObjectType(entity.getClass().getSimpleName());
		try {
			record.setJsonString(EntityFactory.createJSONStringForEntity(entity));
		} catch (JSONObjectAdapterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return record;
	}
}
