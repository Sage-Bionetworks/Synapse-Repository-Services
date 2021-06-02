package org.sagebionetworks.evaluation.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.evaluation.dbo.SubmissionStatusDBO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SubmissionStatusDAOImplTest {
 
	@Autowired
	SubmissionStatusDAO submissionStatusDAO;
    @Autowired
    SubmissionDAO submissionDAO;
    @Autowired
    EvaluationDAO evaluationDAO;
	@Autowired
	NodeDAO nodeDAO;
 
	private String nodeId;
	private String userId;
	
    private List<String> submissionIds;
    private String rogueSubmissionId;
    private List<String> evalIds;
    private static final String NODE_NAME = "test-submission";
    private static final Long VERSION_NUMBER = 1L;
    private static final int NUMBER_OF_EVALUATIONS = 2;
    private static final int NUMBER_OF_SUBMISSIONS = 2;
    
    @BeforeEach
    public void setUp() throws DatastoreException, InvalidModelException, NotFoundException, JSONObjectAdapterException {
    	evalIds = new ArrayList<String>();
    	submissionIds = new ArrayList<String>();
    	userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
    	
    	// create a node
  		Node toCreate = NodeTestUtils.createNew(NODE_NAME, Long.parseLong(userId));
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("0.0.1");
    	nodeId = nodeDAO.createNew(toCreate).substring(3); // trim "syn" from node ID
    	
    	// create evaluations
    	for (int i=0; i<NUMBER_OF_EVALUATIONS; i++) {
	        Evaluation evaluation = new Evaluation();
	        evaluation.setId("1234"+i);
	        evaluation.setEtag("etag"+i);
	        evaluation.setName("name"+i);
	        evaluation.setOwnerId(userId);
	        evaluation.setCreatedOn(new Date());
	        evaluation.setContentSource(nodeId);
	        evalIds.add(evaluationDAO.create(evaluation, Long.parseLong(userId)));
    	}
    	
        // create submissions
        for (int i=0; i<NUMBER_OF_SUBMISSIONS; i++) {
        	Submission submission = createSubmission(
        			"5678"+i,
        			NODE_NAME+"_"+i,
        			nodeId,
        			VERSION_NUMBER,
        			userId,
        			evalIds.get(0)
        			);
        	submissionIds.add(submissionDAO.create(submission));
        }
        
        // create submission under other eval:
    	Submission submission = createSubmission(
    			"98765",
    			"rogue",
    			nodeId,
    			VERSION_NUMBER,
    			userId,
    			evalIds.get(1)
    			);
    	rogueSubmissionId = submissionDAO.create(submission);
    }
    
    private static Submission createSubmission(
    		String id, 
    		String name, 
    		String nodeId, 
    		Long versionNumber, 
    		String userId,
    		String evalId) {
    	Submission submission = new Submission();
    	submission.setId(id);
    	submission.setName(name);
    	submission.setEntityId(nodeId);
    	submission.setVersionNumber(versionNumber);
    	submission.setUserId(userId);
    	submission.setEvaluationId(evalId);
    	submission.setCreatedOn(new Date());
    	submission.setEntityBundleJSON("some bundle");
    	return submission;
    }
    
    @AfterEach
    public void after() throws DatastoreException {
    	try {
    		nodeDAO.delete(nodeId);
    	} catch (NotFoundException e) {};
    	for (String submissionId : submissionIds) {
    		try {
    			submissionDAO.delete(submissionId);
    		} catch (NotFoundException e)  {};
    	}
		try {
			submissionDAO.delete(rogueSubmissionId);
		} catch (NotFoundException e)  {};
		for (String evalId : evalIds) {
			try {
				evaluationDAO.delete(evalId);
			} catch (NotFoundException e) {};
    	}
    }
    
    private SubmissionStatus createStatus(String id) {
        SubmissionStatus status = new SubmissionStatus();
		status.setModifiedOn(new Date());
		status.setId(id);
		status.setStatus(SubmissionStatusEnum.RECEIVED);
		status.setScore(0.1);
		status.setAnnotations(TestUtils.createDummyAnnotations());
        // Create it
        submissionStatusDAO.create(status);
    	return status;
    }
    
    private List<SubmissionStatus> createStatusesForSubmissions(long initialCount) throws DatastoreException, NotFoundException {
    	List<SubmissionStatus> clones = new ArrayList<SubmissionStatus>();
        for (int i=0; i<submissionIds.size(); i++) {
        	SubmissionStatus status = createStatus(submissionIds.get(i));
            assertEquals(initialCount + i + 1, submissionStatusDAO.getCount());
            
            // Fetch it
            SubmissionStatus clone = submissionStatusDAO.get(submissionIds.get(i));
            assertNotNull(clone);
            assertNotNull(clone.getModifiedOn());        
            assertNotNull(clone.getEtag());
            assertEquals(new Long(0L), clone.getStatusVersion());
            status.setModifiedOn(clone.getModifiedOn());
            status.setEtag(clone.getEtag());

            if (status.getSubmissionAnnotations() != null) {
            	Annotations anno = status.getSubmissionAnnotations();
            	anno.setId(clone.getId());
            	anno.setEtag(clone.getEtag());
            }
            
            assertEquals(status, clone);
            clones.add(clone);
    	}
    	return clones;
    }
    
    @Test
    public void testCRUD() throws Exception{
        // Initialize new SubmissionStatus objects for submissionId
        long initialCount = submissionStatusDAO.getCount();
    	List<SubmissionStatus> clones = createStatusesForSubmissions(initialCount);
        
        // Update it
        {
	    	assertTrue(clones.size()>1); 
	    	SubmissionStatus clone = clones.get(0);
	        clone.setStatus(SubmissionStatusEnum.SCORED);
	        clone.setScore(0.9);
	        clone.setCanCancel(false);
	        Thread.sleep(1L);
	        submissionStatusDAO.update(Collections.singletonList(clone));
	        SubmissionStatus clone2 = submissionStatusDAO.get(clone.getId());
	        compare(clone, clone2);
	        clones.set(0, clone2);
        }
        
        // cannot have repeats in a batch
        List<SubmissionStatus> repeats = new ArrayList<SubmissionStatus>();
        repeats.add(clones.get(0));
        repeats.add(clones.get(0));
        try {
        	submissionStatusDAO.update(repeats);
        	fail("InvalidModelException expected");
        } catch (InvalidModelException ime) {
        	// as expected
        }
        
        // now update all
        submissionStatusDAO.update(clones);
        for (int i=0; i<clones.size(); i++) {
        	SubmissionStatus orig = clones.get(i);
        	SubmissionStatus retrieved = submissionStatusDAO.get(clones.get(i).getId());
	        compare(orig, retrieved);
        }

    	// Delete them
    	for (int i=0; i<submissionIds.size(); i++) {
    		submissionStatusDAO.delete(submissionIds.get(i));
            // Fetch it (should not exist)
            try {
            	submissionStatusDAO.get(submissionIds.get(i));
            	fail("NotFoundException expected");
            } catch (NotFoundException e) {
            	// expected
            }
    	}
   		assertEquals(initialCount, submissionStatusDAO.getCount());    
    }
    
    @Test
    public void testGetEvaluationsForBatch() throws Exception {
        long initialCount = submissionStatusDAO.getCount();
    	List<SubmissionStatus> clones = createStatusesForSubmissions(initialCount);
    	assertEquals(
    			evalIds.get(0),
    			submissionStatusDAO.getEvaluationIdForBatch(clones).toString()
    			);
    }
    
    @Test
    public void testGetEvaluationsForBatchMultipleEvals() throws Exception {
        long initialCount = submissionStatusDAO.getCount();
    	List<SubmissionStatus> clones = createStatusesForSubmissions(initialCount);
    	clones.add(createStatus(rogueSubmissionId));
    	IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
    		submissionStatusDAO.getEvaluationIdForBatch(clones);
    	});
		assertEquals("Submission batch must be for a single Evaluation queue.", ex.getMessage());
    }
    
    @Test
    public void testGetEvaluationsForBatchEmptyList() throws Exception {
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			submissionStatusDAO.getEvaluationIdForBatch(new ArrayList<SubmissionStatus>());
		});
		assertEquals("SubmissionStatus batch has no Submission Ids.", ex.getMessage());
    }
    
    // note: this updates 'orig', so the caller shouldn't use 'orig' after passing it to this method
    private static void compare(SubmissionStatus orig, SubmissionStatus retrieved) {
        assertFalse(orig.getEtag().equals(retrieved.getEtag()), "eTag was not updated.");
        assertEquals(new Long(orig.getStatusVersion()+1L), retrieved.getStatusVersion(), "status-version was not updated.");
        assertFalse(orig.getModifiedOn().equals(retrieved.getModifiedOn()), "Modified date was not updated");
        if (retrieved.getSubmissionAnnotations() != null) {
        	assertEquals(retrieved.getId(), retrieved.getSubmissionAnnotations().getId());
        	assertEquals(retrieved.getEtag(), retrieved.getSubmissionAnnotations().getEtag());
        }
        orig.setModifiedOn(retrieved.getModifiedOn());
        orig.setEtag(retrieved.getEtag());
        orig.setStatusVersion(retrieved.getStatusVersion());
        orig.setSubmissionAnnotations(retrieved.getSubmissionAnnotations());
        assertEquals(orig, retrieved);
    }
    
    @Test
    public void testDtoToDbo() {
    	SubmissionStatus subStatusDTO = new SubmissionStatus();
    	SubmissionStatus subStatusDTOclone = new SubmissionStatus();
    	SubmissionStatusDBO subStatusDBO = new SubmissionStatusDBO();
    	SubmissionStatusDBO subStatusDBOclone = new SubmissionStatusDBO();
    	
    	subStatusDTO.setEtag("eTag");
    	subStatusDTO.setId("123");
    	subStatusDTO.setModifiedOn(new Date());
    	subStatusDTO.setScore(0.42);
    	subStatusDTO.setStatus(SubmissionStatusEnum.SCORED);
    	subStatusDTO.setReport("lorem ipsum");
    	subStatusDTO.setStatusVersion(5L);
    	subStatusDTO.setCanCancel(true);
    	subStatusDTO.setCancelRequested(false);
    	
    	Annotations annotations = AnnotationsV2Utils.emptyAnnotations();
    	AnnotationsV2TestUtils.putAnnotations(annotations, "myKey", "myValue", AnnotationsValueType.DOUBLE);
    	annotations.setId(subStatusDTO.getId());
    	annotations.setEtag(subStatusDTO.getEtag());
    	
    	subStatusDTO.setSubmissionAnnotations(annotations);
    	    	
    	subStatusDBO = SubmissionUtils.convertDtoToDbo(subStatusDTO);
    	subStatusDTOclone = SubmissionUtils.convertDboToDto(subStatusDBO);
    	subStatusDBOclone = SubmissionUtils.convertDtoToDbo(subStatusDTOclone);
    	
    	assertEquals(subStatusDTO, subStatusDTOclone);
    	assertEquals(subStatusDBO, subStatusDBOclone);
    }
    
    @Test
    public void testDtoToDboWithEmptySubmissionAnnotations() {
    	SubmissionStatus subStatusDTO = new SubmissionStatus();
    	SubmissionStatus subStatusDTOclone = new SubmissionStatus();
    	SubmissionStatusDBO subStatusDBO = new SubmissionStatusDBO();
    	SubmissionStatusDBO subStatusDBOclone = new SubmissionStatusDBO();
    	
    	subStatusDTO.setEtag("eTag");
    	subStatusDTO.setId("123");
    	subStatusDTO.setModifiedOn(new Date());
    	subStatusDTO.setScore(0.42);
    	subStatusDTO.setStatus(SubmissionStatusEnum.SCORED);
    	subStatusDTO.setReport("lorem ipsum");
    	subStatusDTO.setStatusVersion(5L);
    	subStatusDTO.setCanCancel(true);
    	subStatusDTO.setCancelRequested(false);
    	
    	subStatusDBO = SubmissionUtils.convertDtoToDbo(subStatusDTO);
    	subStatusDTOclone = SubmissionUtils.convertDboToDto(subStatusDBO);
    	subStatusDBOclone = SubmissionUtils.convertDtoToDbo(subStatusDTOclone);
    	    	
    	assertEquals(subStatusDTO, subStatusDTOclone);
    	assertEquals(subStatusDBO, subStatusDBOclone);
    }

}
