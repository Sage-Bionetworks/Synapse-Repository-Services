package org.sagebionetworks.gepipeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;
import org.sagebionetworks.utils.HttpClientHelper;

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
	public static final String TARGET_PROJECT_ID_PROPERTY_NAME = "parentId";
	public static final String TARGET_DATASET_NAME_PROPERTY_NAME = "name";
	private static final String SOURCE_LAYER_PROPERTY_NAME = "layerName";
	private static final String NOTIFICATION_SUBJECT = "GEP Initiator Notification ";
	private static final String NUMBER_OF_SAMPLES_PROPERTY_NAME = "Number_of_Samples";
	private static final String REPOSITORY_PROPERTY_NAME = "repositoryName";
//	private static final String NCBI_REPOSITORY_NAME = "ncbi";
	

	/**
	 * Crawl all top level datasets and identify the ones in which we are
	 * interested
	 */
	void initiateWorkflowTasks() throws Exception {
		
		String projectId = ConfigHelper.getGEPipelineTargetProjectId();
		
		// Instead of 'crawling' GEO...
		// call R function which returns IDs of data sets to process
//		List<String> scriptParams = new ArrayList<String>(Arrays.asList(
//				new String[]{PROJECT_ID_PARAMETER_KEY, projectId
//						})
//		);
//		String script = ConfigHelper.getGEPipelineCrawlerScript();
//		if (script==null || script.length()==0) throw new RuntimeException("Missing crawler script parameter.");
//		ScriptResult results = ScriptProcessor.doProcess(script, scriptParams);
//		// the script returns a map whose keys are GSEIDs to run and values are the input data for each activity instance
//		Map<String,String> idToActivityInputMap = new HashMap<String,String>(results.getStringMapResult(ScriptResult.OUTPUT_JSON_KEY));

		// ... we 'crawl' another project
		String sourceProjectId = ConfigHelper.getGEPipelineSourceProjectId();
		Collection<Map<String,Object>> layerTasks = crawlSourceProject(sourceProjectId);
		
		
		String maxInstances = ConfigHelper.getGEPipelineMaxWorkflowInstances();
		int max = maxInstances==null ? -1 : Integer.parseInt(maxInstances); // set to -1 to disable
		int i = 0;
		System.out.println("Got "+layerTasks.size()+" datasets to run and will run "+
				(max==-1?"all":""+max)+" of them.");
		for (Map<String,Object>parameterMap: layerTasks) {
			if (max>0 && i++>=max) break; // for debugging, just launch a few...
			// the following must contain 'lastUpdate', 'url'
			// may also contain 'description', 'number_of_samples', 'status', 'createdBy', and/or other annotations
			JSONObject jsonParameters = new JSONObject(parameterMap);
			if (!jsonParameters.has(REPOSITORY_PROPERTY_NAME)) {
				jsonParameters.put(REPOSITORY_PROPERTY_NAME, "");
			}
			jsonParameters.put(TARGET_PROJECT_ID_PROPERTY_NAME, projectId);
			String parameterString =jsonParameters.toString();
			String datasetName = jsonParameters.getString(TARGET_DATASET_NAME_PROPERTY_NAME);
			if (!jsonParameters.has(NUMBER_OF_SAMPLES_PROPERTY_NAME)) throw new IllegalStateException("No "+NUMBER_OF_SAMPLES_PROPERTY_NAME+" for "+datasetName);
			int numSamples = (int)jsonParameters.getLong(NUMBER_OF_SAMPLES_PROPERTY_NAME);

			String activityRequirement = getActivityRequirementFromSampleCount(numSamples); 
			GEPWorkflow.doWorkflow(parameterString, activityRequirement);
		}
		
		String crawlerOutput = "foo"; //"Output from Crawler:\nStdout:\n"+results.getStdout()+"Stderr:\n"+results.getStderr();
		Notification.doSnsNotifyFollowers(GEPWorkflow.NOTIFICATION_SNS_TOPIC, 
				NOTIFICATION_SUBJECT + new LocalDate().toString(),
				crawlerOutput);		
	}
	
	// crawl the Synapse project given by sourceProjectId
	// return the layers to process, including the following attributes
	// 'lastUpdate', 'url', 'description', 'number_of_samples', 'status', 'createdBy'
	Collection<Map<String,Object>> crawlSourceProject(String sourceProjectId) {
		Collection<Map<String,Object>> layerTasks = new ArrayList<Map<String,Object>>();
		Synapse synapse = new Synapse();
		HttpClientHelper.setGlobalConnectionTimeout(DefaultHttpClientSingleton.getInstance(), 30000);
		HttpClientHelper.setGlobalSocketTimeout(DefaultHttpClientSingleton.getInstance(), 30000);
		synapse.setAuthEndpoint(ConfigHelper.getAuthenticationServicePublicEndpoint());
		String repositoryServiceEndpoint = ConfigHelper.getRepositoryServiceEndpoint();
		synapse.setRepositoryEndpoint(repositoryServiceEndpoint);

		String user = ConfigHelper.getSynapseUsername();
		String apiKey = ConfigHelper.getSynapseSecretKey();

		synapse.setUserName(user);
		synapse.setApiKey(apiKey);
		int offset=1;
		int total=0;
		int batchSize = 20;
		do {
			try {
				// get a batch of datasets
				JSONObject o = synapse.query("select * from dataset where parentId=="+sourceProjectId+" LIMIT "+batchSize+" OFFSET "+offset);
				total = (int)o.getLong("totalNumberOfResults");
				System.out.println(""+offset+"->"+(offset+batchSize-1)+" of "+total);
				JSONArray a = o.getJSONArray("results");
				for (int i=0; i<a.length(); i++) {
					JSONObject ds = (JSONObject)a.get(i);
					String id = ds.getString("dataset.id");
					String datasetName = ds.getString("dataset.name");
					String description = ds.getString("dataset.description");
					String lastUpdate = ds.getString("dataset.modifiedOn");
					long nos = ds.getLong("dataset.number_of_samples");
					String status = ds.getString("status");
					String createdBy = ds.getString("createdBy");
					// get the genomic and genetic layers
					JSONObject layers = synapse.query("select * from layer where parentId=="+id);
					JSONArray layersArray = layers.getJSONArray("results");
					for (int j=0; j<layersArray.length(); j++) {
						Map<String,Object> layerAttributes = new HashMap<String,Object>();
						layerTasks.add(layerAttributes);
						JSONObject layer = (JSONObject)layersArray.get(j);
						String layerId = layer.getString("layer.id");
						String location = null;//TODO
						layerAttributes.put(TARGET_DATASET_NAME_PROPERTY_NAME, datasetName);
						layerAttributes.put(REPOSITORY_PROPERTY_NAME, null);//TODO
						layerAttributes.put(TARGET_DATASET_NAME_PROPERTY_NAME, null);//TODO
						layerAttributes.put(NUMBER_OF_SAMPLES_PROPERTY_NAME, null);//TODO
						layerAttributes.put(SOURCE_LAYER_PROPERTY_NAME, null);//TODO
						layerAttributes.put(REPOSITORY_PROPERTY_NAME, null);//TODO
						layerAttributes.put("description", description);
						
					}
				}
			} catch (Exception e) {
				log.warn(e);
			}
			offset += batchSize;
		} while (offset<=total);
		return layerTasks;
	}
	
	// map sample count to SMALL, MEDIUM, LARGE
	// for Affy unsupervised QC we know the memory requirement is about 40MB per sample
	// we have Belltown=256GB, Sodo=64GB, Ballard=32GB
	// regular EC2: small=1.7GB, large=7.5GB, xl=15GB ($0.68/hr)
	// hi-mem EC2: xl=17GB ($0.50/hr), dxl=34GB, qxl=68GB
	//
	// One approach for large jobs is to reject anything larger than the capacity of the
	// largest machine.  But here we say that anything larger then the capacity of the 2nd largest
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
