package org.sagebionetworks.gepipeline;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.Task;
import com.amazonaws.services.simpleworkflow.flow.core.TryFinally;
import com.amazonaws.services.simpleworkflow.flow.junit.FlowBlockJUnit4ClassRunner;
import com.amazonaws.services.simpleworkflow.flow.junit.WorkflowTest;

/**
 * @author deflaux
 * 
 */
@RunWith(FlowBlockJUnit4ClassRunner.class)
public class GEPWorkflowTest {

	private static final String EXPECTED_RESULT = "workflow" + ":processData"
			+ ":notifyFollower";

	private final class TestGEPActivities implements GEPActivities {

		String result = "workflow";

		/**
		 * @return the result
		 */
		public String getResult() {
			return result;
		}

		@Override
		public ProcessDataResult processData(String script,
				String activityInput) {
			try {
				Thread.sleep(1000);
				// Delay is for the purpose of illustration

			} catch (InterruptedException e) {
			}
			result += ":processData";
			ProcessDataResult activityResult = new ProcessDataResult();
			activityResult.setResult(result);
			activityResult.setStdout("some stdout");
			activityResult.setStderr("some stderr");
			return activityResult;
		}

		@Override
		public void notifyFollower(String recipient, String subject,
				String message) {
			try {
				Thread.sleep(1000);
				// Delay is for the purpose of illustration
			} catch (InterruptedException e) {
			}
			result += ":notifyFollower";
		}

	}

	/**
     * 
     */
	@Rule
	public WorkflowTest workflowTest = new WorkflowTest();

	private final GEPWorkflowClientFactory workflowFactory = new GEPWorkflowClientFactoryImpl();

	private TestGEPActivities activitiesImplementation;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		workflowTest.addWorkflowImplementationType(GEPWorkflowImpl.class);
		activitiesImplementation = new TestGEPActivities();
		workflowTest.addActivitiesImplementation(GEPWorkflow.SMALL_ACTIVITY_TASK_LIST, activitiesImplementation);
	}

	/**
	 * Test through generated workflow client. As workflow unit tests run in
	 * dummy workflow context the same client that is used for creation of child
	 * workflows is used.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testThroughClient() throws Exception {
		GEPWorkflowClient workflow = workflowFactory.getClient();
		Promise<Void> done = workflow.runMetaGenomicsPipeline("GSE1234",
				GEPWorkflow.ACTIVITY_REQUIREMENT_SMALL);
		assertResult(done);
	}

	@Asynchronous
	private void assertResult(Promise<Void> done) {
		Assert.assertEquals(EXPECTED_RESULT, activitiesImplementation
				.getResult());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testThroughClientAssertWithTask() throws Exception {
		GEPWorkflowClient workflow = workflowFactory.getClient();
		Promise<Void> done = workflow.runMetaGenomicsPipeline("GSE1234",
				GEPWorkflow.ACTIVITY_REQUIREMENT_SMALL);
		new Task(done) {

			@Override
			protected void doExecute() throws Throwable {
				Assert.assertEquals(EXPECTED_RESULT, activitiesImplementation
						.getResult());
			}
		};
	}

	/**
	 * Instantiate workflow implementation object directly. Note that any object
	 * that is part of workflow can be unit tested through direct instantiation.
	 */
	@Test
	public void directTest() {
		final GEPWorkflow workflow = new GEPWorkflowImpl();
		new TryFinally() {

			@Override
			protected void doTry() throws Throwable {
				// this workflow returns void so we use TryFinally
				// to wait for its completion
				workflow.runMetaGenomicsPipeline("GSE1234",
						GEPWorkflow.ACTIVITY_REQUIREMENT_SMALL);
			}

			@Override
			protected void doFinally() throws Throwable {
				Assert.assertEquals(EXPECTED_RESULT, activitiesImplementation
						.getResult());
			}
		};
	}
}
