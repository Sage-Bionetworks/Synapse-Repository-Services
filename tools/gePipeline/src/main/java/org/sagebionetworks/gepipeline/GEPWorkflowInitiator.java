package org.sagebionetworks.gepipeline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ActivityAnnotationProcessor;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.AsyncWorkflowStartContext;

/**

 * 
 */
public class GEPWorkflowInitiator {

	private static final Logger log = Logger
			.getLogger(GEPWorkflowInitiator.class.getName());

	/**
	 * Crawl all top level TCGA datasets and identify the ones in which we are
	 * interested
	 */
	void initiateWorkflowTasks() throws Exception {
		// call R function which returns IDs of data sets to process
		List<String> scriptParams = new ArrayList<String>();
		String script = ConfigHelper.getGEPipelineCrawlerScript();
		if (script==null || script.length()==0) throw new RuntimeException("Missing crawler script parameter.");
		ScriptResult results = ScriptProcessor.doProcess(script, scriptParams);
		Map<String,String> idToDateMap = results.getStringMapResult(ScriptResult.OUTPUT_JSON_KEY);
		log.info("datasetIds to last-update-date map: "+idToDateMap);
		for (String datasetId:idToDateMap.keySet()) GEPWorkflow.doWorkflow(datasetId, idToDateMap.get(datasetId));

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
