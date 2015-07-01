package org.sagebionetworks.rds.workers;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This test validates that entity messages pushed to the topic propagate to the rds queue,
 * then processed by the worker and pushed to the search index.
 * 
 * @author jmhill
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityAnnotationWorkerIntegrationTest {
	
	public static final long MAX_WAIT = 60*1000; // one minute
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private NodeQueryDao nodeQueryDao;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private SemaphoreManager semphoreManager;
	
	private UserInfo adminUserInfo;
	private final String key = "some_annotation_key";
	private String uniqueValue;
	private Project project;
	
	@Before
	public void before() throws Exception {
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Create a project
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		project = new Project();
		project.setName("RdsWorkerIntegrationTest.Project");
		// this should trigger create message.
		String id = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, id, Project.class);
		// Add an annotation to query for
		Annotations annos = entityManager.getAnnotations(adminUserInfo, id);
		uniqueValue = UUID.randomUUID().toString();
		annos.addAnnotation(key, uniqueValue);
		entityManager.updateAnnotations(adminUserInfo, id, annos);
	}
	
	@After
	public void after() throws Exception {
		entityManager.deleteEntity(adminUserInfo, project.getId());
	}
	
	
	@Test
	public void testRoundTrip() throws Exception {
		// First run query
		BasicQuery query = new BasicQuery();
		query.addExpression(new Expression(new CompoundId(null, key), Comparator.EQUALS, uniqueValue));
		long start = System.currentTimeMillis();
		while(nodeQueryDao.executeCountQuery(query, adminUserInfo)< 1){
			System.out.println("Waiting for annotations index to be updated for entity: "+project.getId());
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for annotaion index to be updated for entity: "+project.getId(),elapse < MAX_WAIT);
		}
		System.out.println("Annotations index was updated for entity "+project.getId());
	}
	
}
