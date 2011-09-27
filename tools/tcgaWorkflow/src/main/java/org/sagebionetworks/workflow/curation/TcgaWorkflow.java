package org.sagebionetworks.workflow.curation;

import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.joda.time.LocalDate;
import org.sagebionetworks.workflow.Constants;
import org.sagebionetworks.workflow.activity.Curation;
import org.sagebionetworks.workflow.activity.Notification;
import org.sagebionetworks.workflow.activity.Processing;
import org.sagebionetworks.workflow.activity.Processing.ScriptResult;

import com.amazonaws.services.simpleworkflow.client.activity.ActivityFailureException;
import com.amazonaws.services.simpleworkflow.client.asynchrony.Settable;
import com.amazonaws.services.simpleworkflow.client.asynchrony.Value;
import com.amazonaws.services.simpleworkflow.client.asynchrony.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.Activity;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ActivityRegistrationOptions;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ActivitySchedulingOptions;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.Duration;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.DurationUnit;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ExponentialRetry;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.Workflow;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.WorkflowRegistrationOptions;

/**
 * TODO
 * 
 * - Think about unnecessary repeated processing versus instances were we really
 * do want to rerun the activity. (e.g., use md5 checksums to eliminate
 * redundant downloads/uploads)
 * 
 * - If an activity fails, the workflow fails at that step since we are
 * currently not handling any exceptions; incrementally add error recovery as
 * appropriate
 * 
 * - Keeping things simple for now, all the R scripts will get executed with all
 * new raw tcgaData and they will examine the metadata to determine whether they
 * want to work on it or not. That way the scientists have more control over the
 * workflow. A returned processLayerId of -1 indicates that the script executed
 * successfully but chose not to work on the data.
 * 
 * @author deflaux
 * 
 */
public class TcgaWorkflow {

	/**
	 * Workflow and Activity Annotations Constants
	 * 
	 * Dev Note: annotation values must be resolvable at compilation time, not
	 * runtime, therefore we cannot move this into a config file
	 */
	private static final String WORKFLOW_NAME = "DemoWorkflow";
	private static final String VERSION = "1.4";
	private static final int MAX_WORKFLOW_TIMEOUT_HOURS = 24;
	private static final int MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS = 4;
	private static final int NUM_RETRIES = 3;

	/**
	 * Other Constants (can be determined at runtime)
	 */
	// Even though we are sending stdout and stderr from the R script to a log
	// file, include a portion of that output in the workflow history for
	// convenience. We only want to dig through the logs if we need to.
	private static final int MAX_SCRIPT_OUTPUT = 1024;
	private static final String NOTIFICATION_SUBJECT = "TCGA Workflow Notification ";
	private static final String NOTIFICATION_SNS_TOPIC = ConfigHelper
			.getWorkflowSnsTopic();
	
	/**
	 * @param param
	 * @param datasetId
	 * @param tcgaUrl
	 * @throws Exception
	 */
	@Workflow(name = WORKFLOW_NAME, version = VERSION)
	@WorkflowRegistrationOptions(defaultWorkflowLifetimeTimeout = @Duration(time = MAX_WORKFLOW_TIMEOUT_HOURS, unit = DurationUnit.Hours))
	public static void doWorkflow(String param, String datasetId, String tcgaUrl)
			throws Exception {

		TcgaWorkflow flow = new TcgaWorkflow();

		/**
		 * Create a metadata entity in the repository service for the raw TGCA
		 * data layer
		 */
		Settable<String> rawLayerId = new Settable<String>();
		Value<String> result1 = flow.dispatchCreateMetadata(Value
				.asValue(param), true, Value.asValue(datasetId), Value
				.asValue(tcgaUrl), rawLayerId);

		/**
		 * Formulate the message to be sent to all interested parties about the
		 * new raw data from TCGA
		 * 
		 * Send the email notification to all interested parties, keeping this
		 * simple for now, later on we'll want to check their communication
		 * preferences and batch up these notifications as configured
		 */
		Value<String> result2 = flow.dispatchNotifyDataProcessed(result1,
				rawLayerId);

		/**
		 * Dynamically discover the R scripts to run on this data
		 */
		// TODO add an activity step to look for scripts in a known location
		// where they were dropped off by our scientists
		// and kick off one processData task per script in parallel
		Settable<String> script = new Settable<String>();
		script.set("./src/test/resources/createMatrix.r");

		// TODO once we have that list of scripts, split the workflow here into
		// parallel tasks for the remainder of this pipeline,
		// we don't need a join, but perhaps an accumulation of notifications
		// when it gets spammy

		/**
		 * Run the processing step(s) on this data
		 */
		Settable<String> processedLayerId = new Settable<String>();
		Settable<String> stdout = new Settable<String>();
		Settable<String> stderr = new Settable<String>();
		Value<String> result3 = flow.dispatchProcessData(result2, script,
				datasetId, rawLayerId,
				// machineName,
				processedLayerId, stdout, stderr);

		flow.dispatchNotifyDataProcessed(result3, processedLayerId);

	}

	@Asynchronous
	private Value<String> dispatchCreateMetadata(Value<String> param,
			Boolean doneIfExists, Value<String> datasetId,
			Value<String> tcgaUrl, Settable<String> rawLayerId)
			throws Exception {
		return doCreateMetadata(param.get(), doneIfExists, datasetId.get(),
				tcgaUrl.get(), rawLayerId);
	}

	@Activity(version = VERSION)
	@ExponentialRetry(minimumAttempts = NUM_RETRIES, maximumAttempts = NUM_RETRIES)
	private static Value<String> doCreateMetadata(String param,
			Boolean doneIfExists, String datasetId, String tcgaUrl,
			Settable<String> rawLayerId) throws Exception {
		// Create a new layer, if necessary, in the synapse repository service
		// and return its id

		// This activity will be simple at first (just infer metadata from the
		// structure of the TCGA url) but it could become more complicated in
		// time if we need to pull additional metadata from other TCGA services
		String synapseLayerId = null;
//		try {
//			synapseLayerId = Curation
//					.doCreateSynapseMetadataForTcgaSourceLayer(doneIfExists,
//							datasetId, tcgaUrl);
//			rawLayerId.set(synapseLayerId);
			rawLayerId.set("123");
//		} catch (SocketTimeoutException e) {
//			throw new ActivityFailureException(400,
//					"Communication timeout, try this again", e);
//		}
		return Value.asValue(param + ":CreateMetadata");
	}

	@Asynchronous
	private Value<String> dispatchProcessData(Value<String> param,
			Value<String> script, String datasetId, Value<String> rawLayerId,
			// Value<String> machineName,
			Settable<String> processedLayerId, Settable<String> stdout,
			Settable<String> stderr) throws Exception {

		if (Constants.WORKFLOW_DONE.equals(rawLayerId.get())) {
			return Value.asValue(param + ":noop");
		}
		return doProcessData(param.get(), script.get(), datasetId, rawLayerId
				.get(),
		// machineName.get(),
				processedLayerId, stdout, stderr);
	}

	@Activity(version = VERSION)
	@ExponentialRetry(minimumAttempts = NUM_RETRIES, maximumAttempts = NUM_RETRIES)
	@ActivityRegistrationOptions(
			defaultLifetimeTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours), 
			taskLivenessTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours))
	@ActivitySchedulingOptions(
			lifetimeTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours), 
			queueTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours))
	private static Value<String> doProcessData(String param, String script,
			String datasetId,
			String rawLayerId,
			// @ActivitySchedulingOption(option =
			// ActivitySchedulingElement.requirement) String machineName,
			Settable<String> processedLayerId, Settable<String> stdout,
			Settable<String> stderr) throws Exception {

		// TODO heartbeat thread with shorter taskLivenessTimeout?
		ScriptResult result = Processing.doProcessLayer(script, datasetId,
				rawLayerId);
		processedLayerId.set(result.getProcessedLayerId());
		stdout.set((MAX_SCRIPT_OUTPUT > result.getStdout().length()) ? result
				.getStdout() : result.getStdout().substring(0,
				MAX_SCRIPT_OUTPUT));
		stderr.set((MAX_SCRIPT_OUTPUT > result.getStderr().length()) ? result
				.getStderr() : result.getStderr().substring(0,
				MAX_SCRIPT_OUTPUT));
		return Value.asValue(param + ":ProcessData");
	}

	@Asynchronous
	private Value<String> dispatchNotifyDataProcessed(Value<String> param,
			Value<String> processedLayerId) throws Exception {

		if (Constants.WORKFLOW_DONE.equals(processedLayerId.get())) {
			return Value.asValue(param + ":noop");
		}

		/**
		 * Formulate the message to be sent to all interested parties about the
		 * new processed data from TCGA
		 */
		Settable<String> processedDataMessage = new Settable<String>();
		Value<String> result1 = dispatchFormulateNotificationMessage(param,
				processedLayerId, processedDataMessage);

		Settable<String> recipient = new Settable<String>();
		recipient.set(NOTIFICATION_SNS_TOPIC);
		Settable<String> subject = new Settable<String>();
		subject.set(NOTIFICATION_SUBJECT + new LocalDate().toString());

		/**
		 * Send the email notification to all interested parties, keeping this
		 * simple for now, later on we'll want to check their communication
		 * preferences and batch up these notifications as configured
		 */
		Value<String> result2 = dispatchNotifyFollower(result1, recipient,
				subject, processedDataMessage);

		return result2;
	}

	@Asynchronous
	private Value<String> dispatchFormulateNotificationMessage(
			Value<String> param, Value<String> layerId, Settable<String> message)
			throws Exception {
		return doFormulateNotificationMessage(param.get(), layerId.get(),
				message);
	}

	@Activity(version = VERSION)
	private static Value<String> doFormulateNotificationMessage(String param,
			String layerId, Settable<String> message) throws Exception {
		message.set(Curation.formulateLayerCreationMessage(layerId));
		return Value.asValue(param + ":FormulateNotificationMessage");
	}

	@Asynchronous
	private Value<String> dispatchNotifyFollower(Value<String> param,
			Value<String> recipient, Value<String> subject,
			Value<String> message) {
		return doNotifyFollower(param, recipient, subject, message);
	}

	@Activity(version = VERSION)
	private static Value<String> doNotifyFollower(Value<String> param,
			Value<String> recipient, Value<String> subject,
			Value<String> message) {
		Notification.doSnsNotifyFollowers(recipient.get(), subject.get(),
				message.get());
		return Value.asValue(param.get() + ":NotifyFollowers");
	}

	/**
	 * @return the hostname of the local machine
	 */
	public static String getHostName() {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			return addr.getHostName();
		} catch (UnknownHostException e) {
			throw new Error(e);
		}
	}
}
