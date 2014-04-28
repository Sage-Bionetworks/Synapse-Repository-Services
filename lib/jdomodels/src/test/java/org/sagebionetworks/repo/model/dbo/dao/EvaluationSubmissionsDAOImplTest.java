package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.EvaluationSubmissions;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.evaluation.EvaluationSubmissionsDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class EvaluationSubmissionsDAOImplTest {
	
	@Autowired
	private NodeDAO nodeDAO;

	@Autowired
	private EvaluationDAO evaluationDAO;
	
	@Autowired
	private EvaluationSubmissionsDAO evaluationSubmissionsDAO;
	
	@Autowired
	private IdGenerator idGenerator;

	private String nodeIdToDelete;
	private String evaluationIdToDelete;

	@Before
	public void setUp() throws Exception {
		Long ownerId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		Node node = new Node();
		node.setName("testNode");
		node.setNodeType(EntityType.project.toString());
		node.setCreatedByPrincipalId(ownerId);
		node.setCreatedOn(new Date());
		node.setModifiedByPrincipalId(ownerId);
		node.setModifiedOn(new Date());
		nodeIdToDelete = nodeDAO.createNew(node);
		
		Evaluation evaluation = new Evaluation();
		evaluation.setId(idGenerator.generateNewId(TYPE.DOMAIN_IDS).toString());
		evaluation.setContentSource(nodeIdToDelete);
		evaluation.setName("evaluation name");
		evaluation.setOwnerId(ownerId.toString());
		evaluation.setCreatedOn(new Date());
		evaluation.setStatus(EvaluationStatus.OPEN);
		evaluationIdToDelete = evaluationDAO.create(evaluation, ownerId);
	}

	@After
	public void tearDown() throws Exception {
		if (nodeIdToDelete!=null) {
			nodeDAO.delete(nodeIdToDelete);
			nodeIdToDelete = null;
		}
		if (evaluationIdToDelete!=null) {
			evaluationDAO.delete(evaluationIdToDelete);
			evaluationIdToDelete = null;
		}
	}

	@Test
	public void testRoundTrip() throws Exception {
		Long evalIdLong = Long.parseLong(evaluationIdToDelete);
		EvaluationSubmissions evalSubs = 
				evaluationSubmissionsDAO.createForEvaluation(evalIdLong);
		assertEquals(evalIdLong, evalSubs.getEvaluationId());
		assertNotNull(evalSubs.getId());
		assertNotNull(evalSubs.getEtag());
		
		EvaluationSubmissions retrieved = evaluationSubmissionsDAO.getForEvaluation(evalIdLong);
		assertEquals(evalSubs, retrieved);
		
		retrieved = evaluationSubmissionsDAO.lockAndGetForEvaluation(evalIdLong);
		assertEquals(evalSubs, retrieved);
		
		String newEtag = evaluationSubmissionsDAO.updateEtagForEvaluation(evalIdLong, false);
		
		evalSubs.setEtag(newEtag);
		retrieved = evaluationSubmissionsDAO.getForEvaluation(evalIdLong);
		assertEquals(evalSubs, retrieved);
		
		evaluationSubmissionsDAO.deleteForEvaluation(evalIdLong);
		
		try {
			evaluationSubmissionsDAO.getForEvaluation(evalIdLong);
			fail("NotFoundException expected");
		} catch (NotFoundException e) {
			// as expected
		}
	}

}
