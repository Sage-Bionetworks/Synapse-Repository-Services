package org.sagebionetworks.repo.model.dbo.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ThreadStepper;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AsynchJobStatusDaoImplTest {
	
	@Autowired
	AsynchronousJobStatusDAO asynchJobStatusDao;

	@Resource(name = "txManager")
	private PlatformTransactionManager transactionManager;

	private Long creatorUserGroupId;

	@Before
	public void before(){
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		assertNotNull(creatorUserGroupId);
	}
	
	@After
	public void after(){
		asynchJobStatusDao.truncateAllAsynchTableJobStatus();
	}
	
	@Test
	public void testQueueNameBeans() {
		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext(new String[] { "stack-configuration.spb.xml" });
		// of course, an ApplicationContext is just a BeanFactory
		BeanFactory factory = (BeanFactory) appContext;

		for (AsynchJobType asynchJobType : AsynchJobType.values()) {
			assertEquals(asynchJobType.getQueueName(), factory.getBean("stackConfiguration.asyncQueueName[" + asynchJobType.name() + "]"));
		}
		appContext.close();
	}

	@Test
	public void testUploadCreateGet() throws DatastoreException, NotFoundException{
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId("syn456");
		body.setUploadFileHandleId("123");
		AsynchronousJobStatus status = asynchJobStatusDao.startJob(creatorUserGroupId, body);
		assertNotNull(status.getJobId());
		assertNotNull(status.getEtag());
		assertNotNull(status.getChangedOn());
		assertNotNull(status.getStartedOn());
		assertNull(status.getErrorDetails());
		assertNull(status.getErrorMessage());
		assertNotNull(status.getRuntimeMS());
		assertEquals(AsynchJobState.PROCESSING, status.getJobState());
		assertEquals(creatorUserGroupId, status.getStartedByUserId());
		assertEquals(body, status.getRequestBody());
		
		AsynchronousJobStatus clone = asynchJobStatusDao.getJobStatus(status.getJobId());
		assertEquals(status, clone);
	}
	
	@Test (expected=NotFoundException.class)
	public void testNotFound() throws DatastoreException, NotFoundException{
		asynchJobStatusDao.getJobStatus("-99");
	}
	
	@Test
	public void testUpdateProgress() throws DatastoreException, NotFoundException{
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId("syn456");
		body.setUploadFileHandleId("123");
		AsynchronousJobStatus status = asynchJobStatusDao.startJob(creatorUserGroupId, body);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		// update the progress
		asynchJobStatusDao.updateJobProgress(status.getJobId(), 0L, 1000L, "A MESSAGE");
		AsynchronousJobStatus clone = asynchJobStatusDao.getJobStatus(status.getJobId());
		assertEquals(new Long(0), clone.getProgressCurrent());
		assertEquals(new Long(1000), clone.getProgressTotal());
		assertEquals("A MESSAGE", clone.getProgressMessage());
		assertEquals(body, status.getRequestBody());
		assertEquals(AsynchJobState.PROCESSING, status.getJobState());
	}
	
	@Test
	public void testUpdateProgressNotProcessing() throws DatastoreException, NotFoundException {
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId("syn456");
		body.setUploadFileHandleId("123");
		AsynchronousJobStatus status = asynchJobStatusDao.startJob(creatorUserGroupId, body);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		// update the progress
		asynchJobStatusDao.updateJobProgress(status.getJobId(), 0L, 1000L, "A MESSAGE");
		AsynchronousJobStatus clone = asynchJobStatusDao.getJobStatus(status.getJobId());
		assertEquals("A MESSAGE", clone.getProgressMessage());
		assertEquals(AsynchJobState.PROCESSING, status.getJobState());

		UploadToTableResult response = new UploadToTableResult();
		response.setRowsProcessed(7L);
		asynchJobStatusDao.setComplete(status.getJobId(), response);
		clone = asynchJobStatusDao.getJobStatus(status.getJobId());
		assertEquals("Complete", clone.getProgressMessage());
		assertEquals(AsynchJobState.COMPLETE, clone.getJobState());

		asynchJobStatusDao.updateJobProgress(status.getJobId(), 3L, 2000L, "A MESSAGE2");
		clone = asynchJobStatusDao.getJobStatus(status.getJobId());
		assertEquals("Complete", clone.getProgressMessage());
	}

	@Test
	public void testUpdateProgressDuringTransaction() throws Exception {
		final AtomicReference<String> jobId = new AtomicReference<String>();

		final ThreadStepper stepper = new ThreadStepper(20);

		stepper.add(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					DefaultTransactionDefinition transactionDef = new DefaultTransactionDefinition();
					transactionDef.setReadOnly(false);
					transactionDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
					transactionDef.setName("Test");
					TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager, transactionDef);

					transactionTemplate.execute(new TransactionCallback<String>() {
						@Override
						public String doInTransaction(TransactionStatus tStatus) {
							try {
								UploadToTableRequest body = new UploadToTableRequest();
								body.setTableId("syn456");
								body.setUploadFileHandleId("123");
								AsynchronousJobStatus status = asynchJobStatusDao.startJob(creatorUserGroupId, body);
								assertNotNull(status);
								assertNotNull(status.getEtag());
								jobId.set(status.getJobId());

								stepper.stepDone("job started");
								stepper.waitForStepDone("get status initial");

								asynchJobStatusDao.updateJobProgress(status.getJobId(), 0L, 1000L, "A MESSAGE");

								stepper.stepDone("progress reported 1");
								stepper.waitForStepDone("get status 1");

								asynchJobStatusDao.updateJobProgress(status.getJobId(), 0L, 1000L, "A MESSAGE2");

								stepper.stepDone("progress reported 2");
								stepper.waitForStepDone("get status 2");

								UploadToTableResult response = new UploadToTableResult();
								response.setRowsProcessed(7L);
								asynchJobStatusDao.setComplete(status.getJobId(), response);

								stepper.stepDone("job completed");
								stepper.waitForStepDone("get status completed in transaction");
							} catch (Exception e) {
								fail("got exception: " + e.getMessage());
							}
							return "";
						}
					});

					stepper.stepDone("out of transaction");
					stepper.waitForStepDone("get status completed not in transaction");
				} catch (Exception e) {
					fail("got exception: " + e.getMessage());
				}
				return null;
			}
		});
		
		stepper.add(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					stepper.waitForStepDone("job started");

					AsynchronousJobStatus clone = asynchJobStatusDao.getJobStatus(jobId.get());
					assertEquals(null, clone.getProgressMessage());
					assertEquals(AsynchJobState.PROCESSING, clone.getJobState());

					stepper.stepDone("get status initial");
					stepper.waitForStepDone("progress reported 1");

					clone = asynchJobStatusDao.getJobStatus(jobId.get());
					assertEquals("A MESSAGE", clone.getProgressMessage());

					stepper.stepDone("get status 1");
					stepper.waitForStepDone("progress reported 2");

					clone = asynchJobStatusDao.getJobStatus(jobId.get());
					assertEquals("A MESSAGE2", clone.getProgressMessage());

					stepper.stepDone("get status 2");
					stepper.waitForStepDone("job completed");

					clone = asynchJobStatusDao.getJobStatus(jobId.get());
					assertEquals("A MESSAGE2", clone.getProgressMessage());

					stepper.stepDone("get status completed in transaction");
					stepper.waitForStepDone("out of transaction");

					clone = asynchJobStatusDao.getJobStatus(jobId.get());
					assertEquals("Complete", clone.getProgressMessage());
					assertEquals(AsynchJobState.COMPLETE, clone.getJobState());

					stepper.stepDone("get status completed not in transaction");
				} catch (Exception e) {
					fail("got exception: " + e.getMessage());
				}
				return null;
			}
		});

		stepper.run();
	}

	@Test
	public void testUpdateProgressTooBig() throws DatastoreException, NotFoundException{
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId("syn456");
		body.setUploadFileHandleId("123");
		AsynchronousJobStatus status = asynchJobStatusDao.startJob(creatorUserGroupId, body);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		// update the progress
		char[] chars = new char[DBOAsynchJobStatus.MAX_MESSAGE_CHARS+1];
		Arrays.fill(chars, '1');
		String tooBig = new String(chars);
		asynchJobStatusDao.updateJobProgress(status.getJobId(), 0L, 1000L, tooBig);
		AsynchronousJobStatus clone = asynchJobStatusDao.getJobStatus(status.getJobId());
		assertEquals(new Long(0), clone.getProgressCurrent());
		assertEquals(new Long(1000), clone.getProgressTotal());
		assertEquals(tooBig.substring(0,  DBOAsynchJobStatus.MAX_MESSAGE_CHARS-1), clone.getProgressMessage());
		assertEquals(body, status.getRequestBody());
	}
	
	@Test
	public void testSetFailed() throws DatastoreException, NotFoundException{
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId("syn456");
		body.setUploadFileHandleId("123");
		AsynchronousJobStatus status = asynchJobStatusDao.startJob(creatorUserGroupId, body);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String startEtag = status.getEtag();
		// update the progress
		Throwable error = new Throwable("something when wrong", new IllegalArgumentException("This is bad"));
		String newEtag = asynchJobStatusDao.setJobFailed(status.getJobId(), error);
		assertNotNull(newEtag);
		assertFalse("The etag must change when the status changes",startEtag.equals(newEtag));
		// Get the status
		AsynchronousJobStatus clone = asynchJobStatusDao.getJobStatus(status.getJobId());
		assertEquals("something when wrong", clone.getErrorMessage());
		assertEquals(AsynchJobState.FAILED, clone.getJobState());
		System.out.println(clone.getErrorDetails());
		assertNotNull(clone.getErrorDetails());
		assertTrue(clone.getErrorDetails().contains("This is bad"));
		assertEquals(newEtag, clone.getEtag());
		assertEquals(body, status.getRequestBody());
		assertEquals(AsynchJobState.PROCESSING, status.getJobState());
	}
	
	@Test
	public void testSetComplete() throws DatastoreException, NotFoundException, InterruptedException{
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId("syn456");
		body.setUploadFileHandleId("123");
		AsynchronousJobStatus status = asynchJobStatusDao.startJob(creatorUserGroupId, body);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String previousEtag = status.getEtag();
		// Update the progress
		asynchJobStatusDao.updateJobProgress(status.getJobId(), 10L, 100L, "Made some progress");
		// Now set it complete
		// Make sure at at least some time has passed before me set it complete
		Thread.sleep(10);
		UploadToTableResult response = new UploadToTableResult();
		response.setRowsProcessed(7L);
		String newEtag = asynchJobStatusDao.setComplete(status.getJobId(), response);
		assertNotNull(newEtag);
		assertFalse(previousEtag.equals(newEtag));
		AsynchronousJobStatus result = asynchJobStatusDao.getJobStatus(status.getJobId());
		assertNotNull(result);
		assertNotNull(result.getEtag());
		assertFalse(previousEtag.equals(result.getEtag()));
		assertTrue(result.getRuntimeMS() >= 10);
		assertNull(result.getErrorDetails());
		assertNull(result.getErrorMessage());
		assertEquals(new Long(100l), result.getProgressCurrent());
		assertEquals(new Long(100l), result.getProgressTotal());
		assertEquals(AsynchJobState.COMPLETE, result.getJobState());
		assertNotNull(result.getChangedOn());
		assertNotNull(result.getStartedOn());
		assertTrue(result.getChangedOn().getTime() > result.getStartedOn().getTime());
		assertEquals(body, result.getRequestBody());
		assertEquals(response, result.getResponseBody());
	}
	
}
