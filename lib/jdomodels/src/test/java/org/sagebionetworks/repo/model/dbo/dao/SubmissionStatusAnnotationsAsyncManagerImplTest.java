package org.sagebionetworks.repo.model.dbo.dao;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sagebionetworks.evaluation.dao.AnnotationsDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class SubmissionStatusAnnotationsAsyncManagerImplTest {
	
	private Submission submission;
	private SubmissionStatus subStatus;

	private SubmissionDAO mockSubmissionDAO;
	private SubmissionStatusDAO mockSubmissionStatusDAO;
	private AnnotationsDAO mockSubStatusAnnoDAO;
	private SubmissionStatusAnnotationsAsyncManagerImpl ssAnnoAsyncManager;
	private Annotations annosIn;
	private Annotations expectedAnnosOut;
	private ArgumentCaptor<Annotations> annosCaptor;
	
	@Before
	public void before() throws DatastoreException, NotFoundException, UnsupportedEncodingException, JSONObjectAdapterException{

		mockSubmissionDAO = mock(SubmissionDAO.class);
		mockSubmissionStatusDAO = mock(SubmissionStatusDAO.class);
		mockSubStatusAnnoDAO = mock(AnnotationsDAO.class);
		
		// Submission
		submission = new Submission();
		submission.setCreatedOn(new Date());
		submission.setEntityId("789");
		submission.setEvaluationId("456");
		submission.setId("123");
		submission.setName("my submission");
		submission.setSubmitterAlias("team awesome");
		submission.setUserId("000");
		submission.setVersionNumber(1L);
		
		// SubmissionStatus
		subStatus = new SubmissionStatus();
		subStatus.setId(submission.getId());
		subStatus.setModifiedOn(new Date());
		subStatus.setStatus(SubmissionStatusEnum.SCORED);
		
		// provided input Annotations
		annosIn = new Annotations();
		annosIn.setObjectId(submission.getId());
		annosIn.setScopeId(submission.getEvaluationId());
		subStatus.setAnnotations(annosIn);
		
		// expected output Annotations
		expectedAnnosOut = new Annotations();
		expectedAnnosOut.setObjectId(submission.getId());
		expectedAnnosOut.setScopeId(submission.getEvaluationId());
		insertExpectedAnnos(expectedAnnosOut);
		
		annosCaptor = ArgumentCaptor.forClass(Annotations.class);
		
		when(mockSubmissionDAO.get(eq(submission.getId()))).thenReturn(submission);
		when(mockSubmissionStatusDAO.get(eq(submission.getId()))).thenReturn(subStatus);
		
		ssAnnoAsyncManager = new SubmissionStatusAnnotationsAsyncManagerImpl(mockSubmissionDAO,
				mockSubmissionStatusDAO, mockSubStatusAnnoDAO);
		
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
		LongAnnotation entityIdAnno = new LongAnnotation();
		entityIdAnno.setIsPrivate(false);
		entityIdAnno.setKey(DBOConstants.PARAM_SUBMISSION_ENTITY_ID);
		entityIdAnno.setValue(KeyFactory.stringToKey(submission.getEntityId()));
		annos.getLongAnnos().add(entityIdAnno);
		
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
	}

	@Test
	public void testCreateSubmissionStatus() throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		// Annotations will initially be null when the SubmissionStatus object is created
		subStatus.setAnnotations(null);
		
		ssAnnoAsyncManager.updateSubmissionStatus(submission.getId());
		verify(mockSubStatusAnnoDAO).replaceAnnotations(annosCaptor.capture());
		Annotations actualAnnosOut = annosCaptor.getValue();
		assertTrue(actualAnnosOut.getDoubleAnnos().containsAll(expectedAnnosOut.getDoubleAnnos()));
		assertTrue(actualAnnosOut.getLongAnnos().containsAll(expectedAnnosOut.getLongAnnos()));
		assertTrue(actualAnnosOut.getStringAnnos().containsAll(expectedAnnosOut.getStringAnnos()));
		assertTrue(expectedAnnosOut.getDoubleAnnos().containsAll(actualAnnosOut.getDoubleAnnos()));
		assertTrue(expectedAnnosOut.getLongAnnos().containsAll(actualAnnosOut.getLongAnnos()));
		assertTrue(expectedAnnosOut.getStringAnnos().containsAll(actualAnnosOut.getStringAnnos()));
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
		
		ssAnnoAsyncManager.updateSubmissionStatus(submission.getId());
		verify(mockSubStatusAnnoDAO).replaceAnnotations(annosCaptor.capture());
		Annotations actualAnnosOut = annosCaptor.getValue();
		assertTrue(actualAnnosOut.getDoubleAnnos().containsAll(expectedAnnosOut.getDoubleAnnos()));
		assertTrue(actualAnnosOut.getLongAnnos().containsAll(expectedAnnosOut.getLongAnnos()));
		assertTrue(actualAnnosOut.getStringAnnos().containsAll(expectedAnnosOut.getStringAnnos()));
		assertTrue(expectedAnnosOut.getDoubleAnnos().containsAll(actualAnnosOut.getDoubleAnnos()));
		assertTrue(expectedAnnosOut.getLongAnnos().containsAll(actualAnnosOut.getLongAnnos()));
		assertTrue(expectedAnnosOut.getStringAnnos().containsAll(actualAnnosOut.getStringAnnos()));
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
		
		ssAnnoAsyncManager.updateSubmissionStatus(submission.getId());
		verify(mockSubStatusAnnoDAO).replaceAnnotations(annosCaptor.capture());
		Annotations actualAnnosOut = annosCaptor.getValue();
		assertTrue(actualAnnosOut.getDoubleAnnos().containsAll(expectedAnnosOut.getDoubleAnnos()));
		assertTrue(actualAnnosOut.getLongAnnos().containsAll(expectedAnnosOut.getLongAnnos()));
		assertTrue(actualAnnosOut.getStringAnnos().containsAll(expectedAnnosOut.getStringAnnos()));
		assertTrue(expectedAnnosOut.getDoubleAnnos().containsAll(actualAnnosOut.getDoubleAnnos()));
		assertTrue(expectedAnnosOut.getLongAnnos().containsAll(actualAnnosOut.getLongAnnos()));
		assertTrue(expectedAnnosOut.getStringAnnos().containsAll(actualAnnosOut.getStringAnnos()));
	}
	
	
	@Test
	public void testDeleteSubmission() {
		ssAnnoAsyncManager.deleteSubmission(submission.getId());
		verify(mockSubStatusAnnoDAO).deleteAnnotationsByOwnerId(Long.parseLong(submission.getId()));
	}
}
