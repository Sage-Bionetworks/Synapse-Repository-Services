package org.sagebionetworks.repo.model.dbo.dao;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.model.EvaluationSubmissions;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.evaluation.AnnotationsDAO;
import org.sagebionetworks.repo.model.evaluation.EvaluationSubmissionsDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class SubmissionStatusAnnotationsAsyncManagerImplTest {
	
	private Submission submission;
	private SubmissionStatus subStatus;

	private AnnotationsDAO mockSubStatusAnnoDAO;
	private EvaluationSubmissionsDAO mockEvaluationSubmissionsDAO;
	private SubmissionStatusAnnotationsAsyncManagerImpl ssAnnoAsyncManager;
	private Annotations annosIn;
	private Annotations expectedAnnosOut;
	private ArgumentCaptor<List> annosCaptor;
	
	private static final String EVAL_ID = "456";
	private static final Long EVAL_ID_AS_LONG = Long.parseLong(EVAL_ID);
	private static final String EVAL_SUB_ETAG = "someEvalSubEtag-00000";
	private static final Long STATUS_VERSION = 7L;
	private static final String TEAM_ID = "1010101";
	
	@Before
	public void before() throws Exception {

		mockSubStatusAnnoDAO = mock(AnnotationsDAO.class);
		mockEvaluationSubmissionsDAO = mock(EvaluationSubmissionsDAO.class);
		
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
		
		annosCaptor = ArgumentCaptor.forClass(List.class);
		EvaluationSubmissions evalSubs = new EvaluationSubmissions();
		evalSubs.setEtag(EVAL_SUB_ETAG);
		when(mockEvaluationSubmissionsDAO.getForEvaluationIfExists(EVAL_ID_AS_LONG)).thenReturn(evalSubs);
		SubmissionBundle bundle = new SubmissionBundle();
		bundle.setSubmission(submission);
		bundle.setSubmissionStatus(subStatus);
		when(mockSubStatusAnnoDAO.getChangedSubmissions(Long.parseLong(EVAL_ID))).
			thenReturn(Collections.singletonList(bundle));
		
		ssAnnoAsyncManager = new SubmissionStatusAnnotationsAsyncManagerImpl(mockSubStatusAnnoDAO, mockEvaluationSubmissionsDAO);
	}
	
	private void insertExpectedAnnos(Annotations annos) {
		annos.setDoubleAnnos(new ArrayList<DoubleAnnotation>());
		annos.setLongAnnos(new ArrayList<LongAnnotation>());
		annos.setStringAnnos(new ArrayList<StringAnnotation>());
		
		// owner ID (the Submission ID)
		LongAnnotation ownerIdAnno = new LongAnnotation();
		ownerIdAnno.setIsPrivate(false);
		ownerIdAnno.setKey(DBOConstants.PARAM_ANNOTATION_OBJECT_ID);
		ownerIdAnno.setValue(KeyFactory.stringToKey(submission.getId()));
		annos.getLongAnnos().add(ownerIdAnno);
		
		// ownerParent ID (the Evaluation ID)
		LongAnnotation ownerParentIdAnno = new LongAnnotation();
		ownerParentIdAnno.setIsPrivate(false);
		ownerParentIdAnno.setKey(DBOConstants.PARAM_ANNOTATION_SCOPE_ID);
		ownerParentIdAnno.setValue(KeyFactory.stringToKey(submission.getEvaluationId()));
		annos.getLongAnnos().add(ownerParentIdAnno);
		
		// creator userId
		LongAnnotation creatorIdAnno = new LongAnnotation();
		creatorIdAnno.setIsPrivate(true);
		creatorIdAnno.setKey(DBOConstants.PARAM_SUBMISSION_USER_ID);
		creatorIdAnno.setValue(KeyFactory.stringToKey(submission.getUserId()));
		annos.getLongAnnos().add(creatorIdAnno);
		
		// submitterAlias
		StringAnnotation submitterAnno = new StringAnnotation();
		submitterAnno.setIsPrivate(true);
		submitterAnno.setKey(DBOConstants.PARAM_SUBMISSION_SUBMITTER_ALIAS);
		submitterAnno.setValue(submission.getSubmitterAlias());
		annos.getStringAnnos().add(submitterAnno);
		
		// entityId
		StringAnnotation entityIdAnno = new StringAnnotation();
		entityIdAnno.setIsPrivate(false);
		entityIdAnno.setKey(DBOConstants.PARAM_SUBMISSION_ENTITY_ID);
		entityIdAnno.setValue(submission.getEntityId());
		annos.getStringAnnos().add(entityIdAnno);
		
		// entity version
		LongAnnotation versionAnno = new LongAnnotation();
		versionAnno.setIsPrivate(false);
		versionAnno.setKey(DBOConstants.PARAM_SUBMISSION_ENTITY_VERSION);
		versionAnno.setValue(submission.getVersionNumber());
		annos.getLongAnnos().add(versionAnno);
		
		// name
		StringAnnotation nameAnno = new StringAnnotation();
		nameAnno.setIsPrivate(true);
		nameAnno.setKey(DBOConstants.PARAM_SUBMISSION_NAME);
		nameAnno.setValue(submission.getName());
		annos.getStringAnnos().add(nameAnno);
		
		// createdOn
		LongAnnotation createdOnAnno = new LongAnnotation();
		createdOnAnno.setIsPrivate(true);
		createdOnAnno.setKey(DBOConstants.PARAM_SUBMISSION_CREATED_ON);
		createdOnAnno.setValue(submission.getCreatedOn().getTime());
		annos.getLongAnnos().add(createdOnAnno);
		
		// modifiedOn
		LongAnnotation modifiedOnAnno = new LongAnnotation();
		modifiedOnAnno.setIsPrivate(false);
		modifiedOnAnno.setKey(DBOConstants.PARAM_SUBSTATUS_MODIFIED_ON);
		modifiedOnAnno.setValue(subStatus.getModifiedOn().getTime());
		annos.getLongAnnos().add(modifiedOnAnno);
		
		// status
		StringAnnotation statusAnno = new StringAnnotation();
		statusAnno.setIsPrivate(false);
		statusAnno.setKey(DBOConstants.PARAM_SUBSTATUS_STATUS);
		statusAnno.setValue(subStatus.getStatus().toString());
		annos.getStringAnnos().add(statusAnno);
		
		// submission Team ID
		LongAnnotation teamAnno = new LongAnnotation();
		teamAnno.setIsPrivate(true);
		teamAnno.setKey(DBOConstants.PARAM_SUBMISSION_TEAM_ID);
		teamAnno.setValue(submission.getTeamId()==null?null:Long.parseLong(submission.getTeamId()));
		annos.getLongAnnos().add(teamAnno);
		
	}
	
	@Test
	public void testStaleCreateChangeMessage() throws Exception {
		EvaluationSubmissions evalSubs = new EvaluationSubmissions();
		evalSubs.setEtag("some other etag");
		when(mockEvaluationSubmissionsDAO.getForEvaluationIfExists(EVAL_ID_AS_LONG)).thenReturn(evalSubs);
		ssAnnoAsyncManager.createEvaluationSubmissionStatuses(submission.getEvaluationId(), EVAL_SUB_ETAG);
		verify(mockSubStatusAnnoDAO, times(0)).deleteAnnotationsByScope(EVAL_ID_AS_LONG);
		verify(mockSubStatusAnnoDAO, times(0)).replaceAnnotations((List<Annotations>)any());		
	}

	@Test
	public void testStaleUpdateChangeMessage() throws Exception {
		EvaluationSubmissions evalSubs = new EvaluationSubmissions();
		evalSubs.setEtag("some other etag");
		when(mockEvaluationSubmissionsDAO.getForEvaluationIfExists(EVAL_ID_AS_LONG)).thenReturn(evalSubs);
		ssAnnoAsyncManager.updateEvaluationSubmissionStatuses(submission.getEvaluationId(), EVAL_SUB_ETAG);
		verify(mockSubStatusAnnoDAO, times(0)).replaceAnnotations((List<Annotations>)any());
		verify(mockSubStatusAnnoDAO, times(0)).deleteAnnotationsByScope(EVAL_ID_AS_LONG);		
	}

	@Test
	public void testStaleCreateChangeMessageNullEtag() throws Exception {
		when(mockEvaluationSubmissionsDAO.getForEvaluationIfExists(EVAL_ID_AS_LONG)).thenReturn(null);
		ssAnnoAsyncManager.createEvaluationSubmissionStatuses(submission.getEvaluationId(), EVAL_SUB_ETAG);		
		verify(mockSubStatusAnnoDAO, times(0)).replaceAnnotations((List<Annotations>)any());
		verify(mockSubStatusAnnoDAO, times(0)).deleteAnnotationsByScope(EVAL_ID_AS_LONG);		
	}
	
	private static void checkAnnoMetadata(Annotations annos, String submissionId) {
		assertEquals(STATUS_VERSION, annos.getVersion());
		assertEquals(submissionId, annos.getObjectId());
		assertEquals(EVAL_ID, annos.getScopeId());
	}

	@Test
	public void testCreateSubmissionStatus() throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		// Annotations will initially be null when the SubmissionStatus object is created
		subStatus.setAnnotations(null);
		
		ssAnnoAsyncManager.createEvaluationSubmissionStatuses(submission.getEvaluationId(), EVAL_SUB_ETAG);
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
		
		ssAnnoAsyncManager.updateEvaluationSubmissionStatuses(submission.getEvaluationId(), EVAL_SUB_ETAG);
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
		// Add a user-defined Annotation that should be overwritten
		List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
		StringAnnotation nameAnno = new StringAnnotation();
		nameAnno.setIsPrivate(true);
		nameAnno.setKey(DBOConstants.PARAM_SUBMISSION_NAME);
		nameAnno.setValue("this isn't the owner ID...");
		stringAnnos.add(nameAnno);
		annosIn.setStringAnnos(stringAnnos);
		// This Annotation should not make it through to the DAO.
		
		ssAnnoAsyncManager.updateEvaluationSubmissionStatuses(submission.getEvaluationId(), EVAL_SUB_ETAG);
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
		ssAnnoAsyncManager.deleteEvaluationSubmissionStatuses(submission.getEvaluationId(), EVAL_SUB_ETAG);
		verify(mockSubStatusAnnoDAO).deleteAnnotationsByScope(Long.parseLong(submission.getEvaluationId()));
	}
}
