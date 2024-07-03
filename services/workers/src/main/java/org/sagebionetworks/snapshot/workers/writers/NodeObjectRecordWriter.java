package org.sagebionetworks.snapshot.workers.writers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.NodeTranslationUtils;
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
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Translator;
import org.sagebionetworks.repo.model.audit.NodeRecord;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dbo.schema.DerivedAnnotationDao;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NodeObjectRecordWriter implements ObjectRecordWriter {
		
	private static final String KINESIS_STREAM = "nodeSnapshots";
	
	private static Logger log = LogManager.getLogger(NodeObjectRecordWriter.class);

	private NodeDAO nodeDAO;
	private UserManager userManager;
	private DerivedAnnotationDao derivedAnnotationsDao;
	private AccessRequirementDAO accessRequirementDao;
	private EntityAuthorizationManager entityAuthorizationManager;
	private AwsKinesisFirehoseLogger kinesisLogger;
	
	@Autowired
	public NodeObjectRecordWriter(NodeDAO nodeDAO, UserManager userManager, DerivedAnnotationDao derivedAnnotationsDao, AccessRequirementDAO accessRequirementDao,
			EntityAuthorizationManager entityAuthorizationManager, AwsKinesisFirehoseLogger kinesisLogger) {
		this.nodeDAO = nodeDAO;
		this.userManager = userManager;
		this.derivedAnnotationsDao = derivedAnnotationsDao;
		this.accessRequirementDao = accessRequirementDao;
		this.entityAuthorizationManager = entityAuthorizationManager;
		this.kinesisLogger = kinesisLogger;
	}

	/**
	 * set record's isPublic, isRestricted, and isControlled fields
	 * 
	 * @param record

	 * @return a record that contains all data from the passed in record and
	 * addition information about whether the node is public, restricted, and
	 * controlled.
	 */
	private NodeRecord setAccessProperties(NodeRecord record) {

		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		UserEntityPermissions permissions = entityAuthorizationManager.getUserPermissionsForEntity(adminUserInfo, record.getId());

		record.setIsPublic(permissions.getCanPublicRead());

		List<Long> subjectIds = nodeDAO.getEntityPathIds(record.getId());
		
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

	@Override
	public void buildAndWriteRecords(ProgressCallback progressCallback, List<ChangeMessage> messages) throws IOException {
		List<KinesisObjectSnapshotRecord<NodeRecord>> kinesisRecords = new ArrayList<>(messages.size());
		for (ChangeMessage message : messages) {
			if (message.getObjectType() != ObjectType.ENTITY) {
				throw new IllegalArgumentException();
			}

			if(ChangeType.CREATE == message.getChangeType() || ChangeType.UPDATE == message.getChangeType()) {
				try {
					Node node = nodeDAO.getNode(message.getObjectId());
					
					NodeRecord record = new NodeRecord();
					
					// First copy all the standard node properties
					NodeTranslationUtils.copyNodeProperties(node, record);
					
					// Include derived properties
					record.setBenefactorId(nodeDAO.getBenefactor(message.getObjectId()));
					
					nodeDAO.getProjectId(message.getObjectId()).ifPresent(record::setProjectId);

					// Include user assigned annotations
					record.setAnnotations(nodeDAO.getUserAnnotations(record.getId()));

					// Include derived annotations
					derivedAnnotationsDao.getDerivedAnnotations(record.getId()).ifPresent(record::setDerivedAnnotations);
					
					// Include internal entity properties that are stored as annotations, note that we translate to Annotations V2
					// since they are easier to deal with in JSON
					Annotations internalAnnotations = AnnotationsV2Translator.toAnnotationsV2(nodeDAO.getEntityPropertyAnnotations(record.getId())); 
					record.setInternalAnnotations(internalAnnotations);

					// Include derived access properties
					setAccessProperties(record);
					
					kinesisRecords.add(KinesisObjectSnapshotRecord.map(message, record));
					
				} catch (EntityInTrashCanException e) {
					NodeRecord record = new NodeRecord();
					record.setId(message.getObjectId());
					kinesisRecords.add(KinesisObjectSnapshotRecord.map(message, record));					
				} catch (NotFoundException e) {
					log.error("Cannot find node for a " + message.getChangeType() + " message: " + message.toString()) ;
				}
			}
			else if (ChangeType.DELETE == message.getChangeType() && message.getObjectVersion() == null){
				NodeRecord record = new NodeRecord();
				record.setId(message.getObjectId());
				kinesisRecords.add(KinesisObjectSnapshotRecord.map(message, record));
			}
		}
		if (!kinesisRecords.isEmpty()) {
			kinesisLogger.logBatch(KINESIS_STREAM, kinesisRecords);
		}
	}
	
	@Override
	public ObjectType getObjectType() {
		return ObjectType.ENTITY;
	}
}
