package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionFileHandleDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EvaluationSubmissionFileScannerIntegrationTest {

	@Autowired
	private NodeDAO nodeDao;
	
	@Autowired
	private FileHandleDao fileHandleDao;
			
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private DaoObjectHelper<Node> nodeDaoHelper;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private EvaluationDAO evaluationDao;
	
	@Autowired
	private SubmissionDAO submissionDao;
	
	@Autowired
	private SubmissionFileHandleDAO submissionFileDao;
	
	@Autowired
	private FileHandleAssociationScannerTestUtils utils;
	
	@Autowired
	private FileHandleAssociationManager manager;
	
	private FileHandleAssociateType associationType = FileHandleAssociateType.SubmissionAttachment;
	
	private UserInfo user;
	
	private String projectId;
	
	private String evaluationId;
	
	@BeforeEach
	public void before() {
		submissionDao.truncateAll();
		nodeDao.truncateAll();
		fileHandleDao.truncateTable();
		
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		projectId = nodeDaoHelper.create(n -> {
			n.setName("Project");
			n.setCreatedByPrincipalId(user.getId());
		}).getId();
		
		Evaluation evaluation = new Evaluation();
		
		evaluation.setId(idGenerator.generateNewId(IdType.EVALUATION_ID).toString());
		evaluation.setContentSource(projectId);
		evaluation.setName("TestEvaluation");
		evaluation.setCreatedOn(new Date());
		
		evaluationId = evaluationDao.create(evaluation, user.getId());
	}
	
	@AfterEach
	public void after() {
		submissionDao.truncateAll();
		nodeDao.truncateAll();
		evaluationDao.delete(evaluationId);
		fileHandleDao.truncateTable();
	}
	
	@Test
	public void testScanner() {
		
		Node n1 = createFileNode(projectId);
		Node n2 = createFileNode(projectId);
		Node n3 = createFileNode(projectId);
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
				new ScannedFileHandleAssociation(createSubmission(n1), Long.valueOf(n1.getFileHandleId())),
				new ScannedFileHandleAssociation(createSubmission(n2), Long.valueOf(n2.getFileHandleId())),
				new ScannedFileHandleAssociation(createSubmission(n3), Long.valueOf(n3.getFileHandleId()))
		);
		
		IdRange range = manager.getIdRange(associationType);
		
		// Call under test
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(manager.scanRange(associationType, range).spliterator(), false).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	private Node createFileNode(String parentId) {
		return nodeDaoHelper.create(n -> {
			n.setName("File-" + UUID.randomUUID().toString());
			n.setCreatedByPrincipalId(user.getId());
			n.setParentId(parentId);
			n.setNodeType(EntityType.file);
			n.setFileHandleId(utils.generateFileHandle(user));
		});
	}
	
	private String createSubmission(Node node) {
		Submission submission = new Submission();
		
		submission.setId(idGenerator.generateNewId(IdType.EVALUATION_SUBMISSION_ID).toString());
		submission.setEvaluationId(evaluationId);
		submission.setUserId(user.getId().toString());
		submission.setEntityId(node.getId());
		submission.setVersionNumber(node.getVersionNumber());
		submission.setCreatedOn(new Date());
		
		String submissionId = submissionDao.create(submission);
		
		if (node.getFileHandleId() != null) {
			submissionFileDao.create(submissionId, node.getFileHandleId());
		}
		
		return submissionId;
		
	}
}
