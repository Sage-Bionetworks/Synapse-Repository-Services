package org.sagebionetworks.workflow.curation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.amazonaws.services.simpleworkflow.client.asynchrony.Settable;
import com.amazonaws.services.simpleworkflow.client.asynchrony.Value;
import com.amazonaws.services.simpleworkflow.client.asynchrony.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.Activity;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ActivityRegistrationOptions;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ActivitySchedulingElement;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ActivitySchedulingOption;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.Duration;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.DurationUnit;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ExponentialRetry;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.Workflow;

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
 * - If we want to re-run the R scripts processing activity, do we have to write
 * a different workflow containing just those activities? (e.g., if we had a bug
 * in one of our scientific methods and want to rerun that method on all TCGA
 * data) Seems like "yes". We would write a workflow that gathers all the layer
 * ids for source data from TCGA and re-runs a particular script upon them.
 * 
 * @author deflaux
 * 
 */
public class TcgaWorkflow {

	// Even though we are sending stdout and stderr from the R script to a log
	// file, include a portion of that output in the workflow history for
	// convenience. We only want to dig through the logs if we need to.
	private static final int MAX_SCRIPT_OUTPUT = 1024;

	/**
	 * @param param
	 * @param datasetId
	 * @param tcgaUrl
	 * @throws Exception
	 */
	@Workflow(name = "TcgaWorkflow")
	public static void doWorkflow(String param, Integer datasetId,
			String tcgaUrl) throws Exception {

		TcgaWorkflow flow = new TcgaWorkflow();

		/**
		 * Create a metadata entity in the repository service for the raw TGCA
		 * data layer
		 */
		Settable<Integer> rawLayerId = new Settable<Integer>();
		Value<String> result1 = flow.dispatchCreateMetadata(Value
				.asValue(param), Value.asValue(datasetId), Value
				.asValue(tcgaUrl), rawLayerId);

		/**
		 * Download the raw data from TCGA
		 */
		Settable<String> machineName = new Settable<String>();
		Settable<String> localFilepath = new Settable<String>();
		Settable<String> md5 = new Settable<String>();
		Value<String> result2 = flow.dispatchDownloadDataFromTcga(result1,
				Value.asValue(tcgaUrl), machineName, localFilepath, md5);

		/**
		 * Upload the data to S3
		 */
		Value<String> result3 = flow.dispatchUploadDataToS3(result2,
				rawLayerId, machineName, localFilepath, md5);

		/**
		 * Formulate the message to be sent to all interested parties about the
		 * new raw data from TCGA
		 */
		Settable<String> rawDataMessage = new Settable<String>();
		Value<String> result4 = flow.dispatchFormulateNotificationMessage(
				result3, rawLayerId, rawDataMessage);

		/**
		 * Send the email notification to all interested parties, keeping this
		 * simple for now, later on we'll want to check their communication
		 * preferences and batch up these notifications as configured
		 */
		Value<String> result5 = flow.dispatchNotifyFollowers(result4,
				rawLayerId, rawDataMessage);

		/**
		 * Dynamically discover the R scripts to run on this data
		 */
		// TODO add an activity step to look for scripts in a known location
		// where they were dropped off by our scientists
		// and kick off one processData task per script in parallel
		Settable<String> script = new Settable<String>();
		script
				.set("/Users/deflaux/platform/deflaux/scripts/stdoutKeepAlive.sh");

		// TODO once we have that list of scripts, split the workflow here into
		// parallel tasks for the remainder of this pipeline,
		// we don't need a join, but perhaps an accumulation of notifications
		// when it gets spammy

		/**
		 * Run the processing step(s) on this data
		 */
		Settable<Integer> processedLayerId = new Settable<Integer>();
		Settable<String> stdout = new Settable<String>();
		Settable<String> stderr = new Settable<String>();
		Value<String> result6 = flow.dispatchProcessData(result5, script,
				rawLayerId, machineName, localFilepath, processedLayerId,
				stdout, stderr);

		// TODO this is not the correct way to branch a workflow, do I have the
		// activity call a subworkflow instead?
		// if (-1 < processedLayerId.get()) {
		/**
		 * Formulate the message to be sent to all interested parties about the
		 * new processed data from TCGA
		 */
		Settable<String> processedDataMessage = new Settable<String>();
		Value<String> result7 = flow.dispatchFormulateNotificationMessage(
				result6, processedLayerId, processedDataMessage);

		/**
		 * Send the email notification to all interested parties, keeping this
		 * simple for now, later on we'll want to check their communication
		 * preferences and batch up these notifications as configured
		 */
		flow.dispatchNotifyFollowers(result7, processedLayerId,
				processedDataMessage);
		// }
	}

	@Asynchronous
	private Value<String> dispatchCreateMetadata(Value<String> param,
			Value<Integer> datasetId, Value<String> tcgaUrl,
			Settable<Integer> rawLayerId) {
		return doCreateMetadata(param.get(), datasetId.get(), tcgaUrl.get(),
				rawLayerId);
	}

	@Activity
	private static Value<String> doCreateMetadata(String param,
			Integer datasetId, String tcgaUrl, Settable<Integer> rawLayerId) {
		// create a new layer, if necessary, in the synapse repository service
		// and return its id

		// this activity will be simple at first (just infer metadata from the
		// structure of the TCGA url) but it could become more complicated in
		// time if we need to pull additional metadata from other TCGA services
		rawLayerId.set(23);
		return Value.asValue(param + ":CreateMetadata");
	}

	@Asynchronous
	private Value<String> dispatchDownloadDataFromTcga(Value<String> param,
			Value<String> tcgaUrl, Settable<String> machineName,
			Settable<String> localFilepath, Settable<String> md5) {
		return doDownloadDataFromTcga(param.get(), tcgaUrl.get(), machineName,
				localFilepath, md5);
	}

	@Activity
	@ExponentialRetry(minimumAttempts = 5, maximumAttempts = 10)
	private static Value<String> doDownloadDataFromTcga(String param,
			String tcgaUrl, Settable<String> machineName,
			Settable<String> localFilepath, Settable<String> md5) {
		localFilepath.set("/dev/null");
		md5.set("d131dd02c5e6eec4693d9a0698aff95c");
		machineName.set(getHostName());
		return Value.asValue(param + ":DownloadDataFromTcga");
	}

	@Asynchronous
	private Value<String> dispatchUploadDataToS3(Value<String> param,
			Value<Integer> rawLayerId, Value<String> machineName,
			Value<String> localFilepath, Value<String> md5) {
		return doUploadDataToS3(param.get(), rawLayerId.get(), machineName
				.get(), localFilepath.get(), md5.get());
	}

	@Activity
	@ExponentialRetry(minimumAttempts = 5, maximumAttempts = 10)
	private static Value<String> doUploadDataToS3(
			String param,
			Integer rawLayerId,
			@ActivitySchedulingOption(option = ActivitySchedulingElement.requirement) String machineName,
			String localFilepath, String md5) {
		// - get the pre-signed S3 URL to which to upload the data from the
		// repository service
		// - upload the data to the pre-signed S3 URL
		// - set the S3 URL and md5 in the layer metadata
		return Value.asValue(param + ":UploadDataToS3");
	}

	@Asynchronous
	private Value<String> dispatchProcessData(Value<String> param,
			Value<String> script, Value<Integer> rawLayerId,
			Value<String> machineName, Value<String> localFilepath,
			Settable<Integer> processedLayerId, Settable<String> stdout,
			Settable<String> stderr) throws Exception {
		return doProcessData(param.get(), script.get(), rawLayerId.get(),
				machineName.get(), localFilepath.get(), processedLayerId,
				stdout, stderr);
	}

	@Activity
	@ActivityRegistrationOptions(defaultLifetimeTimeout = @Duration(time = 30, unit = DurationUnit.Minutes))
	// 30 minute lifetime timeout right now will be okay, later
	// defaultLifetimeTimeout = 360, taskLivenessTimeout=10 with a heartbeat
	// thread
	// @ActivityExecutionOptions(external=true)
	// "compile error: @ExternalActivity method body must contain single throw statement to indicate that implementation is elsewhere"
	private static Value<String> doProcessData(
			String param,
			String script,
			Integer rawLayerId,
			@ActivitySchedulingOption(option = ActivitySchedulingElement.requirement) String machineName,
			String localFilepath, Settable<Integer> processedLayerId,
			Settable<String> stdout, Settable<String> stderr) throws Exception {

		// When these R scripts are run via this workflow, a value will be
		// passed in for --localFilepath, the script will get the layer metadata
		// from the repository service which includes the md5 checksum and it
		// will confirm those match before proceeding.
		//
		// When these R scripts are run by hand via scientists, the lack of
		// localFilepath will cause the file to be downloaded from S3url in the
		// layer metadata and stored in the local R file cache. The scientists
		// will work this way when developing new scripts or modifying existing
		// scripts.
		Process process = Runtime.getRuntime().exec(
				new String[] { script, "--rawLayerId", rawLayerId.toString(),
						"--localFilepath", localFilepath });

		// TODO heartbeat thread and threads for slurping in stdout and
		// stderr

		String line;

		// Collect stdout from the script
		BufferedReader inputStream = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
		StringBuilder stdoutAccumulator = new StringBuilder();
		while ((line = inputStream.readLine()) != null) {
			stdoutAccumulator.append(line);
			// TODO log this
			System.out.println(line);
		}
		inputStream.close();
		stdout
				.set((MAX_SCRIPT_OUTPUT > stdoutAccumulator.length()) ? stdoutAccumulator
						.toString()
						: stdoutAccumulator.substring(0, MAX_SCRIPT_OUTPUT));

		// Collect stderr from the script
		BufferedReader errorStream = new BufferedReader(new InputStreamReader(
				process.getErrorStream()));
		StringBuilder stderrAccumulator = new StringBuilder();
		while ((line = errorStream.readLine()) != null) {
			stderrAccumulator.append(line);
			// TODO log this
			System.out.println(line);
		}
		errorStream.close();
		stderr
				.set((MAX_SCRIPT_OUTPUT > stderrAccumulator.length()) ? stderrAccumulator
						.toString()
						: stderrAccumulator.substring(0, MAX_SCRIPT_OUTPUT));

		int returnCode = process.waitFor();
		if (0 != returnCode) {
			throw new Exception("activity failed(" + returnCode + ": "
					+ stderr.get());
		}

		// TODO parse JSON output from R script sent to stdout to get the
		// processed layer id
		processedLayerId.set(42);
		return Value.asValue(param + ":ProcessData");
	}

	@Asynchronous
	private Value<String> dispatchFormulateNotificationMessage(
			Value<String> param, Value<Integer> layerId,
			Settable<String> message) {
		return doFormulateNotificationMessage(param.get(), layerId.get(),
				message);
	}

	@Activity
	private static Value<String> doFormulateNotificationMessage(String param,
			Integer layerId, Settable<String> message) {
		message.set("some data has been processed");
		return Value.asValue(param + ":FormulateNotificationMessage");
	}

	@Asynchronous
	private Value<String> dispatchNotifyFollowers(Value<String> param,
			Value<Integer> layerId, Value<String> message) {
		return doNotifyFollowers(param.get(), layerId.get(), message.get());
	}

	@Activity
	private static Value<String> doNotifyFollowers(String param,
			Integer layerId, String message) {
		return Value.asValue(param + ":NotifyFollowers");
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
