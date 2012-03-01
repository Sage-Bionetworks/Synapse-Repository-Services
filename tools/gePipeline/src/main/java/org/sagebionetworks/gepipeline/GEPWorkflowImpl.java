package org.sagebionetworks.gepipeline;

import org.joda.time.LocalDate;

import com.amazonaws.services.simpleworkflow.flow.ActivitySchedulingOptions;
import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;

/**
 * 
 */
public class GEPWorkflowImpl implements GEPWorkflow {

	private static final String NOTIFICATION_SUBJECT = "GEP Workflow Notification ";
	private static final String NOTIFICATION_SNS_TOPIC = GEPWorkflowConfigHelper
			.getWorkflowSnsTopic();
	private static final String script = GEPWorkflowConfigHelper
			.getGEPipelineWorkflowScript();

	// TODO use return code for this status rather than parsing message
	private static final String NO_CHANGE_MESSAGE = "has not changed since last update";

	// for testing, can set the workflow to 'no-op', i.e. just close out the
	// instance
	// without doing anything
	private static final String NOOP = GEPWorkflowConfigHelper
			.getGEPipelineNoop();

	private GEPActivitiesClient client;

	/**
	 * Default constructor
	 */
	public GEPWorkflowImpl() {
		this(new GEPActivitiesClientImpl());
	}

	/**
	 * Constructor for unit testing or if we are using Spring to wire this up
	 * 
	 * @param client
	 */
	public GEPWorkflowImpl(GEPActivitiesClient client) {
		this.client = client;
	}

	@Override
	public void runMetaGenomicsPipeline(final String activityInput,
			final String activityRequirement) {

		new TryCatchFinally() {

			@Override
			protected void doTry() throws Throwable {

				boolean noop = (NOOP != null && NOOP.equalsIgnoreCase("true"));

				if (noop) {
					Promise<String> result = Promise.asPromise("NO-OP for "
							+ activityInput);
					notifyDataProcessed(result);
					return;
				}

				/**
				 * Run the processing step(s) on this data
				 */
				// 'result' is the message returned by the workflow,
				// other returned data are in 'processedLayerId', 'stdout',
				// 'stderr'
				Promise<String> result = processData(activityInput,
						activityRequirement);

				notifyDataProcessed(result);
			}

			@Override
			protected void doCatch(Throwable e) throws Throwable {
				throw e;
			}

			@Override
			protected void doFinally() throws Throwable {
				// do nothing
			}
		};

	}

	@Asynchronous
	private void notifyDataProcessed(Promise<String> result) {
		if (hasChanged(result.get())) {
			// note, the output is in 'message'
			client.notifyFollower(NOTIFICATION_SNS_TOPIC, NOTIFICATION_SUBJECT
					+ new LocalDate().toString(), result.get());
		}
	}

	private static boolean hasChanged(String msg) {
		return msg.indexOf(NO_CHANGE_MESSAGE) < 0;
	}

	// different datasets require different size machines. The capacity
	// requirement is
	// passed in as 'activityRequirement'
	@Asynchronous
	private Promise<String> processData(String activityInput,
			String activityRequirement) {

		ActivitySchedulingOptions options = new ActivitySchedulingOptions();

		if (ACTIVITY_REQUIREMENT_SMALL.equals(activityRequirement)) {
			options.setTaskList(SMALL_ACTIVITY_TASK_LIST);
		} else if (ACTIVITY_REQUIREMENT_MEDIUM.equals(activityRequirement)) {
			options.setTaskList(MEDIUM_ACTIVITY_TASK_LIST);
		} else if (ACTIVITY_REQUIREMENT_LARGE.equals(activityRequirement)) {
			options.setTaskList(LARGE_ACTIVITY_TASK_LIST);
		} else if (ACTIVITY_REQUIREMENT_EXTRA_LARGE.equals(activityRequirement)) {
			options.setTaskList(EXTRA_LARGE_ACTIVITY_TASK_LIST);
		} else {
			throw new IllegalArgumentException("Unexpected "
					+ activityRequirement);
		}

		return client.processData(script, activityInput);
	}

}
