package org.sagebionetworks.workflow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.utils.HttpClientHelperException;

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
public class SageCommonsWorkflowTest {

	private static final String EXPECTED_RESULT = "workflow" + ":getLayer"
			+ ":processSpreadsheet"
			+ ":runRScript" 
			+ ":runRScript"
			+ ":runRScript"
			+ ":formulateNotificationMessage"
			+ ":notifyFollowers" ;

	private final class TestSageCommonsActivities implements
			SageCommonsActivities {

		String result = "workflow";

		// We can directly test some of the activity impl in the context of a
		// workflow unit test, so here we call what we can and mock out the rest
		SageCommonsActivitiesImpl realImpl = new SageCommonsActivitiesImpl(new MockSageCommonsChildWorkflowDispatcher(childWorkflowFactory));

		/**
		 * @return the result
		 */
		public String getResult() {
			return result;
		}

		@Override
		public Layer getLayer(String layerId) throws SynapseException {
			// Make a fake layer as if we got it from Synapse
			LocationData location = new LocationData();
			location.setType(LocationTypeNames.awss3);
			location.setPath("http://thisGoesNowhere.com/");
			List<LocationData> locations = new ArrayList<LocationData>();
			locations.add(location);

			Layer layer = new Layer();
			layer.setId(layerId);
			layer.setName("Test Layer");
			layer.setLocations(locations);

			result += ":getLayer";
			return layer;
		}

		@Override
		public Integer processSpreadsheet(String url) throws IOException,
				HttpClientHelperException {

			result += ":processSpreadsheet";
			// use a sample file from Brig
			File file = new File("./src/test/resources/tcga.zip");
			return realImpl.processSpreadsheetContents(file);
		}

		@Override
		public ActivityScriptResult runRScript(String script,
				String spreadsheetData) throws IOException,
				InterruptedException, UnrecoverableException, JSONException {
			result += ":runRScript";
			return realImpl.runRScript(script, spreadsheetData);
		}

		@Override
		public String formulateNotificationMessage(Layer layer,
				Integer numJobsDispatched) throws SynapseException,
				JSONException, UnrecoverableException {
			result += ":formulateNotificationMessage";
			return realImpl.formulateNotificationMessage(layer,
					numJobsDispatched);
		}

		@Override
		public void notifyFollowers(String recipient, String subject,
				String message) {
			try {
				Thread.sleep(1000);
				// Delay is for the purpose of illustration
			} catch (InterruptedException e) {
			}
			result += ":notifyFollowers";
		}

		@Override
		public Integer processSpreadsheetContents(File file) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

	}

	/**
     * 
     */
	@Rule
	public WorkflowTest workflowTest = new WorkflowTest();

	private final SageCommonsWorkflowClientFactory workflowFactory = new SageCommonsWorkflowClientFactoryImpl();
	private final SageCommonsRScriptWorkflowClientFactory childWorkflowFactory = new SageCommonsRScriptWorkflowClientFactoryImpl();

	private TestSageCommonsActivities activitiesImplementation;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		workflowTest
				.addWorkflowImplementationType(SageCommonsWorkflowImpl.class);
		workflowTest
				.addWorkflowImplementationType(SageCommonsRScriptWorkflowImpl.class);
		activitiesImplementation = new TestSageCommonsActivities();
		workflowTest.addActivitiesImplementation(activitiesImplementation);
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
		SageCommonsWorkflowClient workflow = workflowFactory.getClient();
		Promise<Void> done = workflow.processSubmission("42");
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
		SageCommonsWorkflowClient workflow = workflowFactory.getClient();
		Promise<Void> done = workflow.processSubmission("42");
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
		final SageCommonsWorkflow workflow = new SageCommonsWorkflowImpl();
		new TryFinally() {

			@Override
			protected void doTry() throws Throwable {
				// this returns void so we use TryFinally
				// to wait for its completion
				workflow.processSubmission("42");
			}

			@Override
			protected void doFinally() throws Throwable {
				Assert.assertEquals(EXPECTED_RESULT, activitiesImplementation
						.getResult());
			}
		};
	}
}
