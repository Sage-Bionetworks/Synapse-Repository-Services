package org.sagebionetworks.evaluation.dao;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
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
	SubmissionStatusDAO submissionStatusDAO;
    @Autowired
    SubmissionDAO submissionDAO;
    @Autowired
    ParticipantDAO participantDAO;
    @Autowired
    EvaluationDAO evaluationDAO;
	@Autowired
	NodeDAO nodeDAO;
	
	private String nodeId = null;
    private String submissionId = null;
    private String userId = "0";
    private String evalId;
    private String name = "test submission";
    private Long versionNumber = 1L;
    private Annotations annos;
	
	@After
	public void after() throws DatastoreException {
		subStatusAnnoDAO.deleteAnnotationsByOwnerId(Long.parseLong(submissionId));
    	try {
    		nodeDAO.delete(nodeId);
    	} catch (NotFoundException e) {};
		try {
			submissionDAO.delete(submissionId);
		} catch (NotFoundException e)  {};
		try {
			participantDAO.delete(userId, evalId);
		} catch (NotFoundException e) {};
		try {
			evaluationDAO.delete(evalId);
		} catch (NotFoundException e) {};
	}
	
	@Before
	public void before() throws DatastoreException, InvalidModelException, NotFoundException {
    	// create a node
  		Node toCreate = NodeTestUtils.createNew(name, Long.parseLong(userId));
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
        evaluation.setStatus(EvaluationStatus.PLANNED);
        evalId = evaluationDAO.create(evaluation, Long.parseLong(userId));
        
        // create a participant
        Participant participant = new Participant();
        participant.setCreatedOn(new Date());
        participant.setUserId(userId);
        participant.setEvaluationId(evalId);
        participantDAO.create(participant);
        
        // create a submission
        Submission submission = new Submission();
        submission.setId("5678");
        submission.setName(name);
        submission.setEntityId(nodeId);
        submission.setVersionNumber(versionNumber);
        submission.setUserId(userId);
        submission.setEvaluationId(evalId);
        submission.setCreatedOn(new Date());
        submission.setEntityBundleJSON("some bundle");
        submissionId = submissionDAO.create(submission);
        
        // create Annotations
		annos = new Annotations();
		annos.setObjectId(submissionId);
		annos.setScopeId(evalId);
		annos.setDoubleAnnos(new ArrayList<DoubleAnnotation>());
		annos.setLongAnnos(new ArrayList<LongAnnotation>());
		annos.setStringAnnos(new ArrayList<StringAnnotation>());
	}
	
	@Test
	public void testStringAnnotations() throws DatastoreException, JSONObjectAdapterException{		 
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
		subStatusAnnoDAO.replaceAnnotations(annos);
		
		// Read
		Annotations clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
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
		subStatusAnnoDAO.replaceAnnotations(annos);
		
		// Verify
		Annotations clone2 = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone2);
		assertTrue(clone2.getStringAnnos().containsAll(annos.getStringAnnos()));
		assertFalse(clone.getStringAnnos().containsAll(clone2.getStringAnnos()));

		// Delete
		subStatusAnnoDAO.deleteAnnotationsByOwnerId(Long.parseLong(submissionId));
		clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
		assertEquals(0, clone.getStringAnnos().size());
	}
	
	@Test
	public void testLongAnnotations() throws DatastoreException, JSONObjectAdapterException{
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
		subStatusAnnoDAO.replaceAnnotations(annos);
		
		// Read
		Annotations clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
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
		subStatusAnnoDAO.replaceAnnotations(annos);
		
		// Verify
		Annotations clone2 = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone2);
		assertTrue(clone2.getLongAnnos().containsAll(annos.getLongAnnos()));
		assertFalse(clone.getLongAnnos().containsAll(clone2.getLongAnnos()));

		// Delete
		subStatusAnnoDAO.deleteAnnotationsByOwnerId(Long.parseLong(submissionId));
		clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
		assertEquals(0, clone.getStringAnnos().size());
	}
	
	@Test
	public void testDoubleAnnotations() throws DatastoreException, JSONObjectAdapterException{
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
		doubleAnnos.add(da1);
		doubleAnnos.add(da2);
		annos.setDoubleAnnos(doubleAnnos);
		subStatusAnnoDAO.replaceAnnotations(annos);
		
		// Read
		Annotations clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
		assertTrue(clone.getDoubleAnnos().containsAll(annos.getDoubleAnnos()));
		assertFalse(annos.getStringAnnos().containsAll(clone.getStringAnnos()));
		
		// Update
		doubleAnnos = new ArrayList<DoubleAnnotation>();
		DoubleAnnotation da3 = new DoubleAnnotation();
		da3.setIsPrivate(true);
		da3.setKey("keyThree");
		da3.setValue(3.3);
		da1.setValue(da1.getValue() + 3.14);
		doubleAnnos.add(da1);
		doubleAnnos.add(da3);
		annos.setDoubleAnnos(doubleAnnos);
		subStatusAnnoDAO.replaceAnnotations(annos);
		
		// Verify
		Annotations clone2 = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone2);
		assertTrue(clone2.getDoubleAnnos().containsAll(annos.getDoubleAnnos()));
		assertFalse(clone.getDoubleAnnos().containsAll(clone2.getDoubleAnnos()));

		// Delete
		subStatusAnnoDAO.deleteAnnotationsByOwnerId(Long.parseLong(submissionId));
		clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
		assertEquals(0, clone.getStringAnnos().size());
	}
	
	@Test
	public void testMixedAnnotations() throws DatastoreException, JSONObjectAdapterException{
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
		
		subStatusAnnoDAO.replaceAnnotations(annos);
		
		// Read
		Annotations clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
		assertTrue(clone.getLongAnnos().containsAll(annos.getLongAnnos()));
		assertTrue(clone.getDoubleAnnos().containsAll(annos.getDoubleAnnos()));
		assertFalse(annos.getStringAnnos().containsAll(clone.getStringAnnos()));
		
		// Read from blob
		Annotations blobClone = subStatusAnnoDAO.getAnnotationsFromBlob(Long.parseLong(submissionId));
		assertNotNull(blobClone);
		assertEquals(annos, blobClone);
		
		// Update
		stringAnnos = new ArrayList<StringAnnotation>();
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
		
		longAnnos = new ArrayList<LongAnnotation>();
		LongAnnotation la2 = new LongAnnotation();
		la2.setIsPrivate(false);
		la2.setKey("keyTwo");
		la2.setValue(2L);
		longAnnos.add(la2);
		annos.setLongAnnos(longAnnos);
		
		doubleAnnos = new ArrayList<DoubleAnnotation>();
		DoubleAnnotation da2 = new DoubleAnnotation();
		da2.setIsPrivate(false);
		da2.setKey("keyThree");
		da2.setValue(3.3);
		doubleAnnos.add(da2);
		annos.setDoubleAnnos(doubleAnnos);
		
		subStatusAnnoDAO.replaceAnnotations(annos);
		
		// Verify
		Annotations clone2 = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone2);
		assertTrue(clone2.getLongAnnos().containsAll(annos.getLongAnnos()));
		assertTrue(clone2.getDoubleAnnos().containsAll(annos.getDoubleAnnos()));	
		assertFalse(clone.getStringAnnos().containsAll(clone2.getStringAnnos()));
		
		// Read from blob
		Annotations blobClone2 = subStatusAnnoDAO.getAnnotationsFromBlob(Long.parseLong(submissionId));
		assertNotNull(blobClone2);
		assertEquals(annos, blobClone2);
		
		// Delete
		subStatusAnnoDAO.deleteAnnotationsByOwnerId(Long.parseLong(submissionId));
		clone = subStatusAnnoDAO.getAnnotations(Long.parseLong(submissionId));
		assertNotNull(clone);
		assertEquals(0, clone.getStringAnnos().size());
	}
}
