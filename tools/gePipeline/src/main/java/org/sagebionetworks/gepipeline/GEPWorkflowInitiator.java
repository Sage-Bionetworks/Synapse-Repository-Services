package org.sagebionetworks.gepipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
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

	/**
	 * Crawl all top level TCGA datasets and identify the ones in which we are
	 * interested
	 */
	void initiateWorkflowTasks() throws Exception {
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
		log.info("datasetIds to input map: "+idToActivityInputMap);
		int max = -1; // set to -1 to disable
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
			GEPWorkflow.doWorkflow(datasetId, parameterString);
		}
	}

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
