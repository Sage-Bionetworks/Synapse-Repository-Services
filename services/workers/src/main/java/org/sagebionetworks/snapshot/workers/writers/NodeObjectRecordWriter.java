package org.sagebionetworks.snapshot.workers.writers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.DeletedNode;
import org.sagebionetworks.repo.model.audit.NodeRecord;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NodeObjectRecordWriter implements ObjectRecordWriter {
		
	private static final String KINESIS_STREAM = "nodeSnapshots";
	
	private static Logger log = LogManager.getLogger(NodeObjectRecordWriter.class);

	private NodeDAO nodeDAO;
	private UserManager userManager;
	private AccessRequirementDAO accessRequirementDao;
	private EntityAuthorizationManager entityAuthorizationManager;
	private ObjectRecordDAO objectRecordDAO;
	private AwsKinesisFirehoseLogger kinesisLogger;
	
	@Autowired
	public NodeObjectRecordWriter(NodeDAO nodeDAO, UserManager userManager, AccessRequirementDAO accessRequirementDao,
			EntityAuthorizationManager entityAuthorizationManager, ObjectRecordDAO objectRecordDAO, AwsKinesisFirehoseLogger kinesisLogger) {
		this.nodeDAO = nodeDAO;
		this.userManager = userManager;
		this.accessRequirementDao = accessRequirementDao;
		this.entityAuthorizationManager = entityAuthorizationManager;
		this.objectRecordDAO = objectRecordDAO;
		this.kinesisLogger = kinesisLogger;
	}

	/**
	 * set record's isPublic, isRestricted, and isControlled fields
	 * 
	 * @param record
	 * @param userManager
	 * @param accessRequirementManager
	 * @param entityPermissionManager
	 * @return a record that contains all data from the passed in record and
	 * addition information about whether the node is public, restricted, and
	 * controlled.
	 */
	private NodeRecord setAccessProperties(NodeRecord record,
			UserManager userManager,
			AccessRequirementDAO accessRequirementDao,
			EntityAuthorizationManager entityAuthorizationManager,
			NodeDAO nodeDao) {

		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		UserEntityPermissions permissions = entityAuthorizationManager.getUserPermissionsForEntity(adminUserInfo, record.getId());

		record.setIsPublic(permissions.getCanPublicRead());

		List<Long> subjectIds = nodeDao.getEntityPathIds(record.getId());
		
		AccessRequirementStats stats = accessRequirementDao.getAccessRequirementStats(subjectIds, RestrictableObjectType.ENTITY);

		record.setIsRestricted(stats.getHasToU());
		record.setIsControlled(stats.getHasACT());
		
		List<Long> effectiveArs = stats.getRequirementIdSet()
			.stream()
			.map(Long::valueOf)
			.sorted()
			.collect(Collectors.toList());
		
		record.setEffectiveArs(effectiveArs);
		
		return record;
	}

	/**
	 * Build a NodeRecord that wrap around Node object
	 * 
	 * @param node
	 * @return
	 */
	public static NodeRecord buildNodeRecord(Node node, String benefactorId, String projectId) {
		NodeRecord record = new NodeRecord();
		record.setId(node.getId());
		record.setBenefactorId(benefactorId);
		record.setProjectId(projectId);
		record.setParentId(node.getParentId());
		record.setNodeType(node.getNodeType());
		record.setCreatedOn(node.getCreatedOn());
		record.setCreatedByPrincipalId(node.getCreatedByPrincipalId());
		record.setModifiedOn(node.getModifiedOn());
		record.setModifiedByPrincipalId(node.getModifiedByPrincipalId());
		record.setVersionNumber(node.getVersionNumber());
		record.setFileHandleId(node.getFileHandleId());
		record.setName(node.getName());
		return record;
	}

	@Override
	public void buildAndWriteRecords(ProgressCallback progressCallback, List<ChangeMessage> messages) throws IOException {
		List<ObjectRecord> nonDeleteRecords = new LinkedList<ObjectRecord>();
		List<ObjectRecord> deleteRecords = new LinkedList<ObjectRecord>();
		List<KinesisObjectSnapshotRecord<NodeRecord>> kinesisRecords = new ArrayList<>(messages.size());
		for (ChangeMessage message : messages) {
			if (message.getObjectType() != ObjectType.ENTITY) {
				throw new IllegalArgumentException();
			}
			if (message.getChangeType() == ChangeType.DELETE) {
				deleteRecords.add(buildDeletedNodeRecord(message));
			
				NodeRecord record = new NodeRecord();
				record.setId(message.getObjectId());
				kinesisRecords.add(KinesisObjectSnapshotRecord.map(message, record));
				
			} else {
				try {
					Node node = nodeDAO.getNode(message.getObjectId());
					String benefactorId = nodeDAO.getBenefactor(message.getObjectId());
					String projectId = nodeDAO.getProjectId(message.getObjectId()).orElseThrow(() -> new NotFoundException("Project id does not exists."));
					NodeRecord record = buildNodeRecord(node, benefactorId, projectId);
					record = setAccessProperties(record, userManager, accessRequirementDao, entityAuthorizationManager, nodeDAO);
					ObjectRecord objectRecord = ObjectRecordBuilderUtils.buildObjectRecord(record, message.getTimestamp().getTime());
					nonDeleteRecords.add(objectRecord);
					
					kinesisRecords.add(KinesisObjectSnapshotRecord.map(message, record));
					
				} catch (EntityInTrashCanException e) {
					deleteRecords.add(buildDeletedNodeRecord(message));
					
					NodeRecord record = new NodeRecord();
					record.setId(message.getObjectId());
					kinesisRecords.add(KinesisObjectSnapshotRecord.map(message, record));					
				} catch (NotFoundException e) {
					log.error("Cannot find node for a " + message.getChangeType() + " message: " + message.toString()) ;
				}
			}
		}
		if (!nonDeleteRecords.isEmpty()) {
			objectRecordDAO.saveBatch(nonDeleteRecords, nonDeleteRecords.get(0).getJsonClassName());
		}
		if (!deleteRecords.isEmpty()) {
			objectRecordDAO.saveBatch(deleteRecords, deleteRecords.get(0).getJsonClassName());
		}
		if (!kinesisRecords.isEmpty()) {
			kinesisLogger.logBatch(KINESIS_STREAM, kinesisRecords);
		}
	}
	
	@Override
	public ObjectType getObjectType() {
		return ObjectType.ENTITY;
	}
	
	public static ObjectRecord buildDeletedNodeRecord(ChangeMessage message) throws IOException {
		DeletedNode deletedNode = new DeletedNode();
		deletedNode.setId(message.getObjectId());
		ObjectRecord objectRecord = ObjectRecordBuilderUtils.buildObjectRecord(deletedNode, message.getTimestamp().getTime());
		return objectRecord;
	}
}
