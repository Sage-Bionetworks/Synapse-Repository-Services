package org.sagebionetworks.gepipeline;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.joda.time.LocalDate;

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

 * 
 */
public class GEPWorkflow {

	/**
	 * Workflow and Activity Annotations Constants
	 * 
	 * Dev Note: annotation values must be resolvable at compilation time, not
	 * runtime, therefore we cannot move this into a config file
	 */
	private static final String WORKFLOW_NAME = "GE Pipeline Workflow";
	private static final String VERSION = "1.1";
	private static final int MAX_WORKFLOW_TIMEOUT_HOURS = 24;
	private static final int MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS = 4;
	private static final int NUM_ATTEMPTS = 1;
	
	public static final String INPUT_DATASET_PARAMETER_KEY = "--datasetId";
	public static final String INPUT_DATA_PARAMETER_KEY = "--inputData";

	/**
	 * Other Constants (can be determined at runtime)
	 */
	// Even though we are sending stdout and stderr from the R script to a log
	// file, include a portion of that output in the workflow history for
	// convenience. We only want to dig through the logs if we need to.
	private static final int MAX_SCRIPT_OUTPUT = 10240;
	private static final String NOTIFICATION_SUBJECT = "GEP Workflow Notification ";
	private static final String NOTIFICATION_SNS_TOPIC = ConfigHelper
			.getWorkflowSnsTopic();
	
	// for testing, can set the workflow to 'no-op', i.e. just close out the instance
	// without doing anything
	private static final String NOOP = ConfigHelper.getGEPipelineNoop();
	
	/**
	 * @param param
	 * @param datasetId
	 * @param gepUrl
	 * @throws Exception
	 */
	@Workflow(name = WORKFLOW_NAME, version = VERSION)
	@WorkflowRegistrationOptions(defaultWorkflowLifetimeTimeout = @Duration(time = MAX_WORKFLOW_TIMEOUT_HOURS, unit = DurationUnit.Hours))
	public static void doWorkflow(String datasetId, String activityInput)
			throws Exception {

		GEPWorkflow flow = new GEPWorkflow();
		
		boolean noop = (NOOP!=null && NOOP.equalsIgnoreCase("true"));
		
		if (noop) {
			Value<String> result = Value.asValue("NO-OP for "+datasetId);
			flow.dispatchNotifyDataProcessed(result);
			return;
		}

		Settable<String> script = new Settable<String>();
		String workflowScript = ConfigHelper.getGEPipelineWorkflowScript();
		if (workflowScript==null || workflowScript.length()==0) throw new RuntimeException("No workflow script param");
		script.set(workflowScript);

		/**
		 * Run the processing step(s) on this data
		 */
		Settable<String> processedLayerId = new Settable<String>();
		Settable<String> stdout = new Settable<String>();
		Settable<String> stderr = new Settable<String>();
		// 'result' is the message returned by the workflow, 
		// other returned data are in 'processedLayerId', 'stdout', 'stderr'
		Value<String> result = flow.dispatchProcessData(script,
				datasetId, activityInput, processedLayerId, stdout, stderr);

		flow.dispatchNotifyDataProcessed(result);

	}

	@Asynchronous
	private Value<String> dispatchProcessData(
			Value<String> script, String datasetId, String activityInput, 
			Settable<String> processedLayerId, Settable<String> stdout,
			Settable<String> stderr) throws Exception {

//		if (Constants.WORKFLOW_DONE.equals(rawLayerId.get())) {
//			return Value.asValue(param + ":noop");
//		}
		return doProcessData(script.get(), datasetId, activityInput,
				processedLayerId, stdout, stderr);
	}

	@Activity(version = VERSION)
	@ExponentialRetry(minimumAttempts = NUM_ATTEMPTS, maximumAttempts = NUM_ATTEMPTS)
	@ActivityRegistrationOptions(
			defaultLifetimeTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours), 
			taskLivenessTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours))
	@ActivitySchedulingOptions(
			lifetimeTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours), 
			queueTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours))
	private static Value<String> doProcessData(String script, 
			String datasetId,
			String activityInput,
			Settable<String> processedLayerId, Settable<String> stdout,
			Settable<String> stderr) throws Exception {

		ScriptResult result = ScriptProcessor.doProcess(script,
				Arrays.asList(new String[]{
						INPUT_DATASET_PARAMETER_KEY, datasetId, 
						INPUT_DATA_PARAMETER_KEY, formatAsScriptParam(activityInput),
						}));
		processedLayerId.set("layer id goes here");
		stdout.set((MAX_SCRIPT_OUTPUT > result.getStdout().length()) ? result
				.getStdout() : result.getStdout().substring(0,
				MAX_SCRIPT_OUTPUT));
		stderr.set((MAX_SCRIPT_OUTPUT > result.getStderr().length()) ? result
				.getStderr() : result.getStderr().substring(0,
				MAX_SCRIPT_OUTPUT));
		return Value.asValue(result.getStringResult(ScriptResult.OUTPUT_JSON_KEY));
	}
	
	private static final String DOUBLE_QUOTE = "\"";
	// the following translates to \\\" in the string passed to DOS, which becomes \", i.e. an escaped quote. whew!
	private static final String ESCAPED_DOUBLE_QUOTE = "\\\\\\"+DOUBLE_QUOTE;
	// escape all double quotes, then surround by double quotes
	public static String formatAsScriptParam(String s) {
		// doesn't work!
		// return DOUBLE_QUOTE+s.replaceAll(DOUBLE_QUOTE, ESCAPED_DOUBLE_QUOTE)+DOUBLE_QUOTE;
		// so let's try URLEncoding the param.  This means we must URLDecode on the R side
		try {
			// R's URLdecode expects %20 for space, not +
			return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}


	@Asynchronous
	private Value<String> dispatchNotifyDataProcessed(Value<String> param) throws Exception {
		/**
		 * Formulate the message to be sent to all interested parties about the
		 * new processed data from TCGA
		 */
		Settable<String> processedDataMessage = new Settable<String>();
		Value<String> result1 = dispatchFormulateNotificationMessage(param,
				processedDataMessage);

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

	// note, the output is in 'message'
	@Asynchronous
	private Value<String> dispatchFormulateNotificationMessage(
			Value<String> param, Settable<String> message)
			throws Exception {
		return doFormulateNotificationMessage(param.get(), 
				message);
	}

	@Activity(version = VERSION)
	private static Value<String> doFormulateNotificationMessage(String param,
			Settable<String> message) throws Exception {
		message.set(param);
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
