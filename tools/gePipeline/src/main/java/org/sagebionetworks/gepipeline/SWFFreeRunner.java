package org.sagebionetworks.gepipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;

public class SWFFreeRunner {
	private static final Logger log = Logger
	.getLogger(SWFFreeRunner.class.getName());


	public static void main(String[] args) throws Exception {
		// call R function which returns IDs of data sets to process
//		String maxDatasetSize = ConfigHelper.getGEPipelineMaxDatasetSize();
		List<String> scriptParams = new ArrayList<String>(Arrays.asList(
				new String[]{/*GEPWorkflowInitiator.MAX_DATASET_SIZE_PARAMETER_KEY, maxDatasetSize*/}
		));
		String crawlerScript = GEPWorkflowConfigHelper.getGEPipelineCrawlerScript();
		if (crawlerScript==null || crawlerScript.length()==0) throw new RuntimeException("Missing crawler script parameter.");
		ScriptResult results = ScriptProcessor.doProcess(crawlerScript, scriptParams);
		Map<String,String> idToActivityInputMap = new HashMap<String,String>(results.getStringMapResult(ScriptResult.OUTPUT_JSON_KEY));
		log.info("datasetIds: "+idToActivityInputMap.keySet());
		int max = -1; // set to -1 to disable
		int i = 0;
		ObjectMapper mapper = new ObjectMapper();
		String projectId = GEPWorkflowConfigHelper.getGEPipelineTargetProjectId();
		for (String datasetId:idToActivityInputMap.keySet()) {
			if (max>0 && i++>=max) break; // for debugging, just launch a few...
			String parameterString = idToActivityInputMap.get(datasetId);
			JSONObject jsonParameters = new JSONObject(parameterString);
			jsonParameters.put(GEPWorkflowInitiator.TARGET_PROJECT_ID_PROPERTY_NAME, GEPWorkflowConfigHelper.getGEPipelineTargetProjectId());
			jsonParameters.put(GEPWorkflowInitiator.TARGET_DATASET_NAME_PROPERTY_NAME, datasetId);
			parameterString =jsonParameters.toString();
			
			String workflowScript = GEPWorkflowConfigHelper.getGEPipelineWorkflowScript();
			if (workflowScript==null || workflowScript.length()==0) throw new RuntimeException("No workflow script param");
			try {
			    ScriptResult result = ScriptProcessor.doProcess(workflowScript,
					Arrays.asList(new String[]{
							GEPWorkflow.INPUT_DATA_PARAMETER_KEY, GEPActivitiesImpl.formatAsScriptParam(parameterString),
							}));
			} catch (Exception e) {
				log.error(e);
			}
		}
	}
}
