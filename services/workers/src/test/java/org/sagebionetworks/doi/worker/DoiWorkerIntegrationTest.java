package org.sagebionetworks.doi.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.doi.DoiManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.model.doi.v2.DoiCreator;
import org.sagebionetworks.repo.model.doi.v2.DoiRequest;
import org.sagebionetworks.repo.model.doi.v2.DoiResourceType;
import org.sagebionetworks.repo.model.doi.v2.DoiResourceTypeGeneral;
import org.sagebionetworks.repo.model.doi.v2.DoiResponse;
import org.sagebionetworks.repo.model.doi.v2.DoiTitle;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DoiWorkerIntegrationTest {
	
	public static final long MAX_WAIT_MS = 1000 * 60;
	
	@Autowired
	StackConfiguration config;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	UserManager userManager;
	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	DoiManager doiManager;
	@Autowired
	DoiAdminDao doiAdminDao;
	@Autowired
	EntityManager entityManager;

	Long adminUser;
	Project project;
	String projectId;
	Doi submissionDoi;

	private static final String title = "5 Easy Steps You Can Take To Become President (You Won't Believe #3!)";
	private static final String author = "Washington, George";
	private static final Long publicationYear = 1787L;
	private static final DoiResourceTypeGeneral resourceTypeGeneral = DoiResourceTypeGeneral.Dataset;

	@Before
	public void before(){
		project = new Project();
		project.setName("Some project that needs a DOI");
		adminUser = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		projectId = entityManager.createEntity(userManager.getUserInfo(adminUser), project, null);
		submissionDoi = setUpRequestBody();
	}
	
	@After
	public void after(){
		doiAdminDao.clear(); // Remove all DOI associations
		entityManager.deleteEntity(userManager.getUserInfo(adminUser), projectId);
	}

	@Test
	public void testCreateDoi() throws Exception {
		Assume.assumeTrue(config.getDoiDataciteEnabled());
		DoiRequest request = new DoiRequest();
		request.setDoi(submissionDoi);
		DoiResponse response = startAndWaitForJob(userManager.getUserInfo(adminUser), request, DoiResponse.class);
		assertNotNull(response);
		assertNotNull(response.getDoi());
		Doi responseDoi = response.getDoi();
		
		// Make sure the DOI refers to the project
		assertEquals(projectId, responseDoi.getObjectId());
		assertEquals(ObjectType.ENTITY, responseDoi.getObjectType());
		assertNull(responseDoi.getObjectVersion());
		
		// Make sure all of the metadata we get back matches the metadata we enter
		assertEquals(responseDoi.getCreators(), submissionDoi.getCreators());
		assertEquals(responseDoi.getTitles(), submissionDoi.getTitles());
		assertEquals(responseDoi.getStatus(), submissionDoi.getStatus());
		assertEquals(responseDoi.getPublicationYear(), submissionDoi.getPublicationYear());
		assertEquals(responseDoi.getResourceType(), submissionDoi.getResourceType());
	}

	/*
	 * Create a DTO with all fields we expect the user to enter.
\	 */
	private Doi setUpRequestBody() {
		Doi body = new Doi();
		body.setObjectId(projectId);
		body.setObjectType(ObjectType.ENTITY);
		// Required metadata fields
		DoiCreator doiCreator = new DoiCreator();
		doiCreator.setCreatorName(author);
		body.setCreators(Collections.singletonList(doiCreator));

		DoiTitle doiTitle = new DoiTitle();
		doiTitle.setTitle(title);
		body.setTitles(Collections.singletonList(doiTitle));

		body.setPublicationYear(publicationYear);

		DoiResourceType doiResourceType = new DoiResourceType();
		doiResourceType.setResourceTypeGeneral(resourceTypeGeneral);
		body.setResourceType(doiResourceType);
		return body;
	}

	/**
	 * Start an asynchronous job and wait for the results.
	 * @param user
	 * @param body
	 * @return
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("unchecked")
	public <T extends AsynchronousResponseBody> T  startAndWaitForJob(UserInfo user, AsynchronousRequestBody body, Class<? extends T> clazz) throws InterruptedException{
		long startTime = System.currentTimeMillis();
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(user, body);
		while(true){
			status = asynchJobStatusManager.getJobStatus(user, status.getJobId());
			switch(status.getJobState()){
			case FAILED:
				assertTrue("Job failed: "+status.getErrorDetails(), false);
			case PROCESSING:
				assertTrue("Timed out waiting for job to complete",(System.currentTimeMillis()-startTime) < MAX_WAIT_MS);
				System.out.println("Waiting for job: "+status.getProgressMessage());
				Thread.sleep(1000);
				break;
			case COMPLETE:
				return (T)status.getResponseBody();
			}
		}
	}
}
