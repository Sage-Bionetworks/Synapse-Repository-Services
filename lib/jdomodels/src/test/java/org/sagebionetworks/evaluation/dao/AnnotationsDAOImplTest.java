package org.sagebionetworks.evaluation.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AnnotationsDAOImplTest {

	@Autowired
	AnnotationsDAO subStatusAnnoDAO;
    @Autowired
    SubmissionDAO submissionDAO;
    @Autowired
    SubmissionStatusDAO submissionStatusDAO;
    @Autowired
    EvaluationDAO evaluationDAO;
	@Autowired
	NodeDAO nodeDAO;
	
	private String nodeId;
	private String userId;
    private List<String> submissionIds;
    private String submissionStatusId;
    private String evalId;
    private static final String SUBMISSION_NAME = "test submission";
    private static final Long VERSION_NUMBER = 1L;
	
	@After
	public void after() throws DatastoreException {
		subStatusAnnoDAO.deleteAnnotationsByScope(Long.parseLong(evalId));
    	try {
    		nodeDAO.delete(nodeId);
    	} catch (NotFoundException e) {};
    	for (String submissionId : submissionIds) {
			try {
				submissionDAO.delete(submissionId);
			} catch (NotFoundException e)  {};
    	}
		try {
			if (submissionStatusId!=null) submissionStatusDAO.delete(submissionStatusId);
		} catch (NotFoundException e)  {};
		try {
			evaluationDAO.delete(evalId);
		} catch (NotFoundException e) {};
	}
	
	@Before
	public void before() throws DatastoreException, InvalidModelException, NotFoundException {
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString(); 
						
    	// create a node
  		Node toCreate = NodeTestUtils.createNew(SUBMISSION_NAME, Long.parseLong(userId));
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("0.0.1");
    	nodeId = nodeDAO.createNew(toCreate).substring(3); // trim "syn" from node ID
    	
    	// create an evaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setId("1234");
        evaluation.setEtag("etag");
        evaluation.setName("name");
        evaluation.setOwnerId(userId);
        evaluation.setCreatedOn(new Date());
        evaluation.setContentSource(nodeId);
        evalId = evaluationDAO.create(evaluation, Long.parseLong(userId));
        
        submissionIds = new ArrayList<String>();
	}
	
	private String createSubmission(String id, String name, String nodeId, String userId) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setName(name);
        submission.setEntityId(nodeId);
        submission.setVersionNumber(VERSION_NUMBER);
        submission.setUserId(userId);
        submission.setEvaluationId(evalId);
        submission.setCreatedOn(new Date());
        submission.setEntityBundleJSON("some bundle");
        return submissionDAO.create(submission);
	}
	
	private static Annotations createAnnotations(String evalId, String submissionId) {
		Annotations annos = new Annotations();
		annos.setObjectId(submissionId);
		annos.setScopeId(evalId);
		annos.setVersion(0L);
		annos.setDoubleAnnos(new ArrayList<DoubleAnnotation>());
		annos.setLongAnnos(new ArrayList<LongAnnotation>());
		annos.setStringAnnos(new ArrayList<StringAnnotation>());
		return annos;
	}
	
	private void checkAnnotationsMetadata(Annotations annotations, String submissionId) {
		assertEquals(submissionId, annotations.getObjectId());
		assertEquals(evalId, annotations.getScopeId());
	}
	
	@Test
	public void testStringAnnotations() throws DatastoreException, JSONObjectAdapterException, NotFoundException{		        
        // create a submission
        String submissionId = createSubmission("5678", SUBMISSION_NAME, nodeId, userId);
        submissionIds.add(submissionId);       
        // create Annotations
        Annotations annos = createAnnotations(evalId, submissionId);
		// Create
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
		annos.setStringAnnos(stringAnnos);
		subStatusAnnoDAO.replaceAnnotations(Collections.singletonList(annos));
		
		// Read
		Annotations clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
		checkAnnotationsMetadata(clone, submissionId);
		assertTrue(clone.getStringAnnos().containsAll(annos.getStringAnnos()));
		assertTrue(annos.getStringAnnos().containsAll(clone.getStringAnnos()));
		
		// Update
		stringAnnos = new ArrayList<StringAnnotation>();
		StringAnnotation sa3 = new StringAnnotation();
		sa3.setIsPrivate(false);
		sa3.setKey("keyThree");
		sa3.setValue("valueThree");
		sa1.setValue("valueOne_modified");
		stringAnnos.add(sa1);
		stringAnnos.add(sa3);
		annos.setStringAnnos(stringAnnos);
		subStatusAnnoDAO.replaceAnnotations(Collections.singletonList(annos));
		
		// Verify
		Annotations clone2 = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone2);
		checkAnnotationsMetadata(clone2, submissionId);
		assertTrue(clone2.getStringAnnos().containsAll(annos.getStringAnnos()));
		assertFalse(clone.getStringAnnos().containsAll(clone2.getStringAnnos()));

		// Delete
		submissionDAO.delete(submissionId);
		subStatusAnnoDAO.deleteAnnotationsByScope(Long.parseLong(evalId));
		clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
		assertEquals(0, clone.getStringAnnos().size());
	}
	
	@Test
	public void testLongAnnotations() throws DatastoreException, JSONObjectAdapterException, NotFoundException{
        // create a submission
        String submissionId = createSubmission("5678", SUBMISSION_NAME, nodeId, userId);
        submissionIds.add(submissionId);       
        // create Annotations
        Annotations annos = createAnnotations(evalId, submissionId);
		// Create
		List<LongAnnotation> longAnnos = new ArrayList<LongAnnotation>();
		LongAnnotation la1 = new LongAnnotation();
		la1.setIsPrivate(false);
		la1.setKey("keyOne");
		la1.setValue(1L);
		LongAnnotation la2 = new LongAnnotation();
		la2.setIsPrivate(true);
		la2.setKey("keyTwo");
		la2.setValue(2L);
		longAnnos.add(la1);
		longAnnos.add(la2);
		annos.setLongAnnos(longAnnos);
		subStatusAnnoDAO.replaceAnnotations(Collections.singletonList(annos));
		
		// Read
		Annotations clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
		checkAnnotationsMetadata(clone, submissionId);
		assertTrue(clone.getLongAnnos().containsAll(annos.getLongAnnos()));
		assertFalse(annos.getStringAnnos().containsAll(clone.getStringAnnos()));
		
		// Update
		longAnnos = new ArrayList<LongAnnotation>();
		LongAnnotation la3 = new LongAnnotation();
		la3.setIsPrivate(false);
		la3.setKey("keyThree");
		la3.setValue(3L);
		la1.setValue(la1.getValue() + 1);
		longAnnos.add(la1);
		longAnnos.add(la3);
		annos.setLongAnnos(longAnnos);
		subStatusAnnoDAO.replaceAnnotations(Collections.singletonList(annos));
		
		// Verify
		Annotations clone2 = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone2);
		checkAnnotationsMetadata(clone2, submissionId);
		assertTrue(clone2.getLongAnnos().containsAll(annos.getLongAnnos()));
		assertFalse(clone.getLongAnnos().containsAll(clone2.getLongAnnos()));

		// Delete
		submissionDAO.delete(submissionId);
		subStatusAnnoDAO.deleteAnnotationsByScope(Long.parseLong(evalId));
		clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
		assertEquals(0, clone.getStringAnnos().size());
	}
	
	@Test
	public void testDoubleAnnotations() throws DatastoreException, JSONObjectAdapterException, NotFoundException{
        // create a submission
        String submissionId = createSubmission("5678", SUBMISSION_NAME, nodeId, userId);
        submissionIds.add(submissionId);       
        // create Annotations
        Annotations annos = createAnnotations(evalId, submissionId);
		// Create
		List<DoubleAnnotation> doubleAnnos = new ArrayList<DoubleAnnotation>();
		DoubleAnnotation da1 = new DoubleAnnotation();
		da1.setIsPrivate(false);
		da1.setKey("keyOne");
		da1.setValue(1.1);
		DoubleAnnotation da2 = new DoubleAnnotation();
		da2.setIsPrivate(false);
		da2.setKey("keyTwo");
		da2.setValue(2.2);
		DoubleAnnotation da3 = new DoubleAnnotation();
		da3.setIsPrivate(false);
		da3.setKey("keyThree");
		da3.setValue(Double.NaN);
		
		DoubleAnnotation daNULL = new DoubleAnnotation();
		daNULL.setIsPrivate(false);
		daNULL.setKey("keyNULL");
		daNULL.setValue(null);
		
		doubleAnnos.add(da1);
		doubleAnnos.add(da2);
		doubleAnnos.add(da3);
		doubleAnnos.add(daNULL);
		annos.setDoubleAnnos(doubleAnnos);
		subStatusAnnoDAO.replaceAnnotations(Collections.singletonList(annos));
		
        Annotations finiteAnnos = createAnnotations(evalId, submissionId);
		List<DoubleAnnotation> finiteDoubleAnnos = new ArrayList<DoubleAnnotation>();
		doubleAnnos.add(da1);
		doubleAnnos.add(da2);
		finiteAnnos.setDoubleAnnos(finiteDoubleAnnos);
		
		// Read
		Annotations clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
		checkAnnotationsMetadata(clone, submissionId);
		assertTrue(clone.getDoubleAnnos().containsAll(finiteAnnos.getDoubleAnnos()));
		assertFalse(annos.getStringAnnos().containsAll(clone.getStringAnnos()));
		
		// Update
		doubleAnnos = new ArrayList<DoubleAnnotation>();
		DoubleAnnotation da4 = new DoubleAnnotation();
		da4.setIsPrivate(true);
		da4.setKey("keyThree");
		da4.setValue(3.3);
		da1.setValue(da1.getValue() + 3.14);
		doubleAnnos.add(da1);
		doubleAnnos.add(da4);
		annos.setDoubleAnnos(doubleAnnos);
		subStatusAnnoDAO.replaceAnnotations(Collections.singletonList(annos));
		
		// Verify
		Annotations clone2 = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone2);
		checkAnnotationsMetadata(clone2, submissionId);
		assertTrue(clone2.getDoubleAnnos().containsAll(annos.getDoubleAnnos()));
		assertFalse(clone.getDoubleAnnos().containsAll(clone2.getDoubleAnnos()));

		// Delete
		submissionDAO.delete(submissionId);
		subStatusAnnoDAO.deleteAnnotationsByScope(Long.parseLong(evalId));
		clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
		assertEquals(0, clone.getStringAnnos().size());
	}
	
	// in this test we also test updating annotations for two submissions at once
	@Test
	public void testMixedAnnotations() throws DatastoreException, JSONObjectAdapterException, NotFoundException{
        // create a submission
        String submissionId = createSubmission("5678", SUBMISSION_NAME, nodeId, userId);
        submissionIds.add(submissionId);       
        // create Annotations
        Annotations annos = createAnnotations(evalId, submissionId);
		
		List<Annotations> annotationsList = new ArrayList<Annotations>();
		annotationsList.add(annos);
		{
			// Create
			List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
			StringAnnotation sa1 = new StringAnnotation();
			sa1.setIsPrivate(false);
			sa1.setKey("keyOne");
			sa1.setValue("valueOne");
			stringAnnos.add(sa1);
			annos.setStringAnnos(stringAnnos);
			
			List<LongAnnotation> longAnnos = new ArrayList<LongAnnotation>();
			LongAnnotation la1 = new LongAnnotation();
			la1.setIsPrivate(false);
			la1.setKey("keyTwo");
			la1.setValue(1L);
			longAnnos.add(la1);
			annos.setLongAnnos(longAnnos);
			
			List<DoubleAnnotation> doubleAnnos = new ArrayList<DoubleAnnotation>();
			DoubleAnnotation da1 = new DoubleAnnotation();
			da1.setIsPrivate(true);
			da1.setKey("keyThree");
			da1.setValue(1.1);
			doubleAnnos.add(da1);
			annos.setDoubleAnnos(doubleAnnos);		
		}
		
	       // create a submission
        String submissionId2 = createSubmission("8765", SUBMISSION_NAME, nodeId, userId);
        submissionIds.add(submissionId2);       
        // create Annotations
        Annotations annos2 = createAnnotations(evalId, submissionId2);
		
        // in order to test PLFM-2775, make sure the Ids are in *descending* order
        if (Long.parseLong(submissionId2)<Long.parseLong(submissionId)) {
        	annotationsList.add(annos2);
		} else {
        	annotationsList.add(0, annos2);
		}
        
		{
			// Create
			List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
			StringAnnotation sa1 = new StringAnnotation();
			sa1.setIsPrivate(false);
			sa1.setKey("keyOne2");
			sa1.setValue("valueOne2");
			stringAnnos.add(sa1);
			annos2.setStringAnnos(stringAnnos);
			
			List<LongAnnotation> longAnnos = new ArrayList<LongAnnotation>();
			LongAnnotation la1 = new LongAnnotation();
			la1.setIsPrivate(false);
			la1.setKey("keyTwo2");
			la1.setValue(2L);
			longAnnos.add(la1);
			annos2.setLongAnnos(longAnnos);
			
			List<DoubleAnnotation> doubleAnnos = new ArrayList<DoubleAnnotation>();
			DoubleAnnotation da1 = new DoubleAnnotation();
			da1.setIsPrivate(true);
			da1.setKey("keyThree2");
			da1.setValue(2.2);
			doubleAnnos.add(da1);
			annos2.setDoubleAnnos(doubleAnnos);		
		}
		
		
		subStatusAnnoDAO.replaceAnnotations(annotationsList);
		
		// Read
		Annotations clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
		checkAnnotationsMetadata(clone, submissionId);
		assertTrue(clone.getLongAnnos().containsAll(annos.getLongAnnos()));
		assertTrue(clone.getDoubleAnnos().containsAll(annos.getDoubleAnnos()));
		assertFalse(annos.getStringAnnos().containsAll(clone.getStringAnnos()));

		Annotations clone2 = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId2));
		assertNotNull(clone2);
		checkAnnotationsMetadata(clone2, submissionId2);
		assertTrue(clone2.getLongAnnos().containsAll(annos2.getLongAnnos()));
		assertTrue(clone2.getDoubleAnnos().containsAll(annos2.getDoubleAnnos()));
		assertFalse(annos2.getStringAnnos().containsAll(clone2.getStringAnnos()));

		// Read from blob
		Annotations blobClone = subStatusAnnoDAO.getAnnotationsFromBlob(Long.parseLong(submissionId));
		assertNotNull(blobClone);
		checkAnnotationsMetadata(blobClone, submissionId);
		assertEquals(annos, blobClone);
		
		Annotations blobClone2 = subStatusAnnoDAO.getAnnotationsFromBlob(Long.parseLong(submissionId2));
		assertNotNull(blobClone2);
		checkAnnotationsMetadata(blobClone2, submissionId2);
		assertEquals(annos2, blobClone2);
		
		// Update
		{
			List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
			StringAnnotation sa2 = new StringAnnotation();
			sa2.setIsPrivate(false);
			sa2.setKey("keyOne");
			sa2.setValue("valueTwo");
			stringAnnos.add(sa2);
			StringAnnotation sa3 = new StringAnnotation();
			sa3.setIsPrivate(true);
			sa3.setKey("keyFour");
			sa3.setValue(null);
			annos.setStringAnnos(stringAnnos);
			
			List<LongAnnotation> longAnnos = new ArrayList<LongAnnotation>();
			LongAnnotation la2 = new LongAnnotation();
			la2.setIsPrivate(false);
			la2.setKey("keyTwo");
			la2.setValue(2L);
			longAnnos.add(la2);
			annos.setLongAnnos(longAnnos);
			
			List<DoubleAnnotation> doubleAnnos = new ArrayList<DoubleAnnotation>();
			DoubleAnnotation da2 = new DoubleAnnotation();
			da2.setIsPrivate(false);
			da2.setKey("keyThree");
			da2.setValue(3.3);
			doubleAnnos.add(da2);
			annos.setDoubleAnnos(doubleAnnos);
		}
		
		subStatusAnnoDAO.replaceAnnotations(Collections.singletonList(annos));
		
		// Verify
		Annotations clone3 = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone3);
		checkAnnotationsMetadata(clone3, submissionId);
		assertTrue(clone3.getLongAnnos().containsAll(annos.getLongAnnos()));
		assertTrue(clone3.getDoubleAnnos().containsAll(annos.getDoubleAnnos()));	
		assertFalse(clone.getStringAnnos().containsAll(clone3.getStringAnnos()));
		
		// Read from blob
		Annotations blobClone3 = subStatusAnnoDAO.getAnnotationsFromBlob(Long.parseLong(submissionId));
		assertNotNull(blobClone3);
		checkAnnotationsMetadata(blobClone3, submissionId);
		assertEquals(annos, blobClone3);
		
		// Delete
		submissionDAO.delete(submissionId);
		subStatusAnnoDAO.deleteAnnotationsByScope(Long.parseLong(evalId));
		clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
		assertEquals(0, clone.getStringAnnos().size());
		
		submissionDAO.delete(submissionId2);
		subStatusAnnoDAO.deleteAnnotationsByScope(Long.parseLong(evalId));
		clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId2));
		assertNotNull(clone);
		assertEquals(0, clone.getStringAnnos().size());
	}
	
	@Test
	public void testChangedSubmissions() throws Exception {
		// there's no diff between the status (truth) table and the annotations table
		List<SubmissionBundle> changed = subStatusAnnoDAO.getChangedSubmissions(Long.parseLong(evalId));
		assertTrue(changed.isEmpty());
		
        // create a submission
        String submissionId = createSubmission("5678", SUBMISSION_NAME, nodeId, userId);
        submissionIds.add(submissionId);       
		SubmissionStatus status = new SubmissionStatus();
		status.setId(submissionId);
		status.setStatus(SubmissionStatusEnum.RECEIVED);
		status.setVersionNumber(7L);
		status.setStatusVersion(0L);
		status.setModifiedOn(new Date());
        Annotations annos = createAnnotations(evalId, submissionId);
		
		status.setAnnotations(annos);
		submissionStatusId = submissionStatusDAO.create(status);
		assertEquals(submissionId, submissionStatusId);
		
		// now there is a status that's not reflected in the annotations
		changed = subStatusAnnoDAO.getChangedSubmissions(Long.parseLong(evalId));
		assertEquals(1, changed.size());
		assertEquals(submissionId, changed.get(0).getSubmission().getId());
		SubmissionStatus retrievedStatus = changed.get(0).getSubmissionStatus();
		assertEquals(submissionStatusId, retrievedStatus.getId());
		assertEquals(status.getVersionNumber(), retrievedStatus.getVersionNumber());
		assertEquals(status.getStatusVersion(), retrievedStatus.getStatusVersion());
		
		status = submissionStatusDAO.get(submissionStatusId);
		annos = status.getAnnotations();
		// we're on the initial version of the annotations
		assertEquals(new Long(0L), annos.getVersion());
		subStatusAnnoDAO.replaceAnnotations(Collections.singletonList(annos));
		
		// now the queryable annotations match the 'truth'
		changed = subStatusAnnoDAO.getChangedSubmissions(Long.parseLong(evalId));
		assertTrue(changed.isEmpty());
		
		// this should increment the version.  once again the submission is in the 'diff list'
		submissionStatusDAO.update(Collections.singletonList(status));
		changed = subStatusAnnoDAO.getChangedSubmissions(Long.parseLong(evalId));
		assertEquals(1, changed.size());
		Submission changedSubmission = changed.get(0).getSubmission();
		assertEquals(submissionId, changedSubmission.getId());
		assertEquals(submissionDAO.get(submissionId), changedSubmission);
		SubmissionStatus changedStatus = changed.get(0).getSubmissionStatus();
		assertEquals(submissionStatusId, changedStatus.getId());
		assertEquals(submissionStatusDAO.get(submissionId), changedStatus);
	}
	
	@Test
	public void testDeleteAnnotationsByScope() throws Exception {
        // create a submission
        String submissionId = createSubmission("5678", SUBMISSION_NAME, nodeId, userId);
        submissionIds.add(submissionId); 
        
        // create annotations table entry
        Annotations annos = createAnnotations(evalId, submissionId);
		subStatusAnnoDAO.replaceAnnotations(Collections.singletonList(annos));
		
		subStatusAnnoDAO.deleteAnnotationsByScope(Long.parseLong(evalId));
		
		// the annotations have not been deleted
		Annotations retrieved = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertEquals(submissionId, retrieved.getObjectId());
		assertEquals(evalId, retrieved.getScopeId());
		
		// now delete the submission
		submissionDAO.delete(submissionId);
		subStatusAnnoDAO.deleteAnnotationsByScope(Long.parseLong(evalId));
		
		// the annotations have been deleted
		retrieved = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNull(retrieved.getObjectId());
		assertNull(retrieved.getScopeId());
		
	}
	
}
