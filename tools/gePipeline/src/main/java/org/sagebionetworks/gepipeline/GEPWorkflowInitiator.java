package org.sagebionetworks.gepipeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
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
//	private static final String TARGET_LAYER_NAME_PROPERTY_NAME = "layerName";
	private static final String NOTIFICATION_SUBJECT = "GEP Initiator Notification ";
	private static final String NUMBER_OF_SAMPLES_PROPERTY_NAME = "Number_of_Samples";
//	private static final String LAST_UPDATE_PROPERTY_NAME = "lastUpdate";
	private static final String SOURCE_LAYER_ID_PROPERTY_NAME = "sourceLayerId";
	

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
		Synapse synapse = connectToSynapse();
		Collection<Map<String,Object>> layerTasks = crawlSourceProject(synapse);
		
		
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
			jsonParameters.put(TARGET_PROJECT_ID_PROPERTY_NAME, projectId);
			String parameterString =jsonParameters.toString();
			String activityRequirement = GEPWorkflow.ACTIVITY_REQUIREMENT_LARGE;
			if (jsonParameters.has(NUMBER_OF_SAMPLES_PROPERTY_NAME)) {
				int numSamples = (int)jsonParameters.getLong(NUMBER_OF_SAMPLES_PROPERTY_NAME);
				activityRequirement = getActivityRequirementFromSampleCount(numSamples); 
			}
			GEPWorkflow.doWorkflow(parameterString, activityRequirement);
		}
		
		String crawlerOutput = "Initiated "+layerTasks.size()+" MetaGenomics workflows."; //"Output from Crawler:\nStdout:\n"+results.getStdout()+"Stderr:\n"+results.getStderr();
		Notification.doSnsNotifyFollowers(GEPWorkflow.NOTIFICATION_SNS_TOPIC, 
				NOTIFICATION_SUBJECT + new LocalDate().toString(),
				crawlerOutput);		
	}
	
//	private Map<String, String> getLastUpdate(Synapse synapse, int datasetId) {
//		try {
//			JSONObject proj = synapse.getEntity("/dataset/"+datasetId);
//			
//		} catch (SynapseException e) {
//			throw new RuntimeException(e);
//		}
//	}
//	
	private Synapse connectToSynapse() {
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
		return synapse;
	}
	
	// determine whether an update is needed to the QCed data for the given source layer
	// first check the MD5 checksums. If that doesn't answer the question then check the 'modifiedOn' timestamp:
	//
	// if the src lyr has an md5 checksum and there is a checksum for the QCed data
	//	    then return FALSE if they match, TRUE otherwise
	// if there is a 'lastUpdate' for the QCed data and it matches the 'modifiedOn' date
	// for the src lyr, then return FALSE
	// otherwise return TRUE
	private static boolean updateNeeded(String md5Sum, DateTime modDate, String targetMd5sum, DateTime targetLastDate) {
		if (md5Sum!=null && targetMd5sum!=null) {
			return !md5Sum.equals(targetMd5sum);
		}
		if (targetLastDate!=null) {
			return !targetLastDate.equals(modDate);
		}
		return true;
	}
	
	// crawl the Synapse project given by sourceProjectId
	// return the layers to process, including the following attributes
	// 'lastUpdate', 'url', 'description', 'number_of_samples', 'status', 'createdBy'
	Collection<Map<String,Object>> crawlSourceProject(Synapse synapse) {
		String sourceProjectId = ConfigHelper.getGEPipelineSourceProjectId();
		String targetProjectId = ConfigHelper.getGEPipelineTargetProjectId();
		Collection<Map<String,Object>> layerTasks = new ArrayList<Map<String,Object>>();

		int offset=1;
		int total=0;
		int batchSize = 20;
		int layerCount = 0;
		do {
			try {
				// get a batch of datasets
				JSONObject o = synapse.query("select * from dataset where parentId=="+sourceProjectId+" LIMIT "+batchSize+" OFFSET "+offset);
				total = (int)o.getLong("totalNumberOfResults");
				System.out.println("Datasets: "+offset+"->"+Math.min(total, offset+batchSize-1)+" of "+total);
				JSONArray a = o.getJSONArray("results");
				for (int i=0; i<a.length(); i++) {
					JSONObject ds = (JSONObject)a.get(i);
					String id = ds.getString("dataset.id");
					String sourceDatasetName = ds.getString("dataset.name");
					
					// !!! temporary FOR DEBUGGING !!!
					//if (!"Uterine Corpus Endometrioid Carcinoma TCGA".equals(sourceDatasetName)) continue;
					
					System.out.println("Dataset: "+sourceDatasetName);
					String description = ds.getString("dataset.description");
//					System.out.println("Dataset: "+ds);
					String status = ds.getString("dataset.status");
					String createdBy = ds.getString("dataset.createdBy");
					// get the genomic and genetic layers
					JSONObject layers = synapse.query("select * from layer where parentId=="+id);
					JSONArray layersArray = layers.getJSONArray("results");
					JSONObject targetDataset = null;
					for (int j=0; j<layersArray.length(); j++) {
						JSONObject layer = (JSONObject)layersArray.get(j);
						
						String layerId = layer.getString("layer.id");
						String layerName = layer.getString("layer.name");
						
						// !!!! TEMPORARY, for debugging !!!!
						//if (!"unc.edu_COAD.AgilentG4502A_07_3.Level_1.2.0.0".equals(layerName)) continue;

						String type = layer.getString("layer.type");
						// only 'crawl' Expression and Genotyping layers (not Clincial or Media layers)
						if (!(type.equalsIgnoreCase("E") || type.equalsIgnoreCase("G"))) continue;
						
						// get the URL for the layer:
						JSONObject locationQueryResult = synapse.query("select * from location where parentId=="+layerId);
						JSONArray locations = locationQueryResult.getJSONArray("results");
						if (locations.length()==0) continue;
						
						layerCount++;
						

						Map<String,Object> layerAttributes = new HashMap<String,Object>();
						layerAttributes.put(SOURCE_LAYER_ID_PROPERTY_NAME, layer.getString("layer.id"));
						String targetDatasetName = sourceDatasetName;
						layerAttributes.put(TARGET_DATASET_NAME_PROPERTY_NAME, targetDatasetName);
						String modDateString = layer.getString("layer.modifiedOn");
						DateTime modDate = new DateTime(Long.parseLong(modDateString));
						
						// get location mdsum
						JSONObject location = locations.getJSONObject(0);
						String md5Sum = location.getString("location.md5sum");
						
						// find the target dataset...
						if (targetDataset==null || !targetDatasetName.equals(targetDataset.getString("dataset.name"))) {
							JSONObject dsQueryResult = synapse.query("select * from dataset where parentId=="+targetProjectId+" AND name==\""+targetDatasetName+"\"");
							JSONArray datasets = dsQueryResult.getJSONArray("results");
							if (datasets.length()>0) {
								targetDataset = datasets.getJSONObject(0);
								String tdsId = targetDataset.getString("dataset.id");
								JSONObject annots = synapse.getEntity("/dataset/"+tdsId+"/annotations");
//								System.out.println("\t\tTarget dataset annotations:\n\t\t"+annots);								
							}
						}
						// ... then extract the last update date and md5sum from the annotations
						if (targetDataset==null) {
							System.out.println("\t\tUnable to find target dataset: "+targetDatasetName);
						} else {
							// the following mirrors 'lastUpdateAnnotName' in synapseWorkflow.R
							DateTime targetLastDate = null;
							String lastUpdateAnnotName = "dataset."+layerId+"_lastUpdate";
							if (targetDataset.has(lastUpdateAnnotName)) {
								JSONArray targetLastUpdateArray = targetDataset.getJSONArray(lastUpdateAnnotName);
								if (targetLastUpdateArray.length()>0) {
									String targetLastUpdate = targetLastUpdateArray.getString(0);
									targetLastDate = new DateTime(targetLastUpdate);
								}
							}
							String targetMd5sum = null;
							String md5sumAnnotName = "dataset."+layerId+"_md5sum";
							if (targetDataset.has(md5sumAnnotName)) {
								JSONArray targetMd5sumArray = targetDataset.getJSONArray(md5sumAnnotName);
								if (targetMd5sumArray.length()>0) {
									targetMd5sum = targetMd5sumArray.getString(0);
								}
							}
							if (!updateNeeded(md5Sum, modDate, targetMd5sum, targetLastDate)) continue;
						}
						
						// we only reach this point if we can't show that the task has already been done
						// for the source layer

						if (layer.has("dataset.number_of_samples")) {
							layerAttributes.put(NUMBER_OF_SAMPLES_PROPERTY_NAME, layer.getLong("dataset.number_of_samples"));
						}
						layerAttributes.put("description", description);
						if (status!=null) layerAttributes.put("status", status);
						if (createdBy!=null) layerAttributes.put("createdBy", createdBy);
						layerTasks.add(layerAttributes);
					}
				}
			} catch (Exception e) {
				log.warn(e);
				e.printStackTrace();
			}
			offset += batchSize;
		} while (offset<=total);
		System.out.println("Found "+layerCount+" layers, of which "+layerTasks.size()+" need to be updated.");
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
		Logger.getLogger(Synapse.class.getName()).setLevel(Level.WARN);
		 // Running Annotation Processor is very important, otherwise it will
		// treat Workflows and Activities as regular java method calls
		ActivityAnnotationProcessor.processAnnotations();

		// Create the client for Simple Workflow Service
		AmazonSimpleWorkflow swfService = ConfigHelper.createSWFClient();
		AsyncWorkflowStartContext.initialize(swfService);

		GEPWorkflowInitiator initiator = new GEPWorkflowInitiator();

		if (true) {
			initiator.initiateWorkflowTasks();
		} else {
			Synapse synapse = initiator.connectToSynapse();
			Collection<Map<String,Object>> layerTasks = initiator.crawlSourceProject(synapse);
		}

		
		System.exit(0);
	}

}
