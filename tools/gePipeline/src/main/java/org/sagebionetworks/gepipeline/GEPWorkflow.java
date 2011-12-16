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
	private static final String WORKFLOW_NAME = "MetaGenomics";
	private static final String VERSION = "1.2";
	private static final int MAX_WORKFLOW_TIMEOUT_HOURS = 24;
	private static final int MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS = 4;
	private static final int MAX_QUEUE_TIMEOUT_HOURS = 5*24; // five days
	private static final int NUM_ATTEMPTS = 1;
	
//	public static final String INPUT_DATASET_PARAMETER_KEY = "--datasetId";
	public static final String INPUT_DATA_PARAMETER_KEY = "--inputData";
	
	
	public static final String ACTIVITY_REQUIREMENT_SMALL = "small";
	public static final String ACTIVITY_REQUIREMENT_MEDIUM = "medium";
	public static final String ACTIVITY_REQUIREMENT_LARGE = "large";
	public static final String ACTIVITY_REQUIREMENT_EXTRA_LARGE = "extralarge";


	/**
	 * Other Constants (can be determined at runtime)
	 */
	// Even though we are sending stdout and stderr from the R script to a log
	// file, include a portion of that output in the workflow history for
	// convenience. We only want to dig through the logs if we need to.
	private static final int MAX_SCRIPT_OUTPUT = 10240;
	private static final String NOTIFICATION_SUBJECT = "GEP Workflow Notification ";
	public static final String NOTIFICATION_SNS_TOPIC = ConfigHelper
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
	@WorkflowRegistrationOptions(
			defaultWorkflowLifetimeTimeout = @Duration(time = MAX_WORKFLOW_TIMEOUT_HOURS, unit = DurationUnit.Hours)
	)
	public static void doWorkflow(String activityInput, String activityRequirement)
			throws Exception {

		GEPWorkflow flow = new GEPWorkflow();
		
		boolean noop = (NOOP!=null && NOOP.equalsIgnoreCase("true"));
		
		if (noop) {
			Value<String> result = Value.asValue("NO-OP for "+activityInput);
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
		Settable<String> stdout = new Settable<String>();
		Settable<String> stderr = new Settable<String>();
		// 'result' is the message returned by the workflow, 
		// other returned data are in 'processedLayerId', 'stdout', 'stderr'
		Value<String> result = flow.dispatchProcessData(script,
				activityInput, activityRequirement, stdout, stderr);

		if (hasChanged(result.get())) {
			flow.dispatchNotifyDataProcessed(result);
		}

	}
	
	// TODO use return code for this status rather than parsing message
	private static final String NO_CHANGE_MESSAGE = "has not changed since last update";
	
	private static boolean hasChanged(String msg) {return msg.indexOf(NO_CHANGE_MESSAGE)<0;}

	// different datasets require different size machines.  The capacity requirement is 
	// passed in as 'activityRequirement'
	@Asynchronous
	private Value<String> dispatchProcessData(
			Value<String> script, String activityInput, String activityRequirement,
			Settable<String> stdout,
			Settable<String> stderr) throws Exception {

		if (ACTIVITY_REQUIREMENT_SMALL.equals(activityRequirement)) {
			return doProcessDataSmall(script.get(), activityInput, stdout, stderr);
		} else if (ACTIVITY_REQUIREMENT_MEDIUM.equals(activityRequirement)) {
			return doProcessDataMedium(script.get(), activityInput, stdout, stderr);
		} else if (ACTIVITY_REQUIREMENT_LARGE.equals(activityRequirement)) {
			return doProcessDataLarge(script.get(), activityInput, stdout, stderr);
		} else if (ACTIVITY_REQUIREMENT_EXTRA_LARGE.equals(activityRequirement)) {
			return doProcessDataExtraLarge(script.get(), activityInput, stdout, stderr);
		} else {
			throw new IllegalArgumentException("Unexpected "+activityRequirement);
		}
	}
	
	@Activity(version = VERSION)
	@ExponentialRetry(minimumAttempts = NUM_ATTEMPTS, maximumAttempts = NUM_ATTEMPTS)
	@ActivityRegistrationOptions(
			defaultLifetimeTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours), 
			taskLivenessTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours))
	@ActivitySchedulingOptions(
			lifetimeTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours), 
			queueTimeout = @Duration(time = MAX_QUEUE_TIMEOUT_HOURS, unit = DurationUnit.Hours), requirement=ACTIVITY_REQUIREMENT_SMALL)
	private static Value<String> doProcessDataSmall(String script, 
			String activityInput,
			Settable<String> stdout,
			Settable<String> stderr) throws Exception {

		ScriptResult result = ScriptProcessor.doProcess(script,
				Arrays.asList(new String[]{INPUT_DATA_PARAMETER_KEY, formatAsScriptParam(activityInput),
						}));
		stdout.set((MAX_SCRIPT_OUTPUT > result.getStdout().length()) ? result
				.getStdout() : result.getStdout().substring(0,
				MAX_SCRIPT_OUTPUT));
		stderr.set((MAX_SCRIPT_OUTPUT > result.getStderr().length()) ? result
				.getStderr() : result.getStderr().substring(0,
				MAX_SCRIPT_OUTPUT));
		return Value.asValue(result.getStringResult(ScriptResult.OUTPUT_JSON_KEY));
	}
	
	@Activity(version = VERSION)
	@ExponentialRetry(minimumAttempts = NUM_ATTEMPTS, maximumAttempts = NUM_ATTEMPTS)
	@ActivityRegistrationOptions(
			defaultLifetimeTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours), 
			taskLivenessTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours))
	@ActivitySchedulingOptions(
			lifetimeTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours), 
			queueTimeout = @Duration(time = MAX_QUEUE_TIMEOUT_HOURS, unit = DurationUnit.Hours), requirement=ACTIVITY_REQUIREMENT_MEDIUM)
	private static Value<String> doProcessDataMedium(String script, 
			String activityInput,
			Settable<String> stdout,
			Settable<String> stderr) throws Exception {

		ScriptResult result = ScriptProcessor.doProcess(script,
				Arrays.asList(new String[]{
						INPUT_DATA_PARAMETER_KEY, formatAsScriptParam(activityInput),
						}));
		stdout.set((MAX_SCRIPT_OUTPUT > result.getStdout().length()) ? result
				.getStdout() : result.getStdout().substring(0,
				MAX_SCRIPT_OUTPUT));
		stderr.set((MAX_SCRIPT_OUTPUT > result.getStderr().length()) ? result
				.getStderr() : result.getStderr().substring(0,
				MAX_SCRIPT_OUTPUT));
		return Value.asValue(result.getStringResult(ScriptResult.OUTPUT_JSON_KEY));
	}
	
	@Activity(version = VERSION)
	@ExponentialRetry(minimumAttempts = NUM_ATTEMPTS, maximumAttempts = NUM_ATTEMPTS)
	@ActivityRegistrationOptions(
			defaultLifetimeTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours), 
			taskLivenessTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours))
	@ActivitySchedulingOptions(
			lifetimeTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours), 
			queueTimeout = @Duration(time = MAX_QUEUE_TIMEOUT_HOURS, unit = DurationUnit.Hours), requirement=ACTIVITY_REQUIREMENT_LARGE)
	private static Value<String> doProcessDataLarge(String script, 
			String activityInput,
			Settable<String> stdout,
			Settable<String> stderr) throws Exception {

		ScriptResult result = ScriptProcessor.doProcess(script,
				Arrays.asList(new String[]{
						INPUT_DATA_PARAMETER_KEY, formatAsScriptParam(activityInput),
						}));
		stdout.set((MAX_SCRIPT_OUTPUT > result.getStdout().length()) ? result
				.getStdout() : result.getStdout().substring(0,
				MAX_SCRIPT_OUTPUT));
		stderr.set((MAX_SCRIPT_OUTPUT > result.getStderr().length()) ? result
				.getStderr() : result.getStderr().substring(0,
				MAX_SCRIPT_OUTPUT));
		return Value.asValue(result.getStringResult(ScriptResult.OUTPUT_JSON_KEY));
	}
	
	@Activity(version = VERSION)
	@ExponentialRetry(minimumAttempts = NUM_ATTEMPTS, maximumAttempts = NUM_ATTEMPTS)
	@ActivityRegistrationOptions(
			defaultLifetimeTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours), 
			taskLivenessTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours))
	@ActivitySchedulingOptions(
			lifetimeTimeout = @Duration(time = MAX_SCRIPT_EXECUTION_TIMEOUT_HOURS, unit = DurationUnit.Hours), 
			queueTimeout = @Duration(time = MAX_QUEUE_TIMEOUT_HOURS, unit = DurationUnit.Hours), requirement=ACTIVITY_REQUIREMENT_EXTRA_LARGE)
	private static Value<String> doProcessDataExtraLarge(String script, 
			String activityInput,
			Settable<String> stdout,
			Settable<String> stderr) throws Exception {

		ScriptResult result = ScriptProcessor.doProcess(script,
				Arrays.asList(new String[]{INPUT_DATA_PARAMETER_KEY, formatAsScriptParam(activityInput),
						}));
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
