package org.sagebionetworks.gepipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.LocalDate;
import org.json.JSONObject;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ActivityAnnotationProcessor;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.AsyncWorkflowStartContext;

/**

 * 
 */
public class GEPWorkflowInitiator {

	private static final Logger log = Logger
			.getLogger(GEPWorkflowInitiator.class.getName());
	
	public static final String MAX_DATASET_SIZE_PARAMETER_KEY = "--maxDatasetSize";
	public static final String NAME_PROPERTY_NAME = "name";
	public static final String PARENT_ID_PROPERTY_NAME = "parentId";
	private static final String NOTIFICATION_SUBJECT = "GEP Initiator Notification ";
	private static final String NUMBER_OF_SAMPLES_PROPERTY_NAME = "Number_of_Samples";

	/**
	 * Crawl all top level datasets and identify the ones in which we are
	 * interested
	 */
	void initiateWorkflowTasks() throws Exception {
		String maxInstances = ConfigHelper.getGEPipelineMaxWorkflowInstances();
		// call R function which returns IDs of data sets to process
		String maxDatasetSize = ConfigHelper.getGEPipelineMaxDatasetSize();
		List<String> scriptParams = new ArrayList<String>(Arrays.asList(
				new String[]{MAX_DATASET_SIZE_PARAMETER_KEY, maxDatasetSize})
		);
		String script = ConfigHelper.getGEPipelineCrawlerScript();
		if (script==null || script.length()==0) throw new RuntimeException("Missing crawler script parameter.");
		ScriptResult results = ScriptProcessor.doProcess(script, scriptParams);
//		System.out.println("results stdout:\n"+results.getStdout().toString()+"\n\n");
//		System.out.println("results stderr:\n"+results.getStderr().toString()+"\n\n");
		// the script returns a map whose keys are GSEIDs to run and values are the input data for each activity instance
		Map<String,String> idToActivityInputMap = new HashMap<String,String>(results.getStringMapResult(ScriptResult.OUTPUT_JSON_KEY));
//		log.info("datasetIds to input map: "+idToActivityInputMap);
		int max = maxInstances==null ? -1 : Integer.parseInt(maxInstances); // set to -1 to disable
		int i = 0;
		ObjectMapper mapper = new ObjectMapper();
		String projectId = ConfigHelper.getGEPipelineProjectId();
		for (String datasetId:idToActivityInputMap.keySet()) {
			if (max>0 && i++>=max) break; // for debugging, just launch a few...
			String parameterString = idToActivityInputMap.get(datasetId);
			JSONObject jsonParameters = new JSONObject(parameterString);
			//Map<String,String> parameters = jsonParameters.
			jsonParameters.put(PARENT_ID_PROPERTY_NAME, ConfigHelper.getGEPipelineProjectId());
			jsonParameters.put(NAME_PROPERTY_NAME, datasetId);
			parameterString =jsonParameters.toString();
			if (!jsonParameters.has(NUMBER_OF_SAMPLES_PROPERTY_NAME)) throw new IllegalStateException("No "+NUMBER_OF_SAMPLES_PROPERTY_NAME+" for "+datasetId);
			int numSamples = (int)jsonParameters.getLong(NUMBER_OF_SAMPLES_PROPERTY_NAME);

			String activityRequirement = getActivityRequirementFromSampleCount(numSamples); 
			GEPWorkflow.doWorkflow(datasetId, parameterString, activityRequirement);
		}
		
		String crawlerOutput = "Output from Crawler:\nStdout:\n"+results.getStdout()+"Stderr:\n"+results.getStderr();
		Notification.doSnsNotifyFollowers(GEPWorkflow.NOTIFICATION_SNS_TOPIC, 
				NOTIFICATION_SUBJECT + new LocalDate().toString(),
				crawlerOutput);		
	}
	
	// TODO map sample count to SMALL, MEDIUM, LARGE
	// for Affy unsupervised QC we know the memory requirement is about 40MB per sample
	// we have Belltown=256GB, Sodo=64GB, Ballard=32GB
	// regular EC2: small=1.7GB, large=7.5GB, xl=15GB ($0.68/hr)
	// hi-mem EC2: xl=17GB ($0.50/hr), dxl=34GB, qxl=68GB
	//
	// One approach for large jobs is to reject anything larger than the capacity of the
	// largest machine.  Here we say that anything larger then the capacity of the 2nd largest
	// machine is sent to the largest, which can 'take a crack at it.'
	//
	public static String getActivityRequirementFromSampleCount(int numSamples) {
		// compute the memory needed for this number of samples, rounded up to the nearest GB
		int gigaBytesRequired = (int)Math.ceil((float)numSamples * (float)MEAGABYTES_PER_SAMPLE/1000F);
		if (gigaBytesRequired<=SMALL_CAPACITY_GB) {
			return GEPWorkflow.ACTIVITY_REQUIREMENT_SMALL;
		} else if (gigaBytesRequired<=MEDIUM_CAPACITY_GB) {
			return GEPWorkflow.ACTIVITY_REQUIREMENT_MEDIUM;
		} else if (gigaBytesRequired<=LARGE_CAPACITY_GB) {
			return GEPWorkflow.ACTIVITY_REQUIREMENT_LARGE;
		} else {
			return GEPWorkflow.ACTIVITY_REQUIREMENT_EXTRA_LARGE;
		}
	}
	
	private static final int MEAGABYTES_PER_SAMPLE = 40;
	private static final int SMALL_CAPACITY_GB = ConfigHelper.getGEPipelineSmallCapacityGB();
	private static final int MEDIUM_CAPACITY_GB = ConfigHelper.getGEPipelineMediumCapacityGB();
	private static final int LARGE_CAPACITY_GB = ConfigHelper.getGEPipelineLargeCapacityGB();


	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// Running Annotation Processor is very important, otherwise it will
		// treat Workflows and Activities as regular java method calls
		ActivityAnnotationProcessor.processAnnotations();

		// Create the client for Simple Workflow Service
		AmazonSimpleWorkflow swfService = ConfigHelper.createSWFClient();
		AsyncWorkflowStartContext.initialize(swfService);

		GEPWorkflowInitiator initiator = new GEPWorkflowInitiator();
		initiator.initiateWorkflowTasks();
		
		System.exit(0);
	}

}
