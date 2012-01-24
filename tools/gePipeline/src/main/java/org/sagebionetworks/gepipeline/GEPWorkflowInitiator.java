package org.sagebionetworks.gepipeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.Location;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
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
	private static final String NOTIFICATION_SUBJECT = "MetaGenomics Initiator Notification ";
	private static final String NUMBER_OF_SAMPLES_PROPERTY_NAME = "Number_of_Samples";
	private static final String SOURCE_LAYER_ID_PROPERTY_NAME = "sourceLayerId";
	private static final String SOURCE_LAYER_LOCATION_PROPERTY_NAME = "layerLocation";
	private static final int MAX_LISTED_LOCATIONS = 200;

	/**
	 * Crawl all top level datasets and identify the ones in which we are
	 * interested
	 */
	public static void initiateWorkflowTasks() {
		
		String projectId = ConfigHelper.getGEPipelineTargetProjectId();
		
		Synapse synapse = connectToSynapse();
		String sourceProjectId = ConfigHelper.getGEPipelineSourceProjectId();
		String targetProjectId = ConfigHelper.getGEPipelineTargetProjectId();
		Collection<Map<String,Object>> layerTasks = crawlSourceProject(synapse, sourceProjectId, targetProjectId);
		
		
		String maxInstances = ConfigHelper.getGEPipelineMaxWorkflowInstances();
		int max = maxInstances==null ? -1 : Integer.parseInt(maxInstances); // set to -1 to disable
		int i = 0;
		System.out.println("Got "+layerTasks.size()+" datasets to run and will run "+
				(max==-1?"all":""+max)+" of them.");
		List<String> locations = new ArrayList<String>();
		for (Map<String,Object>parameterMap: layerTasks) {
			if (max>0 && i++>=max) break; // for debugging, allows us to launch just a few...
			locations.add((String)parameterMap.remove(SOURCE_LAYER_LOCATION_PROPERTY_NAME));
			try {
				JSONObject jsonParameters = new JSONObject(parameterMap);
				jsonParameters.put(TARGET_PROJECT_ID_PROPERTY_NAME, projectId);
				String parameterString =jsonParameters.toString();
				String activityRequirement = GEPWorkflow.ACTIVITY_REQUIREMENT_LARGE;
				if (jsonParameters.has(NUMBER_OF_SAMPLES_PROPERTY_NAME)) {
					int numSamples = (int)jsonParameters.getLong(NUMBER_OF_SAMPLES_PROPERTY_NAME);
					activityRequirement = getActivityRequirementFromSampleCount(numSamples); 
				}
				GEPWorkflow.doWorkflow(parameterString, activityRequirement);
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}
		
		StringBuilder crawlerOutput = new StringBuilder("Initiated "+locations.size()+" MetaGenomics workflows."); //"Output from Crawler:\nStdout:\n"+results.getStdout()+"Stderr:\n"+results.getStderr();
		if (locations.size()>MAX_LISTED_LOCATIONS) crawlerOutput.append("\nHere are the first "+MAX_LISTED_LOCATIONS+":");
		int listed = 0;
		for (String loc: locations) {
			if (++listed>MAX_LISTED_LOCATIONS) break;
			crawlerOutput.append("\n"+loc);
		}
		Notification.doSnsNotifyFollowers(GEPWorkflow.NOTIFICATION_SNS_TOPIC, 
				NOTIFICATION_SUBJECT + new LocalDate().toString(),
				crawlerOutput.toString());		
	}
	
	private static Synapse connectToSynapse() {
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
	public static Collection<Map<String,Object>> crawlSourceProject(Synapse synapse, String sourceProjectId, String targetProjectId) {
		Collection<Map<String,Object>> layerTasks = new ArrayList<Map<String,Object>>();

		int offset=1;
		int total=0;
		int batchSize = 20;
		int layerCount = 0;
		do {
			try {
				// get a batch of datasets
				JSONObject ids = synapse.query("select id from dataset where parentId=="+sourceProjectId+" LIMIT "+batchSize+" OFFSET "+offset);
				total = (int)ids.getLong("totalNumberOfResults");
				System.out.println("Datasets: "+offset+"->"+Math.min(total, offset+batchSize-1)+" of "+total);
				JSONArray a = ids.getJSONArray("results");
				for (int i=0; i<ids.length(); i++) {
					Dataset ds = (Dataset) synapse.getEntityById(((JSONObject)a.get(i)).getString("dataset.id"));
					String id = ds.getId();
					String sourceDatasetName = ds.getName();
					
					// TEMPORARY, for testing
					// if (sourceDatasetName.indexOf("TCGA")>=0) continue; // skip TCGA
					// end TEMPORARY
					
					System.out.println("Dataset: "+sourceDatasetName);
					String description = ds.getDescription();
					String status = ds.getStatus();
					String createdBy = ds.getCreatedBy();
					// get the genomic and genetic layers
					JSONObject layerIds = synapse.query("select id from layer where parentId=="+id);
					JSONArray layersIdsArray = layerIds.getJSONArray("results");
					Dataset targetDataset = null;
					JSONObject targetDatasetJSON = null;
					for (int j=0; j<layersIdsArray.length(); j++) {
						Layer layer = (Layer) synapse.getEntityById(((JSONObject)layersIdsArray.get(j)).getString("layer.id"));
						
						String layerId = layer.getId();
						
						LayerTypeNames type = layer.getType();
						// only 'crawl' Expression and Genotyping layers (not Clincial or Media layers)
						if (!(type.equals(LayerTypeNames.E) || type.equals(LayerTypeNames.G))) continue;
						
						List<LocationData> locations = layer.getLocations();
						if (locations==null || locations.size()==0) continue;
						
						layerCount++;
						

						Map<String,Object> layerAttributes = new HashMap<String,Object>();
						layerAttributes.put(SOURCE_LAYER_ID_PROPERTY_NAME, layerId);
						String targetDatasetName = sourceDatasetName;
						layerAttributes.put(TARGET_DATASET_NAME_PROPERTY_NAME, targetDatasetName);
						DateTime modDateTime = new DateTime(layer.getModifiedOn());
						
						LocationData locationData = locations.get(0);
						// get location mdsum
						String md5Sum = layer.getMd5();
						// get the URL for the layer:
						String layerLocation = locationData.getPath();
						layerAttributes.put(SOURCE_LAYER_LOCATION_PROPERTY_NAME, layerLocation);
						
						// find the target dataset...
						if (targetDataset==null || !targetDatasetName.equals(targetDataset.getName())) {
							// TODO:  In long run, migrate away from 'select *'.  For now we need to do this
							// in order to get the annotations
							JSONObject dsQueryResult = synapse.query("select * from dataset where parentId=="+targetProjectId+" AND name==\""+targetDatasetName+"\"");
							JSONArray datasets = dsQueryResult.getJSONArray("results");
							if (datasets.length()>0) {
								targetDatasetJSON = datasets.getJSONObject(0);
								targetDataset =  (Dataset) synapse.getEntityById(targetDatasetJSON.getString("dataset.id"));
//								targetDataset = (Dataset)EntityFactory.createEntityFromJSONObject(targetDatasetJSON, Dataset.class);
//								String tdsId = targetDataset.getId();
							}
						}
						// ... then extract the last update date and md5sum from the annotations
						if (targetDataset==null) {
							System.out.println("\t\tUnable to find target dataset: "+targetDatasetName);
						} else {
							// the following mirrors 'lastUpdateAnnotName' in synapseWorkflow.R
							DateTime targetLastDate = null;
							String lastUpdateAnnotName = "dataset."+layerId+"_lastUpdate";
							if (targetDatasetJSON.has(lastUpdateAnnotName)) {
								JSONArray targetLastUpdateArray = targetDatasetJSON.getJSONArray(lastUpdateAnnotName);
								if (targetLastUpdateArray.length()>0) {
									String targetLastUpdate = targetLastUpdateArray.getString(0);
									targetLastDate = new DateTime(targetLastUpdate);
								}
							}
							String targetMd5sum = null;
							String md5sumAnnotName = "dataset."+layerId+"_md5sum";
							if (targetDatasetJSON.has(md5sumAnnotName)) {
								JSONArray targetMd5sumArray = targetDatasetJSON.getJSONArray(md5sumAnnotName);
								if (targetMd5sumArray.length()>0) {
									targetMd5sum = targetMd5sumArray.getString(0);
								}
							}
							if (!updateNeeded(md5Sum, modDateTime, targetMd5sum, targetLastDate)) continue;
						}
						
						// we only reach this point if we can't show that the task has already been done
						// for the source layer

						if (layer.getNumSamples()!=null) {
							layerAttributes.put(NUMBER_OF_SAMPLES_PROPERTY_NAME, layer.getNumSamples());
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
			// TEMPORARY, for testing
			// if (layerCount>=200) break;
			// end TEMPORARY
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
//			initiator.kickoffPeriodicWorkflow();
			GEPWorkflowInitiator.initiateWorkflowTasks();
		} else {
			// this allows us to see what jobs will be started, without actually scheduling them.
			Synapse synapse = initiator.connectToSynapse();
			String sourceProjectId = ConfigHelper.getGEPipelineSourceProjectId();
			String targetProjectId = ConfigHelper.getGEPipelineTargetProjectId();
			Collection<Map<String,Object>> layerTasks = initiator.crawlSourceProject(synapse, sourceProjectId, targetProjectId);
		}

		
		System.exit(0);
	}

}
