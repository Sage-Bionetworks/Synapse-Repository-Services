package org.sagebionetworks.repo.model.dbo.dao;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.dbo.dao.SubmissionStatusAnnotationsAsyncManagerImpl.CANCEL_CONTROL;
import static org.sagebionetworks.repo.model.dbo.dao.SubmissionStatusAnnotationsAsyncManagerImpl.CANCEL_REQUESTED;
import static org.sagebionetworks.repo.model.dbo.dao.SubmissionStatusAnnotationsAsyncManagerImpl.CAN_CANCEL;
import static org.sagebionetworks.repo.model.dbo.dao.SubmissionStatusAnnotationsAsyncManagerImpl.SYSTEM_GENERATED_ANNO_IS_PRIVATE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.evaluation.dao.AnnotationsDAO;
import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.model.CancelControl;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

@ExtendWith(MockitoExtension.class)
public class SubmissionStatusAnnotationsAsyncManagerImplTest {
	
	private Submission submission;
	private SubmissionStatus subStatus;

	@Mock
	private AnnotationsDAO mockSubStatusAnnoDAO;
	
	@InjectMocks
	private SubmissionStatusAnnotationsAsyncManagerImpl ssAnnoAsyncManager;
	
	private Annotations annosIn;
	private Annotations expectedAnnosOut;
	private SubmissionBundle bundle;
	
	@Captor
	private ArgumentCaptor<List<Annotations>> annosCaptor;
	
	private static final String EVAL_ID = "456";
	private static final Long EVAL_ID_AS_LONG = Long.parseLong(EVAL_ID);
	private static final Long STATUS_VERSION = 7L;
	private static final String TEAM_ID = "1010101";
	private static final String DOCKER_REPO_NAME = "docker.synapse.org/syn12345/my-repo";
	private static final String DOCKER_DIGEST = "sha256:0123456789abcdef";
	
	@BeforeEach
	public void before() throws Exception {
		
		// Submission
		submission = new Submission();
		submission.setCreatedOn(new Date());
		submission.setEntityId("789");
		submission.setEvaluationId(EVAL_ID);
		submission.setId("123");
		submission.setName("my submission");
		submission.setSubmitterAlias("team awesome");
		submission.setUserId("000");
		submission.setTeamId(TEAM_ID);
		submission.setVersionNumber(1L);
		submission.setDockerDigest("sha256:0123456789abcdef");
		EntityBundle entityBundle = new EntityBundle();
		DockerRepository repository = new DockerRepository();
		repository.setRepositoryName(DOCKER_REPO_NAME);
		entityBundle.setEntity(repository);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		entityBundle.writeToJSONObject(joa);
		submission.setEntityBundleJSON(joa.toJSONString());
		
		// SubmissionStatus
		subStatus = new SubmissionStatus();
		subStatus.setId(submission.getId());
		subStatus.setModifiedOn(new Date());
		subStatus.setStatus(SubmissionStatusEnum.SCORED);
		subStatus.setStatusVersion(STATUS_VERSION);
		
		// provided input Annotations
		annosIn = new Annotations();
		annosIn.setObjectId(submission.getId());
		annosIn.setScopeId(submission.getEvaluationId());
		annosIn.setVersion(STATUS_VERSION);
		subStatus.setAnnotations(annosIn);
		
		// expected output Annotations
		expectedAnnosOut = new Annotations();
		expectedAnnosOut.setObjectId(submission.getId());
		expectedAnnosOut.setScopeId(submission.getEvaluationId());
		expectedAnnosOut.setVersion(STATUS_VERSION);
		insertExpectedAnnos(expectedAnnosOut);
		
		bundle = new SubmissionBundle();
		bundle.setSubmission(submission);
		bundle.setSubmissionStatus(subStatus);
	}
	
	private void insertExpectedAnnos(Annotations annos) throws Exception {
		annos.setDoubleAnnos(new ArrayList<DoubleAnnotation>());
		annos.setLongAnnos(new ArrayList<LongAnnotation>());
		annos.setStringAnnos(new ArrayList<StringAnnotation>());
		
		// owner ID (the Submission ID)
		LongAnnotation ownerIdAnno = new LongAnnotation();
		ownerIdAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		ownerIdAnno.setKey(DBOConstants.PARAM_ANNOTATION_OBJECT_ID);
		ownerIdAnno.setValue(KeyFactory.stringToKey(submission.getId()));
		annos.getLongAnnos().add(ownerIdAnno);
		
		// ownerParent ID (the Evaluation ID)
		LongAnnotation ownerParentIdAnno = new LongAnnotation();
		ownerParentIdAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		ownerParentIdAnno.setKey(DBOConstants.PARAM_ANNOTATION_SCOPE_ID);
		ownerParentIdAnno.setValue(KeyFactory.stringToKey(submission.getEvaluationId()));
		annos.getLongAnnos().add(ownerParentIdAnno);
		
		// creator userId
		LongAnnotation creatorIdAnno = new LongAnnotation();
		creatorIdAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		creatorIdAnno.setKey(DBOConstants.PARAM_SUBMISSION_USER_ID);
		creatorIdAnno.setValue(KeyFactory.stringToKey(submission.getUserId()));
		annos.getLongAnnos().add(creatorIdAnno);
		
		// submitterAlias
		StringAnnotation submitterAnno = new StringAnnotation();
		submitterAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		submitterAnno.setKey(DBOConstants.PARAM_SUBMISSION_SUBMITTER_ALIAS);
		submitterAnno.setValue(submission.getSubmitterAlias());
		annos.getStringAnnos().add(submitterAnno);
		
		// entityId
		StringAnnotation entityIdAnno = new StringAnnotation();
		entityIdAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		entityIdAnno.setKey(DBOConstants.PARAM_SUBMISSION_ENTITY_ID);
		entityIdAnno.setValue(submission.getEntityId());
		annos.getStringAnnos().add(entityIdAnno);
		
		// entity version
		LongAnnotation versionAnno = new LongAnnotation();
		versionAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		versionAnno.setKey(DBOConstants.PARAM_SUBMISSION_ENTITY_VERSION);
		versionAnno.setValue(submission.getVersionNumber());
		annos.getLongAnnos().add(versionAnno);
		
		// name
		StringAnnotation nameAnno = new StringAnnotation();
		nameAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		nameAnno.setKey(DBOConstants.PARAM_SUBMISSION_NAME);
		nameAnno.setValue(submission.getName());
		annos.getStringAnnos().add(nameAnno);
		
		// createdOn
		LongAnnotation createdOnAnno = new LongAnnotation();
		createdOnAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		createdOnAnno.setKey(DBOConstants.PARAM_SUBMISSION_CREATED_ON);
		createdOnAnno.setValue(submission.getCreatedOn().getTime());
		annos.getLongAnnos().add(createdOnAnno);
		
		// modifiedOn
		LongAnnotation modifiedOnAnno = new LongAnnotation();
		modifiedOnAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		modifiedOnAnno.setKey(DBOConstants.PARAM_SUBSTATUS_MODIFIED_ON);
		modifiedOnAnno.setValue(subStatus.getModifiedOn().getTime());
		annos.getLongAnnos().add(modifiedOnAnno);
		
		// status
		StringAnnotation statusAnno = new StringAnnotation();
		statusAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		statusAnno.setKey(DBOConstants.PARAM_SUBSTATUS_STATUS);
		statusAnno.setValue(subStatus.getStatus().toString());
		annos.getStringAnnos().add(statusAnno);
		
		// submission Team ID
		LongAnnotation teamAnno = new LongAnnotation();
		teamAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		teamAnno.setKey(DBOConstants.PARAM_SUBMISSION_TEAM_ID);
		teamAnno.setValue(submission.getTeamId()==null?null:Long.parseLong(submission.getTeamId()));
		annos.getLongAnnos().add(teamAnno);
		
		// repository name 
		StringAnnotation repoNameAnno = new StringAnnotation();
		repoNameAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		repoNameAnno.setKey(SubmissionStatusAnnotationsAsyncManagerImpl.REPOSITORY_NAME);
		repoNameAnno.setValue(DOCKER_REPO_NAME);
		annos.getStringAnnos().add(repoNameAnno);

		// docker digest
		StringAnnotation digestAnno = new StringAnnotation();
		digestAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		digestAnno.setKey(DBOConstants.PARAM_SUBMISSION_DOCKER_DIGEST);
		digestAnno.setValue(DOCKER_DIGEST);
		annos.getStringAnnos().add(digestAnno);

		// canCancel
		StringAnnotation canCancelAnno = new StringAnnotation();
		canCancelAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		canCancelAnno.setKey(CAN_CANCEL);
		canCancelAnno.setValue(Boolean.FALSE.toString());
		annos.getStringAnnos().add(canCancelAnno);

		// cancelRequested
		StringAnnotation cancelRequestedAnno = new StringAnnotation();
		cancelRequestedAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		cancelRequestedAnno.setKey(CANCEL_REQUESTED);
		cancelRequestedAnno.setValue(Boolean.FALSE.toString());
		annos.getStringAnnos().add(cancelRequestedAnno);

		// cancelControl
		CancelControl cancelControl = new CancelControl();
		cancelControl.setCanCancel(false);
		cancelControl.setCancelRequested(false);
		cancelControl.setSubmissionId(submission.getId());
		cancelControl.setUserId(submission.getUserId());
		StringAnnotation cancelControlAnno = new StringAnnotation();
		cancelControlAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		cancelControlAnno.setKey(CANCEL_CONTROL);
		cancelControlAnno.setValue(EntityFactory.createJSONStringForEntity(cancelControl));
		annos.getStringAnnos().add(cancelControlAnno);
		
		// submitterId
		LongAnnotation submitterIdAnno = new LongAnnotation();
		submitterIdAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		submitterIdAnno.setKey(DBOConstants.PARAM_SUBMISSION_SUBMITTER_ID);
		submitterIdAnno.setValue(StringUtils.isEmpty(submission.getTeamId()) ?  Long.parseLong(submission.getUserId()) : Long.parseLong(submission.getTeamId()));
		annos.getLongAnnos().add(submitterIdAnno);
	}
	
	private static void checkAnnoMetadata(Annotations annos, String submissionId) {
		assertEquals(STATUS_VERSION, annos.getVersion());
		assertEquals(submissionId, annos.getObjectId());
		assertEquals(EVAL_ID, annos.getScopeId());
	}

	@Test
	public void testCreateSubmissionStatus() throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		when(mockSubStatusAnnoDAO.getChangedSubmissions(Long.parseLong(EVAL_ID))).thenReturn(Collections.singletonList(bundle));
		
		// Annotations will initially be null when the SubmissionStatus object is created
		subStatus.setAnnotations(null);
		
		ssAnnoAsyncManager.createEvaluationSubmissionStatuses(submission.getEvaluationId());
		verify(mockSubStatusAnnoDAO).replaceAnnotations(annosCaptor.capture());
		verify(mockSubStatusAnnoDAO).deleteAnnotationsByScope(EVAL_ID_AS_LONG);
		Annotations actualAnnosOut = (Annotations)annosCaptor.getValue().get(0);
		assertTrue(actualAnnosOut.getDoubleAnnos().containsAll(expectedAnnosOut.getDoubleAnnos()));
		assertTrue(actualAnnosOut.getLongAnnos().containsAll(expectedAnnosOut.getLongAnnos()));
		assertTrue(actualAnnosOut.getStringAnnos().containsAll(expectedAnnosOut.getStringAnnos()));
		assertTrue(expectedAnnosOut.getDoubleAnnos().containsAll(actualAnnosOut.getDoubleAnnos()));
		assertTrue(expectedAnnosOut.getLongAnnos().containsAll(actualAnnosOut.getLongAnnos()));
		assertTrue(expectedAnnosOut.getStringAnnos().containsAll(actualAnnosOut.getStringAnnos()));
		checkAnnoMetadata(actualAnnosOut, submission.getId());
	}
	
	@Test
	public void testUpdateSubmissionStatus() throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		when(mockSubStatusAnnoDAO.getChangedSubmissions(Long.parseLong(EVAL_ID))).thenReturn(Collections.singletonList(bundle));
		
		// Add some Annotations
		List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
		StringAnnotation sa1 = new StringAnnotation();
		sa1.setIsPrivate(false);
		sa1.setKey("keyOne");
		sa1.setValue("valueOne");
		StringAnnotation sa2 = new StringAnnotation();
		sa2.setIsPrivate(true);
		sa2.setKey("keyTwo");
		sa2.setValue("valueTwo");
		stringAnnos.add(sa2);
		stringAnnos.add(sa1);
		annosIn.setStringAnnos(stringAnnos);		
		// Both of these Annos should pass through to the DAO
		expectedAnnosOut.getStringAnnos().add(sa1);
		expectedAnnosOut.getStringAnnos().add(sa2);
		
		ssAnnoAsyncManager.updateEvaluationSubmissionStatuses(submission.getEvaluationId());
		verify(mockSubStatusAnnoDAO).replaceAnnotations(annosCaptor.capture());
		verify(mockSubStatusAnnoDAO).deleteAnnotationsByScope(EVAL_ID_AS_LONG);
		Annotations actualAnnosOut = (Annotations)annosCaptor.getValue().get(0);
		assertTrue(actualAnnosOut.getDoubleAnnos().containsAll(expectedAnnosOut.getDoubleAnnos()));
		assertTrue(actualAnnosOut.getLongAnnos().containsAll(expectedAnnosOut.getLongAnnos()));
		assertTrue(actualAnnosOut.getStringAnnos().containsAll(expectedAnnosOut.getStringAnnos()));
		assertTrue(expectedAnnosOut.getDoubleAnnos().containsAll(actualAnnosOut.getDoubleAnnos()));
		assertTrue(expectedAnnosOut.getLongAnnos().containsAll(actualAnnosOut.getLongAnnos()));
		assertTrue(expectedAnnosOut.getStringAnnos().containsAll(actualAnnosOut.getStringAnnos()));
		checkAnnoMetadata(actualAnnosOut, submission.getId());
	}
	
	@Test
	public void testUpdateSubmissionStatusOverwrite() throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		when(mockSubStatusAnnoDAO.getChangedSubmissions(Long.parseLong(EVAL_ID))).thenReturn(Collections.singletonList(bundle));
		
		// Add a user-defined Annotation that should be overwritten
		List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
		StringAnnotation nameAnno = new StringAnnotation();
		nameAnno.setIsPrivate(true);
		nameAnno.setKey(DBOConstants.PARAM_SUBMISSION_NAME);
		nameAnno.setValue("this isn't the owner ID...");
		stringAnnos.add(nameAnno);
		annosIn.setStringAnnos(stringAnnos);
		// This Annotation should not make it through to the DAO.
		
		ssAnnoAsyncManager.updateEvaluationSubmissionStatuses(submission.getEvaluationId());
		verify(mockSubStatusAnnoDAO).replaceAnnotations(annosCaptor.capture());
		Annotations actualAnnosOut = (Annotations)annosCaptor.getValue().get(0);
		assertTrue(actualAnnosOut.getDoubleAnnos().containsAll(expectedAnnosOut.getDoubleAnnos()));
		assertTrue(actualAnnosOut.getLongAnnos().containsAll(expectedAnnosOut.getLongAnnos()));
		assertTrue(actualAnnosOut.getStringAnnos().containsAll(expectedAnnosOut.getStringAnnos()));
		assertTrue(expectedAnnosOut.getDoubleAnnos().containsAll(actualAnnosOut.getDoubleAnnos()));
		assertTrue(expectedAnnosOut.getLongAnnos().containsAll(actualAnnosOut.getLongAnnos()));
		assertTrue(expectedAnnosOut.getStringAnnos().containsAll(actualAnnosOut.getStringAnnos()));
		checkAnnoMetadata(actualAnnosOut, submission.getId());
	}
	
	
	@Test
	public void testDeleteSubmission() {
		ssAnnoAsyncManager.deleteEvaluationSubmissionStatuses(submission.getEvaluationId());
		verify(mockSubStatusAnnoDAO).deleteAnnotationsByScope(Long.parseLong(submission.getEvaluationId()));
	}
	
	@Test
	public void testUpdateEvaluationSubmissionStatusSumbitterIdWhenNoTeam() throws Exception{
		when(mockSubStatusAnnoDAO.getChangedSubmissions(Long.parseLong(EVAL_ID))).thenReturn(Collections.singletonList(bundle));
		
		//recalculate the expected annotations with teamID removed
		submission.setTeamId(null);
		insertExpectedAnnos(expectedAnnosOut);
		
		//method under test
		ssAnnoAsyncManager.updateEvaluationSubmissionStatuses(submission.getEvaluationId());
		verify(mockSubStatusAnnoDAO).replaceAnnotations(annosCaptor.capture());
		verify(mockSubStatusAnnoDAO).deleteAnnotationsByScope(EVAL_ID_AS_LONG);
		Annotations actualAnnosOut = (Annotations)annosCaptor.getValue().get(0);
		
		assertTrue(actualAnnosOut.getDoubleAnnos().containsAll(expectedAnnosOut.getDoubleAnnos()));
		assertTrue(actualAnnosOut.getLongAnnos().containsAll(expectedAnnosOut.getLongAnnos()));
		assertTrue(actualAnnosOut.getStringAnnos().containsAll(expectedAnnosOut.getStringAnnos()));
		assertTrue(expectedAnnosOut.getDoubleAnnos().containsAll(actualAnnosOut.getDoubleAnnos()));
		assertTrue(expectedAnnosOut.getLongAnnos().containsAll(actualAnnosOut.getLongAnnos()));
		assertTrue(expectedAnnosOut.getStringAnnos().containsAll(actualAnnosOut.getStringAnnos()));
	}
}
