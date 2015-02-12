package org.sagebionetworks.evaluation.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.dbo.SubmissionDBO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionStatusDAO;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SubmissionDAOImplTest {
 
    @Autowired
    private SubmissionDAO submissionDAO;
    
    @Autowired
    private SubmissionStatusDAO submissionStatusDAO;
    
    @Autowired
    private EvaluationDAO evaluationDAO;
	
    @Autowired
	private NodeDAO nodeDAO;
	
    @Autowired
	private FileHandleDao fileHandleDAO;
 
	@Autowired
	UserGroupDAO userGroupDAO;
	
	@Autowired
	GroupMembersDAO groupMembersDAO;
	
	@Autowired
	TeamDAO teamDAO;
	
	private String nodeId;
	private String userId;
	private String userId2;
	
    private String submissionId = "206";
    private String userId_does_not_exist = "2";
    private String evalId;
    private String evalId_does_not_exist = "456";
    private String name = "test submission";
    private String fileHandleId;
    private Long versionNumber = 1L;
    private Submission submission;
    private Submission submission2;
    
    // create a team and add the given ID as a member
	private Team createTeam(String ownerId) throws NotFoundException {
		Team team = new Team();
		UserGroup ug = new UserGroup();
		ug.setIsIndividual(false);
		Long id = userGroupDAO.create(ug);
		
		team.setId(id.toString());
		team.setCreatedOn(new Date());
		team.setCreatedBy(ownerId);
		team.setModifiedOn(new Date());
		team.setModifiedBy(ownerId);
		Team created = teamDAO.create(team);
		try {
			groupMembersDAO.addMembers(id.toString(), Arrays.asList(new String[]{ownerId}));
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
			
		return created;
	}
	
	private void deleteTeam(Team team) throws NotFoundException {
		teamDAO.delete(team.getId());
		userGroupDAO.delete(team.getId());
	}
	
	private Submission createSubmission(Date createdDate) {
        // Initialize a Submission
        submission = new Submission();
        submission.setCreatedOn(createdDate);
        submission.setId(submissionId);
        submission.setName(name);
        submission.setEvaluationId(evalId);
        submission.setEntityId(nodeId);
        submission.setVersionNumber(versionNumber);
        submission.setUserId(userId);
        submission.setSubmitterAlias("Team Awesome");
        submission.setEntityBundleJSON("some bundle");
        return submission;
	}

    @Before
    public void setUp() throws DatastoreException, InvalidModelException, NotFoundException {
    	userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
    	userId2 = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString();
    	
    	// create a file handle
		PreviewFileHandle meta = new PreviewFileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy("" + userId);
		meta.setFileName("preview.jpg");
		fileHandleId = fileHandleDAO.createFile(meta).getId();
		
    	// create a node
  		Node toCreate = NodeTestUtils.createNew(name, Long.parseLong(userId));
    	toCreate.setVersionComment("This is the first version of the first node ever!");
    	toCreate.setVersionLabel("0.0.1");
    	toCreate.setFileHandleId(fileHandleId);
    	nodeId = nodeDAO.createNew(toCreate);
    	
    	// create an Evaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setId("1234");
        evaluation.setEtag("etag");
        evaluation.setName("name");
        evaluation.setOwnerId(userId);
        evaluation.setCreatedOn(new Date());
        evaluation.setContentSource(nodeId);
        evaluation.setStatus(EvaluationStatus.PLANNED);
        evalId = evaluationDAO.create(evaluation, Long.parseLong(userId));
        
        // Initialize a Submission
        submission = new Submission();
        submission.setCreatedOn(new Date());
        submission.setId(submissionId);
        submission.setName(name);
        submission.setEvaluationId(evalId);
        submission.setEntityId(nodeId);
        submission.setVersionNumber(versionNumber);
        submission.setUserId(userId);
        submission.setSubmitterAlias("Team Awesome");
        submission.setEntityBundleJSON("some bundle");
    }
    
    @After
    public void tearDown() throws DatastoreException {
		try {
			submissionDAO.delete(submissionId);
		} catch (NotFoundException e)  {};
		try {
			evaluationDAO.delete(evalId);
		} catch (NotFoundException e) {};
    	try {
    		nodeDAO.delete(nodeId);
    	} catch (NotFoundException e) {};
		fileHandleDAO.delete(fileHandleId);
    }
    
    @Test
    public void testCRD() throws Exception{
        long initialCount = submissionDAO.getCount();
 
        // create Submission
        submissionId = submissionDAO.create(submission);
        assertNotNull(submissionId);   
        
        // fetch it
        Submission clone = submissionDAO.get(submissionId);
        assertNotNull(clone);
        submission.setId(submissionId);
        submission.setCreatedOn(clone.getCreatedOn());
        assertEquals(initialCount + 1, submissionDAO.getCount());
        assertEquals(submission, clone);
        
        // delete it
        submissionDAO.delete(submissionId);
        try {
        	clone = submissionDAO.get(submissionId);
        } catch (NotFoundException e) {
        	// expected
        	assertEquals(initialCount, submissionDAO.getCount());
        	return;
        }
        fail("Failed to delete Participant");
    }
    
    @Test
    public void testGetAllByUser() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
    	submissionId = submissionDAO.create(submission);
    	
    	// userId should have submissions
    	List<Submission> subs = submissionDAO.getAllByUser(userId, 10, 0);
    	assertEquals(1, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByUser(userId));
    	submission.setCreatedOn(subs.get(0).getCreatedOn());
    	submission.setId(subs.get(0).getId());
    	assertEquals(subs.get(0), submission);
    	
    	// userId_does_not_exist should not have any submissions
    	subs = submissionDAO.getAllByUser(userId_does_not_exist, 10, 0);
    	assertEquals(0, subs.size());
    }
    
    @Test
    public void testGetAllByEvaluation() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
    	submissionId = submissionDAO.create(submission);
    	
    	// evalId should have submissions
    	List<Submission> subs = submissionDAO.getAllByEvaluation(evalId, 10, 0);
    	assertEquals(1, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByEvaluation(evalId));
    	submission.setCreatedOn(subs.get(0).getCreatedOn());
    	submission.setId(subs.get(0).getId());
    	assertEquals(subs.get(0), submission);
    	
    	// evalId_does_not_exist should not have any submissions
    	subs = submissionDAO.getAllByEvaluation(evalId_does_not_exist, 10, 0);
    	assertEquals(0, subs.size());
    	assertEquals(0, submissionDAO.getCountByEvaluation(evalId_does_not_exist));
    }
    
    @Test
    public void testGetAllByEvaluationAndStatus() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
    	submissionId = submissionDAO.create(submission);
    	
    	// create a SubmissionStatus object
    	SubmissionStatus subStatus = new SubmissionStatus();
    	subStatus.setId(submissionId);
    	subStatus.setStatus(SubmissionStatusEnum.RECEIVED);
    	subStatus.setModifiedOn(new Date());
    	submissionStatusDAO.create(subStatus);
    	
    	// hit evalId and hit status => should find 1 submission
    	List<Submission> subs = submissionDAO.getAllByEvaluationAndStatus(evalId, SubmissionStatusEnum.RECEIVED, 10, 0);
    	assertEquals(1, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByEvaluationAndStatus(evalId, SubmissionStatusEnum.RECEIVED));
    	submission.setCreatedOn(subs.get(0).getCreatedOn());
    	submission.setId(subs.get(0).getId());
    	assertEquals(subs.get(0), submission);
    	
    	// miss evalId and hit status => should find 0 submissions
    	subs = submissionDAO.getAllByEvaluationAndStatus(evalId_does_not_exist, SubmissionStatusEnum.RECEIVED, 10, 0);
    	assertEquals(0, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByEvaluationAndStatus(evalId_does_not_exist, SubmissionStatusEnum.RECEIVED));
    	
    	// hit evalId and miss status => should find 0 submissions
    	subs = submissionDAO.getAllByEvaluationAndStatus(evalId, SubmissionStatusEnum.SCORED, 10, 0);
    	assertEquals(0, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByEvaluationAndStatus(evalId, SubmissionStatusEnum.SCORED));
    }
    
    @Test
    public void testGetAllByEvaluationAndUser() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
    	submissionId = submissionDAO.create(submission);
    	
    	// hit evalId and hit user => should find 1 submission
    	List<Submission> subs = submissionDAO.getAllByEvaluationAndUser(evalId, userId, 10, 0);
    	assertEquals(1, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByEvaluationAndUser(evalId, userId));
    	submission.setCreatedOn(subs.get(0).getCreatedOn());
    	submission.setId(subs.get(0).getId());
    	assertEquals(subs.get(0), submission);
    	
    	// miss evalId and hit user => should find 0 submissions
    	subs = submissionDAO.getAllByEvaluationAndUser(evalId_does_not_exist, userId, 10, 0);
    	assertEquals(0, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByEvaluationAndUser(evalId_does_not_exist, userId));
    	
    	// hit evalId and miss user => should find 0 submissions
    	subs = submissionDAO.getAllByEvaluationAndUser(evalId, userId_does_not_exist, 10, 0);
    	assertEquals(0, subs.size());
    	assertEquals(subs.size(), submissionDAO.getCountByEvaluationAndUser(evalId, userId_does_not_exist));
    }
    
    @Test
    public void testDtoToDbo() {
    	Submission subDTO = new Submission();
    	Submission subDTOclone = new Submission();
    	SubmissionDBO subDBO = new SubmissionDBO();
    	SubmissionDBO subDBOclone = new SubmissionDBO();
    	
    	subDTO.setEvaluationId("123");
    	subDTO.setCreatedOn(new Date());
    	subDTO.setEntityId("syn456");
    	subDTO.setId("789");
    	subDTO.setName("name");
    	subDTO.setUserId("42");
    	subDTO.setSubmitterAlias("Team Awesome");
    	subDTO.setVersionNumber(1L);
    	subDTO.setEntityBundleJSON("foo");
    	    	
    	SubmissionUtils.copyDtoToDbo(subDTO, subDBO);
    	SubmissionUtils.copyDboToDto(subDBO, subDTOclone);
    	SubmissionUtils.copyDtoToDbo(subDTOclone, subDBOclone);
    	
    	assertEquals(subDTO, subDTOclone);
    	assertEquals(subDBO, subDBOclone);
    }
    
    @Test
    public void testDtoToDboNullColumn() {
    	Submission subDTO = new Submission();
    	Submission subDTOclone = new Submission();
    	SubmissionDBO subDBO = new SubmissionDBO();
    	SubmissionDBO subDBOclone = new SubmissionDBO();
    	
    	subDTO.setEvaluationId("123");
    	subDTO.setCreatedOn(new Date());
    	subDTO.setEntityId("syn456");
    	subDTO.setId("789");
    	subDTO.setName("name");
    	subDTO.setUserId("42");
    	subDTO.setSubmitterAlias("Team Awesome");
    	subDTO.setVersionNumber(1L);
    	// null EntityBundle
    	    	
    	SubmissionUtils.copyDtoToDbo(subDTO, subDBO);
    	SubmissionUtils.copyDboToDto(subDBO, subDTOclone);
    	SubmissionUtils.copyDtoToDbo(subDTOclone, subDBOclone);
    	
    	assertEquals(subDTO, subDTOclone);
    	assertEquals(subDBO, subDBOclone);
    	assertNull(subDTOclone.getEntityBundleJSON());
    	assertNull(subDBOclone.getEntityBundle());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testMissingVersionNumber() throws DatastoreException, JSONObjectAdapterException {
        submission.setVersionNumber(null);
        submissionDAO.create(submission);
    }
    
    // Should be able to have null entity bundle
    @Test
    public void testPLFM1859() {
    	submission.setEntityBundleJSON(null);
    	String id = submissionDAO.create(submission);
    	assertNotNull(id);
    }
}
