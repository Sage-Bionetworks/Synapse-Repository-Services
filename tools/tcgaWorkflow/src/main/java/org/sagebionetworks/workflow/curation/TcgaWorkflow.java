package org.sagebionetworks.workflow.curation;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

import org.sagebionetworks.utils.HttpClientHelperException;
import org.sagebionetworks.workflow.UnrecoverableException;
import org.sagebionetworks.workflow.activity.Constants;
import org.sagebionetworks.workflow.activity.Curation;
import org.sagebionetworks.workflow.activity.DataIngestion;
import org.sagebionetworks.workflow.activity.Notification;
import org.sagebionetworks.workflow.activity.Processing;
import org.sagebionetworks.workflow.activity.Storage;
import org.sagebionetworks.workflow.activity.DataIngestion.DownloadResult;
import org.sagebionetworks.workflow.activity.Processing.ScriptResult;

import com.amazonaws.services.simpleworkflow.client.activity.ActivityFailureException;
import com.amazonaws.services.simpleworkflow.client.asynchrony.Settable;
import com.amazonaws.services.simpleworkflow.client.asynchrony.Value;
import com.amazonaws.services.simpleworkflow.client.asynchrony.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.Activity;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ActivityRegistrationOptions;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ActivitySchedulingElement;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ActivitySchedulingOption;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ActivitySchedulingOptions;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.Duration;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.DurationUnit;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ExponentialRetry;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.Workflow;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.WorkflowRegistrationOptions;

/**
 * All the activity bodies below are merely stubs. Once the workflow seems
 * adequately described, the full activities will be factored out from here and
 * implemented in a class per activity.
 * 
 * TODO
 * 
 * - rip out the 'param' parameter to each activity, it was just for initial
 * debugging/learning on my part
 * 
 * - Ensure that all activities are idempotent.
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

	// Even though we are sending stdout and stderr from the R script to a log
	// file, include a portion of that output in the workflow history for
	// convenience. We only want to dig through the logs if we need to.
	private static final int MAX_SCRIPT_OUTPUT = 1024;

	private static final int MAX_SCRIPT_EXECUTION_HOURS_TIMEOUT = ConfigHelper
			.createConfig().getMaxScriptExecutionHoursTimeout();

	private static final String NOTIFICATION_SUBJECT = "TCGA Workflow Notification";
	private static final String NOTIFICATION_SNS_TOPIC = ConfigHelper
			.createConfig().getSnsTopic();

	/**
	 * @param param
	 * @param datasetId
	 * @param tcgaUrl
	 * @throws Exception
	 */
	@Workflow(name = "TcgaWorkflow", version = "1.2")
	@WorkflowRegistrationOptions(defaultWorkflowLifetimeTimeout = @Duration(time = 24, unit = DurationUnit.Hours))
	public static void doWorkflow(String param, String datasetId,
			String tcgaUrl) throws Exception {

		TcgaWorkflow flow = new TcgaWorkflow();

		/**
		 * Create a metadata entity in the repository service for the raw TGCA
		 * data layer
		 */
		Settable<String> rawLayerId = new Settable<String>();
		Value<String> result1 = flow.dispatchCreateMetadata(Value
				.asValue(param), Value.asValue(datasetId), Value
				.asValue(tcgaUrl), rawLayerId);

		/**
		 * Download the raw data from TCGA
		 */
//		Settable<String> machineName = new Settable<String>();
//		Settable<String> localFilepath = new Settable<String>();
//		Settable<String> md5 = new Settable<String>();
//		Value<String> result2 = flow.dispatchDownloadDataFromTcga(result1,
//				Value.asValue(tcgaUrl), machineName, localFilepath, md5);

		/**
		 * Upload the data to S3
		 */
//		Value<String> result3 = flow.dispatchUploadLayerToStorage(result2,
//				datasetId, rawLayerId, machineName, localFilepath, md5);

		/**
		 * Formulate the message to be sent to all interested parties about the
		 * new raw data from TCGA
		 */
		Settable<String> rawDataMessage = new Settable<String>();
		Value<String> result4 = flow.dispatchFormulateNotificationMessage(
				result1, rawLayerId, rawDataMessage);

		/**
		 * Send the email notification to all interested parties, keeping this
		 * simple for now, later on we'll want to check their communication
		 * preferences and batch up these notifications as configured
		 */
		Settable<String> recipient = new Settable<String>();
		recipient.set(NOTIFICATION_SNS_TOPIC);
		Settable<String> subject = new Settable<String>();
		subject.set(NOTIFICATION_SUBJECT);

		Value<String> result5 = flow.dispatchNotifyFollower(result4, recipient,
				subject, rawDataMessage);

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
		Value<String> result6 = flow.dispatchProcessData(result5, script,
				datasetId, rawLayerId, 
//				machineName, localFilepath,
				processedLayerId, stdout, stderr);

		flow.dispatchNotifyDataProcessed(result6, processedLayerId);

	}

	@Asynchronous
	private Value<String> dispatchCreateMetadata(Value<String> param,
			Value<String> datasetId, Value<String> tcgaUrl,
			Settable<String> rawLayerId) throws Exception {
		return doCreateMetadata(param.get(), datasetId.get(), tcgaUrl.get(),
				rawLayerId);
	}

	@Activity(version = "1.2")
	private static Value<String> doCreateMetadata(String param,
			String datasetId, String tcgaUrl, Settable<String> rawLayerId)
			throws Exception {
		// Create a new layer, if necessary, in the synapse repository service
		// and return its id

		// This activity will be simple at first (just infer metadata from the
		// structure of the TCGA url) but it could become more complicated in
		// time if we need to pull additional metadata from other TCGA services
		String synapseLayerId = Curation
				.doCreateSynapseMetadataForTcgaSourceLayer(datasetId, tcgaUrl);
		rawLayerId.set(synapseLayerId);

		return Value.asValue(param + ":CreateMetadata");
	}

	@Asynchronous
	private Value<String> dispatchDownloadDataFromTcga(Value<String> param,
			Value<String> tcgaUrl, Settable<String> machineName,
			Settable<String> localFilepath, Settable<String> md5)
			throws UnrecoverableException, NoSuchAlgorithmException,
			HttpClientHelperException {
		return doDownloadDataFromTcga(param.get(), tcgaUrl.get(), machineName,
				localFilepath, md5);
	}

	@Activity(version = "1.2")
	@ExponentialRetry(minimumAttempts = 3, maximumAttempts = 5)
	@ActivityRegistrationOptions(defaultLifetimeTimeout = @Duration(time = 6, unit = DurationUnit.Hours), taskLivenessTimeout = @Duration(time = 6, unit = DurationUnit.Hours))
	@ActivitySchedulingOptions(lifetimeTimeout = @Duration(time = 6, unit = DurationUnit.Hours), queueTimeout = @Duration(time = 6, unit = DurationUnit.Hours))
	private static Value<String> doDownloadDataFromTcga(String param,
			String tcgaUrl, Settable<String> machineName,
			Settable<String> localFilepath, Settable<String> md5)
			throws UnrecoverableException, NoSuchAlgorithmException,
			HttpClientHelperException {

		try {
			DownloadResult result = DataIngestion.doDownloadFromTcga(tcgaUrl);
			localFilepath.set(result.getLocalFilepath());
			md5.set(result.getMd5());
			machineName.set(getHostName());
		} catch (IOException e) {
			// This particular exception is retryable
			throw new ActivityFailureException(500, "IOException", e);
		}
		return Value.asValue(param + ":DownloadDataFromTcga");
	}

	@Asynchronous
	private Value<String> dispatchUploadLayerToStorage(Value<String> param,
			String datasetId, Value<String> rawLayerId,
			Value<String> machineName, Value<String> localFilepath,
			Value<String> md5) throws Exception {
		return doUploadLayerToStorage(param.get(), datasetId, rawLayerId.get(),
				machineName.get(), localFilepath.get(), md5.get());
	}

	@Activity(version = "1.2")
	@ExponentialRetry(minimumAttempts = 3, maximumAttempts = 5)
	@ActivitySchedulingOptions(lifetimeTimeout = @Duration(time = 6, unit = DurationUnit.Hours), queueTimeout = @Duration(time = 6, unit = DurationUnit.Hours))
	@ActivityRegistrationOptions(defaultLifetimeTimeout = @Duration(time = 6, unit = DurationUnit.Hours), taskLivenessTimeout = @Duration(time = 6, unit = DurationUnit.Hours))
	private static Value<String> doUploadLayerToStorage(
			String param,
			String datasetId,
			String rawLayerId,
			@ActivitySchedulingOption(option = ActivitySchedulingElement.requirement) String machineName,
			String localFilepath, String md5) throws Exception {
		// - get the pre-signed S3 URL to which to upload the data from the
		// repository service
		// - upload the data to the pre-signed S3 URL
		// - set the S3 URL and md5 in the layer metadata

		Storage.doUploadLayerToStorage(datasetId, rawLayerId, localFilepath,
				md5);

		return Value.asValue(param + ":UploadLayerToS3");
	}

	@Asynchronous
	private Value<String> dispatchProcessData(Value<String> param,
			Value<String> script, String datasetId, Value<String> rawLayerId,
//			Value<String> machineName, Value<String> localFilepath,
			Settable<String> processedLayerId, Settable<String> stdout,
			Settable<String> stderr) throws Exception {

		return doProcessData(param.get(), script.get(), datasetId, rawLayerId
				.get(), 
//				machineName.get(), localFilepath.get(),
				processedLayerId, stdout, stderr);
	}

	@Activity(version = "1.2")
	// TODO ask SWF team why we cannot use a variable here
	// @ActivityRegistrationOptions(defaultLifetimeTimeout = @Duration(time =
	// MAX_SCRIPT_EXECUTION_HOURS_TIMEOUT, unit = DurationUnit.Hours),
	// taskLivenessTimeout = @Duration(time =
	// MAX_SCRIPT_EXECUTION_HOURS_TIMEOUT, unit = DurationUnit.Hours))
	@ActivityRegistrationOptions(defaultLifetimeTimeout = @Duration(time = 12, unit = DurationUnit.Hours), taskLivenessTimeout = @Duration(time = 12, unit = DurationUnit.Hours))
	@ActivitySchedulingOptions(lifetimeTimeout = @Duration(time = 12, unit = DurationUnit.Hours), queueTimeout = @Duration(time = 12, unit = DurationUnit.Hours))
	private static Value<String> doProcessData(
			String param,
			String script,
			String datasetId,
			String rawLayerId,
//			@ActivitySchedulingOption(option = ActivitySchedulingElement.requirement) String machineName,
//			String localFilepath, 
			Settable<String> processedLayerId,
			Settable<String> stdout, Settable<String> stderr) throws Exception {

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

		if (Constants.WORKFLOW_DONE != processedLayerId.get()) {
			/**
			 * Formulate the message to be sent to all interested parties about
			 * the new processed data from TCGA
			 */
			Settable<String> processedDataMessage = new Settable<String>();
			Value<String> result1 = dispatchFormulateNotificationMessage(param,
					processedLayerId, processedDataMessage);

			Settable<String> recipient = new Settable<String>();
			recipient.set(NOTIFICATION_SNS_TOPIC);
			Settable<String> subject = new Settable<String>();
			subject.set(NOTIFICATION_SUBJECT);

			/**
			 * Send the email notification to all interested parties, keeping
			 * this simple for now, later on we'll want to check their
			 * communication preferences and batch up these notifications as
			 * configured
			 */
			Value<String> result2 = dispatchNotifyFollower(result1, recipient,
					subject, processedDataMessage);

			return result2;
		}

		return Value.asValue(param + ":noop");
	}

	@Asynchronous
	private Value<String> dispatchFormulateNotificationMessage(
			Value<String> param, Value<String> layerId,
			Settable<String> message) throws Exception {
		return doFormulateNotificationMessage(param.get(), layerId.get(),
				message);
	}

	@Activity(version = "1.2")
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

	@Activity(version = "1.2")
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
