package org.sagebionetworks.gepipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationStatusNames;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.MD5ChecksumHelper;

/**
 * This class is a collection of miscellaneous scripts
 *  for modifying data created by the GEPipeline workflow
 *  or for collecting information about the datasets in a project
 */
public class DataModifier {

	private static final String REPO_ENDPOINT = "https://repo-alpha.sagebase.org/repo/v1";
	private static final String AUTH_ENDPOINT = "https://auth-alpha.sagebase.org/auth/v1";

	private static final String DATASET_NOS_TAG = "Number_of_Samples";
	private static final String LAYER_NOS_TAG = "numSamples";
	private static final String INTERIM_NOS_TAG = "number_of_samples";
	
	
	private static Synapse getSynapse(String user, String pw) throws SynapseException {
		Synapse synapse = new Synapse();
		HttpClientHelper.setGlobalConnectionTimeout(DefaultHttpClientSingleton.getInstance(), 30000);
		HttpClientHelper.setGlobalSocketTimeout(DefaultHttpClientSingleton.getInstance(), 30000);
		synapse.setAuthEndpoint(AUTH_ENDPOINT);
		synapse.setRepositoryEndpoint(REPO_ENDPOINT);

		synapse.login(user, pw);
		return synapse;
	}
	
	/**
	 * The R client cannot currently write the datasets' number of samples into the Long field meant for this purpose.
	 * As a workaround we have the client write into another, String field, and use this ad hoc process to copy the
	 * data into the Long field.
	 */
	public static void updateNOSAnnotations(String user, String pw) throws Exception {
		Synapse synapse = getSynapse(user, pw);
		int offset=1;
		int total=0;
		int batchSize = 20;
		do {
			try {
				JSONObject o = synapse.query("select * from dataset where parentId==102610 LIMIT "+batchSize+" OFFSET "+offset);
				total = (int)o.getLong("totalNumberOfResults");
				System.out.println(""+offset+"->"+(offset+batchSize-1)+" of "+total);
				JSONArray a = o.getJSONArray("results");
	//			System.out.println("this batch has "+a.length()+" result(s) out of "+total+".");
				for (int i=0; i<a.length(); i++) {
					JSONObject ds = (JSONObject)a.get(i);
					String id = ds.getString("dataset.id");
					String name = ds.getString("dataset.name");
					String annotUri = "/dataset/"+id+"/annotations";
					JSONObject annots = synapse.getSynapseEntity(REPO_ENDPOINT, annotUri); // this is an unnecessary repeated retrieval
					long nos=-1;
					try {
						nos = dataSetNumberOfSamples(synapse, annots); // see if the target annotation is already set
					} catch (JSONException e) {
						System.err.println("Unable to extract the nos from the 'long' annotations for ds "+name);
					}
					boolean commuteDS=false;
					if (nos<0) {
						nos=commuteNosDataset(synapse, id, annots); // if not, try to set it from the interim annotation
						commuteDS = (nos>=0);
					}
					if (nos<0) continue; // if no target nos annot OR interim field, then give up on this dataset
					// now get the layers and update them too, if needed
					JSONObject layers = synapse.query("select * from layer where parentId=="+id);
					JSONArray layersArray = layers.getJSONArray("results");
					int updatedLayerCount = 0;
					for (int j=0; j<layersArray.length(); j++) {
						JSONObject layer = (JSONObject)layersArray.get(j);
						if (layer.has(LAYER_NOS_TAG)) {
							long currentNos = layer.getLong(LAYER_NOS_TAG);
							if (currentNos==nos) continue; // nothing to do
						}
						String layerId = layer.getString("layer.id");
						setNosLayer(synapse, layerId, nos);
						updatedLayerCount++;
					}
					if (commuteDS) {
						System.out.println("Commuted n.o.s. for dataset "+name+" and "+updatedLayerCount+" layers.");
					} else {
						System.out.println("For dataset "+name+", set n.o.s. for "+updatedLayerCount+" layers.");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			offset += batchSize;
		} while (offset<=total);
	}
	
	// if the dataset set already has Number_of_Samples returns this value, else returns -1
	private static long dataSetNumberOfSamples(Synapse synapse, JSONObject annots) throws Exception {
		JSONObject longAnnots = annots.getJSONObject("longAnnotations");
		if (longAnnots.has(DATASET_NOS_TAG)) {
			return longAnnots.getLong(DATASET_NOS_TAG);
		} else {
			return -1;
		}
	}
	
	private static long commuteNosDataset(Synapse synapse, String id, JSONObject annots) throws Exception {
		String annotUri = "/dataset/"+id+"/annotations";
		long nos = -1L;
		if (nos<0) {
			JSONObject stringAnnots = annots.getJSONObject("stringAnnotations");
			if (stringAnnots.has(INTERIM_NOS_TAG)) {
				JSONArray stringNos = stringAnnots.getJSONArray(INTERIM_NOS_TAG);
				nos = Long.parseLong(stringNos.getString(0));
			}
		}
		if (nos>=0) {
			// is NOS already set?
			
			JSONArray longNos = new JSONArray();
			longNos.put(nos);
			JSONObject longNosAnnots = new JSONObject();
			longNosAnnots.put(DATASET_NOS_TAG, longNos);
			JSONObject newAnnots = new JSONObject();
			newAnnots.put("longAnnotations", longNosAnnots);
//			System.out.println("Modified annots:\n"+newAnnots);
			synapse.updateEntity(annotUri, newAnnots);
			return nos;
		}
		return -1L;
	}
	
	private static void setNosLayer(Synapse synapse, String id, long nos) throws Exception {
		String layerUri = "/layer/"+id;
		JSONObject layer = new JSONObject();
		layer.put(LAYER_NOS_TAG, nos);
		synapse.updateSynapseEntity(REPO_ENDPOINT, layerUri, layer);
	}
	
	public static void deleteStaleProvenanceString(String user, String pw) throws Exception {
		Synapse synapse = getSynapse(user, pw);
		int offset=1;
		int total=0;
		int batchSize = 20;
		int deletedNodeCount = 0;
		do {
			int deletedAnalysesInBatch = 0;
			try {
				JSONObject o = synapse.query("select * from analysis where parentId==16114 LIMIT "+batchSize+" OFFSET "+offset);
				total = (int)o.getLong("totalNumberOfResults");
				System.out.println(""+offset+"->"+(offset+batchSize-1)+" of "+total);
				JSONArray a = o.getJSONArray("results");
				for (int i=0; i<a.length(); i++) {
					JSONObject ds = (JSONObject)a.get(i);
					String id = ds.getString("analysis.id");
					JSONObject steps = synapse.query("select * from step where parentId=="+id);
					JSONArray stepsArray = steps.getJSONArray("results");
					int stepCount = stepsArray.length();
					int outputLayerReferenceCount = 0;
//					System.out.println("Analysis "+id+" has "+stepsArray.length()+" steps.");
					for (int j=0; j<stepsArray.length(); j++) {
						JSONObject step = stepsArray.getJSONObject(j);
//						System.out.println("\t"+step.get("step.id"));
						if (step.has("step.output")) {
							JSONArray outputRefs = step.getJSONArray("step.output");
							for (int k=0; k<outputRefs.length(); k++) {
								JSONObject ref = outputRefs.getJSONObject(k);
								if (ref.has("targetId")) {
									String layerId = (String)ref.get("targetId");
//									System.out.println("\t\toutput layer id: "+layerId);
									try {
										JSONObject layer = synapse.getEntity("/layer/"+layerId);
//										System.out.println("\t\t\t"+layer);
										outputLayerReferenceCount++;
									} catch (SynapseException ioe) {
//										System.out.println("\t\t\tcan't retrieve id");
									}
								}
							}
						}
					}
//					System.out.println("Analysis "+id+" has "+stepCount+" steps and "+outputLayerReferenceCount+" valid output layer references.");
					if (outputLayerReferenceCount==0) {
						synapse.deleteEntity("/analysis/"+id);
						deletedNodeCount+= 1+stepCount;
						deletedAnalysesInBatch++;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			offset += batchSize-deletedAnalysesInBatch;
			System.out.println("Deleted "+deletedNodeCount+" nodes for stale analyses/steps.");
		} while (offset<=total);
	}
	
	// migrate all datasets from the project 'origProjectID' to 'newProjectID'
	// which have 'datasetNameSubstring' in their name, unless their IDs are in the list of 'exceptions'
	public static void migrateDatasets(int origProjectID, int newProjectID, String datasetNameSubstring,
			Collection<Integer> exceptions, String user, String pw) throws SynapseException {
		Synapse synapse = getSynapse(user, pw);
		int offset=1;
		int total=0;
		int batchSize = 20;
		do {
			int movedCount = 0;
			try {
				JSONObject o = synapse.query("select * from dataset where parentId=="+origProjectID+" LIMIT "+batchSize+" OFFSET "+offset);
				total = (int)o.getLong("totalNumberOfResults");
				System.out.println(""+offset+"->"+(offset+batchSize-1)+" of "+total);
				JSONArray a = o.getJSONArray("results");
				for (int i=0; i<a.length(); i++) {
					JSONObject ds = (JSONObject)a.get(i);
					String id = ds.getString("dataset.id");
					String name = ds.getString("dataset.name");
					if (name.contains(datasetNameSubstring) && !exceptions.contains(Integer.parseInt(id))) {
						// then move the dataset to the new project
						String datasetUri = "/dataset/"+id;
						JSONObject dsjson = new JSONObject();
						dsjson.put("parentId", ""+newProjectID);
						synapse.updateSynapseEntity(REPO_ENDPOINT, datasetUri, dsjson);
						System.out.println("Moved "+name+" to project "+newProjectID);
						movedCount++;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			offset += batchSize-movedCount;
		} while (offset<=total);
	}
	
	// delete datasets from the project 'projectID' unless their IDs are in the list of 'exceptions'
	public static void deleteDatasets(int projectID, Collection<Integer> exceptions, String user, String pw) throws SynapseException {
		Synapse synapse = getSynapse(user, pw);
		int offset=1;
		int total=0;
		int batchSize = 20;
		do {
			int deletedCount = 0;
			try {
				JSONObject o = synapse.query("select * from dataset where parentId=="+projectID+" LIMIT "+batchSize+" OFFSET "+offset);
				total = (int)o.getLong("totalNumberOfResults");
				System.out.println(""+offset+"->"+(offset+batchSize-1)+" of "+total);
				JSONArray a = o.getJSONArray("results");
				for (int i=0; i<a.length(); i++) {
					JSONObject ds = (JSONObject)a.get(i);
					String id = ds.getString("dataset.id");
					String name = ds.getString("dataset.name");
					if (!exceptions.contains(Integer.parseInt(id))) {
						// then move the dataset to the new project
						String datasetUri = "/dataset/"+id;
						synapse.deleteSynapseEntity(REPO_ENDPOINT, datasetUri);
						System.out.println("Deleted "+name+" from project "+projectID);
						deletedCount++;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			offset += batchSize-deletedCount;
		} while (offset<=total);
	}
	
	// count the datasets, layers, and locations in a project
	public static void projectStats(int projectId, String user, String pw) throws SynapseException {
		Synapse synapse = getSynapse(user, pw);
		int offset=1;
		int total=0;
		int batchSize = 20;
		
		int datasetCount = 0;
		int layerCount = 0;
		int locationCount = 0;
		do {
			try {
				JSONObject o = synapse.query("select * from dataset where parentId=="+projectId+" LIMIT "+batchSize+" OFFSET "+offset);
				total = (int)o.getLong("totalNumberOfResults");
				System.out.println(""+offset+"->"+(offset+batchSize-1)+" of "+total);
				JSONArray a = o.getJSONArray("results");
				for (int i=0; i<a.length(); i++) {
					datasetCount++;
					JSONObject ds = (JSONObject)a.get(i);
					System.out.println(ds);
					String datasetId = ds.getString("dataset.id");
					JSONObject lo = synapse.query("select * from layer where parentId=="+datasetId);
					JSONArray la = lo.getJSONArray("results");
					for (int j=0; j<la.length(); j++) {
						layerCount++;
						JSONObject layer = (JSONObject)la.get(j);
						String layerId = layer.getString("layer.id");
						JSONObject loco = synapse.query("select * from location where parentId=="+layerId);
						JSONArray loca = loco.getJSONArray("results");
						locationCount += loca.length();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			offset += batchSize;
		} while (offset<=total);
		System.out.println(
				"For projectId="+projectId+", "+
				"# datasets="+datasetCount+", "+
				"# layers="+layerCount+", "+
				"# locations="+locationCount
				);
	}
	
	public static void layerModifiedOn(int id, String user, String pw) throws Exception {
		Synapse synapse = getSynapse(user, pw);
		
		JSONObject o = synapse.getEntity("/layer/"+id);
		String modString = o.getString("modifiedOn");
//		DateTime dt = new DateTime(Long.parseLong(modString));
		System.out.println("Layer "+id+" modified on: "+modString);
	}
	
	private static final String LAST_UPDATE_SUFFIX = "_lastUpdate";
	private static final String MD5SUM_SUFFIX = "_md5sum";
	
	// find all the "_lastUpdate" annotations in the datasets in projectId
	// for each, take the prefix, which is a layer id, get the corresponding
	// md5 checksum, and add a "_md5sum" annotation to the dataset
	public static void checksum(int projectId, String user, String pw) throws SynapseException {
		Synapse synapse = getSynapse(user, pw);
		int offset=1;
		int total=0;
		int batchSize = 20;
		
		int datasetCount = 0;
		int layerCount = 0;
		int locationCount = 0;
		do {
			try {
				JSONObject o = synapse.query("select * from dataset where parentId=="+projectId+" LIMIT "+batchSize+" OFFSET "+offset);
				total = (int)o.getLong("totalNumberOfResults");
				System.out.println(""+offset+"->"+(offset+batchSize-1)+" of "+total);
				JSONArray a = o.getJSONArray("results");
				for (int i=0; i<a.length(); i++) {
					datasetCount++;
					JSONObject ds = (JSONObject)a.get(i);
					String id = ds.getString("dataset.id");
					String name = ds.getString("dataset.name");
					System.out.println(name);
					String dsAnnotUri = "/dataset/"+id+"/annotations";
					JSONObject annots = synapse.getEntity(dsAnnotUri);
					JSONObject stringAnnots = annots.getJSONObject("stringAnnotations");
					Iterator<String> it = stringAnnots.keys();
					Map<String,String> md5Map = new HashMap<String,String>();
					while (it.hasNext()) {
						String annotName = it.next();
						//System.out.println("\t"+annotName);
						if (!annotName.endsWith(LAST_UPDATE_SUFFIX)) continue;
						String srcLayerId = annotName.substring(0, annotName.length()-LAST_UPDATE_SUFFIX.length());
						if (stringAnnots.has(srcLayerId+MD5SUM_SUFFIX)) continue;
						JSONObject locationQueryResult = synapse.query("select * from location where parentId=="+srcLayerId);
						JSONArray locations = locationQueryResult.getJSONArray("results");
						if (locations.length()<1) continue;
						JSONObject location = locations.getJSONObject(0);
						String md5Sum = location.getString("location.md5sum");
						md5Map.put(srcLayerId, md5Sum);
					}
					System.out.println("\t"+md5Map.size()+" source layers found.");
					JSONObject newAnnots = new JSONObject();
					stringAnnots = new JSONObject();
					for (String key : md5Map.keySet()) {
						JSONArray value = new JSONArray();
						value.put(md5Map.get(key));
						stringAnnots.put(key+MD5SUM_SUFFIX,value);
					}
					newAnnots.put("stringAnnotations", stringAnnots);
					synapse.updateSynapseEntity(REPO_ENDPOINT, dsAnnotUri, newAnnots);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			offset += batchSize;
		} while (offset<=total);
	}

	// reconcile the source project with the MetaGenomics output
	public static void reconcileProject(int sourceProjectId, int targetProjectId, String user, String pw, boolean noGEO) throws SynapseException {
		Synapse synapse = getSynapse(user, pw);
		int offset=1;
		int total=0;
		int batchSize = 20;
		
		int datasetCount = 0;
		int layerCount = 0;
		int locationCount = 0;
		System.out.println("SrcLyr\tworkflowHasRun\tqcHasCompleted");
		do {
			try {
				JSONObject o = synapse.query("select * from dataset where parentId=="+sourceProjectId+" LIMIT "+batchSize+" OFFSET "+offset);
				total = (int)o.getLong("totalNumberOfResults");
				System.out.println(""+offset+"->"+(offset+batchSize-1)+" of "+total);
				JSONArray a = o.getJSONArray("results");
				for (int i=0; i<a.length(); i++) {
					datasetCount++;
					JSONObject ds = (JSONObject)a.get(i);
					String datasetName = ds.getString("dataset.name");
					if (noGEO && datasetName.startsWith("GSE")) continue;
					System.out.println(datasetName);
					// find the target dataset
					JSONObject tdsQR = synapse.query("select * from dataset where parentId=="+targetProjectId+" AND dataset.name==\""+datasetName+"\"");
					JSONArray tdsa = tdsQR.getJSONArray("results");
					JSONObject targetDataset = tdsa.getJSONObject(0);
					// get its (String) annotations
					JSONObject targetDSAnnotations = synapse.getEntity("/dataset/"+targetDataset.getString("dataset.id")+"/annotations").getJSONObject("stringAnnotations");
					// find the layers
					String datasetId = ds.getString("dataset.id");
					JSONObject lo = synapse.query("select * from layer where parentId=="+datasetId);
					JSONArray la = lo.getJSONArray("results");
					for (int j=0; j<la.length(); j++) {
						layerCount++;
						JSONObject layer = (JSONObject)la.get(j);
						String layerId = layer.getString("layer.id");
						String sourceLayerName = layer.getString("layer.name");
						JSONObject loco = synapse.query("select * from location where parentId=="+layerId);
						JSONArray loca = loco.getJSONArray("results");
						locationCount += loca.length();
						String sourceURL = null;
						if (loca.length()>0) {
							JSONObject location = loca.getJSONObject(0);
							String path = location.getString("location.path");
							int start = path.lastIndexOf('/');
							int end = path.indexOf('?');
							if (start<0) start=0;
							if (end<0) end = path.length();
							sourceURL = path.substring(start+1, end);
						}
						// find the annotations in the target dataset that show the source layer has been processed
						boolean workflowHasRun = false;
						boolean qcHasCompleted = false;
						Iterator annotIt = targetDSAnnotations.keys();
						while (annotIt.hasNext()) {
							String key = (String)annotIt.next();
							if (key.startsWith(layerId)) {
								workflowHasRun = true;
								break;
							}
						}
						// look for the QCed layers in the target dataset
						JSONObject tlo = synapse.query("select * from layer where parentId=="+targetDataset.getString("dataset.id"));
						JSONArray tla = tlo.getJSONArray("results");
						for (int k=0; k<tla.length(); k++) {
							String tgtLyrName = ((JSONObject)tla.get(k)).getString("layer.name");
							// the target layers are named after the source layers (with prefix, suffix added)
							// so just look to see if the source layer name is contained
							if (tgtLyrName.indexOf(sourceLayerName)>=0) {
								qcHasCompleted=true;
								break;
							}
						}
						System.out.println(layerId+"\t"+workflowHasRun+"\t"+qcHasCompleted+"\t"+sourceURL);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			offset += batchSize;
		} while (offset<=total);

	}
	
	private static final String[] RSCRIPT_PREFIX = new String[] {"Completed exec: ", " Activity failed(1) for "};
	private static final String STATUS_PREFIX = " \"status\": ";
	private static final String STATUS_SUFFIX = ",";
	private static final String SOURCE_LAYER_PREFIX = "sourceLayerId%22%3A%22";
	private static final String SOURCE_LAYER_SUFFIX = "%22";
	private static final String ERROR_MESSAGE_PREFIX = "\"msg\": ";
	public static void parseLog(String[] logFileNames, String outputFileName) throws IOException {
		PrintWriter pw = new PrintWriter(new File(outputFileName));
		for (String logFileName: logFileNames) {
			InputStream is = new FileInputStream(new File(logFileName));
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String s = br.readLine();
			int lineNo = 1;
			String rscriptCmd = null;
			String latestStatus = null;
			String latestLayerId = null;
			String outputStatus = null;
			Collection<String> layers = new HashSet<String>();
			while (s!=null) {
				for (String prefix : RSCRIPT_PREFIX) {
					int rscriptStart = s.indexOf(prefix);
					if (rscriptStart>=0) {
						rscriptStart += prefix.length();
						rscriptCmd = s.substring(rscriptStart);
						break;
					}
				}
				int statusStart = s.indexOf(STATUS_PREFIX);
				if (statusStart>=0) {
					statusStart += STATUS_PREFIX.length();
					int statusEnd = s.indexOf(STATUS_SUFFIX, statusStart);
					latestStatus = s.substring(statusStart, statusEnd);
				}
				int layerStart = s.indexOf(SOURCE_LAYER_PREFIX);
				if (layerStart>=0) {
					layerStart += SOURCE_LAYER_PREFIX.length();
					int layerEnd = s.indexOf(SOURCE_LAYER_SUFFIX, layerStart);
					latestLayerId = s.substring(layerStart, layerEnd);
				}
				int msgStart = s.indexOf(ERROR_MESSAGE_PREFIX);
				if (msgStart>=0) {
					msgStart += ERROR_MESSAGE_PREFIX.length();
					String where = " "+logFileName+" line "+lineNo+"\n"+s;
					if (latestLayerId==null) {
						throw new IllegalStateException("Found msg before finding a source layer Id."+where);
					}
					if (rscriptCmd==null) {
						throw new IllegalStateException("Found msg before finding RSCRIPT command."+where);
					}
					if (latestStatus==null) {
						throw new IllegalStateException("Found msg before finding a 'status'."+where);
					}
					if (rscriptCmd.indexOf(latestLayerId)<0) {
						throw new IllegalStateException("wrong RSCRIPT command for "+latestLayerId+": "+rscriptCmd+where);
					}
					if (!layers.contains(latestLayerId)) pw.println(latestStatus+"\t"+latestLayerId+"\t"+s.substring(msgStart)+"\t"+rscriptCmd);
					layers.add(latestLayerId);
					latestLayerId = null;
					latestStatus = null;
					rscriptCmd = null;
				}
				s = br.readLine();
				lineNo++;
			}
			br.close();
			is.close();
		}
		pw.close();
	}
	
	public static void checkCheckSums(String user, String pw, String[] layerIds) throws Exception {
		Synapse synapse = getSynapse(user, pw);

		for (String layerId : layerIds) {
			// find a location
			JSONObject loco = synapse.query("select * from location where parentId=="+layerId);
			JSONArray loca = loco.getJSONArray("results");
			if (loca.length()==0) {
				System.out.println("No location for layer "+layerId);
				continue;
			} else {
				JSONObject location = loca.getJSONObject(0);
				// get the md5checksum for the location
				String reportedMd5 = location.getString("location.md5sum");
				// download the file from the location
				String path = location.getString("location.path");
				System.out.println(path);
				if (false) {
					File dataFile = File.createTempFile("foo", "bar");
					// this will also check the checksums
					try {
						dataFile = synapse.downloadFromSynapse(path, reportedMd5, dataFile);
						System.out.println("Checksum OK for "+layerId);
					} catch (Exception e) {
						(new Exception("Download or Checksum failed for "+layerId, e)).printStackTrace();
					}
				}
			}
		}
	}
	
	private static final String QCED_EXPRESSION_DATA_PREFIX = "QCd Expression Data ";
	/**
	 * This 'script' migrates 'metaGEO' datasets to the Commons and MetaGenomics projects, so that the 
	 * raw data is in the former and QCed data is in the latter.
	 * For each dataset in MetaGEO:
	 * 0) Log the start of the migration for the dataset
	 * 1) Find or create a dataset under 'MetaGenomics' having the same name
	 * 1a) Copy the 'description'
	 * 2) Find the raw data layer id, which will have the name <datasetName>_rawExpression, and get its layerId
	 * 3) For all other layers:
	 * 3a) Name should be "QCd Expression Data <Platform>".  If not, log the exception and skip the layer
	 * 3b) Move the layer to the MetaGenomics dataset
	 * 3c) Change the name to "QCd Expression Data <datasetName> <Platform>"
	 * 4) Change parent of source dataset from metaGEO to Commons
	 * 5) Change the name of the source layer ID, removing the '_rawExpression' suffix
	 * 6) Retrieve the source layer once more, to get the latest modifiedOn date
	 * 7) To the MetaGenomics dataset, add the String annotation <src_layerId>_lastUpdate = modifiedOn date from source layer
	 * 8) Log the completion of the migration of the dataset
	 */
	public static void geoMigrator(String user, String pw,
			int geoProjectId, int commonsProjectId, int mgProjectId) throws SynapseException {
		Synapse synapse = getSynapse(user, pw);
		int offset=1;
		int total=0;
		int batchSize = 20;
		
		do {
			int migratedDatasetsInBatch = 0;
			try {
				JSONObject o = synapse.query("select * from dataset where parentId=="+geoProjectId+" LIMIT "+batchSize+" OFFSET "+offset);
				total = (int)o.getLong("totalNumberOfResults");
				System.out.println(""+offset+"->"+(offset+batchSize-1)+" of "+total);
				JSONArray a = o.getJSONArray("results");
				for (int i=0; i<a.length(); i++) {
					JSONObject ds = (JSONObject)a.get(i);
					String metaGEODatasetId = ds.getString("dataset.id");
					String metaGEODatasetName = ds.getString("dataset.name");
					System.out.println("Starting migration of "+metaGEODatasetId+" "+metaGEODatasetName);
					JSONObject rawLayer = getUniqueObject(synapse, "select * from layer where parentId=="+metaGEODatasetId+" AND name==\""+metaGEODatasetName+"_rawExpression\"");
					if (rawLayer==null) {
						System.out.println("\tNo raw layer, cannot continue migration.");
						// just move the parent dataset over to the Commons project
						// note: this replicates logic, below
						ds = new JSONObject();
						ds.put("id", metaGEODatasetId);
						ds.put("parentId", commonsProjectId);
						ds = synapse.updateEntity("/dataset/"+metaGEODatasetId, ds);
						migratedDatasetsInBatch++;
						continue;
					}
					String rawLayerId = rawLayer.getString("layer.id");
					if (rawLayerId==null) throw new NullPointerException();
					String targetDatasetId = null;
					JSONObject targetDataset = getUniqueObject(synapse, "select * from dataset where parentId=="+mgProjectId+" AND name==\""+metaGEODatasetName+"\"");
					if (targetDataset==null) {
						// if dataset doesn't exist, then create it
						targetDataset = new JSONObject();
						targetDataset.put("name", metaGEODatasetName);
						targetDataset.put("parentId", mgProjectId);
						if (ds.has("dataset.description")) targetDataset.put("description", ds.getString("dataset.description"));
						targetDataset = synapse.createEntity("/dataset", targetDataset);
						targetDatasetId = targetDataset.getString("id");
					} else {
						targetDatasetId = targetDataset.getString("dataset.id");
					}
					if (targetDatasetId==null) throw new NullPointerException();
					JSONArray la = synapse.query("select * from layer where parentId=="+metaGEODatasetId).getJSONArray("results");
					// now go through the non-raw layers
					for (int j=0; j<la.length(); j++) {
						JSONObject layer = (JSONObject)la.get(j);
						String layerId = layer.getString("layer.id");
						if (layerId.equals(rawLayerId)) continue;
						String layerName = layer.getString("layer.name");
						if (!layerName.startsWith(QCED_EXPRESSION_DATA_PREFIX)) {
							System.out.println("\tUnrecognized layer "+layerName);
							continue;
						}
						String platformSuffix = layerName.substring(QCED_EXPRESSION_DATA_PREFIX.length());
						layer = new JSONObject();
						layer.put("id", layerId);
						layer.put("parentId", targetDatasetId);
						layer.put("name", QCED_EXPRESSION_DATA_PREFIX+metaGEODatasetName+" "+platformSuffix);
						layer = synapse.updateEntity("/layer/"+layerId, layer);
					}
					ds = new JSONObject();
					ds.put("id", metaGEODatasetId);
					ds.put("parentId", commonsProjectId);
					ds = synapse.updateEntity("/dataset/"+metaGEODatasetId, ds);
					migratedDatasetsInBatch++;
					rawLayer = new JSONObject();
					rawLayer.put("id", rawLayerId);
					rawLayer.put("name", metaGEODatasetName);
					rawLayer = synapse.updateEntity("/layer/"+rawLayerId, rawLayer);
					String rawLayerModifiedOn = rawLayer.getString("modifiedOn");
					String targetDatasetAnnotUri = "/dataset/"+targetDatasetId+"/annotations";
					JSONObject targetDatasetAnnots = createAnnot(rawLayerId+"_lastUpdate", rawLayerModifiedOn);
					synapse.updateEntity(targetDatasetAnnotUri, targetDatasetAnnots);
					System.out.println("Completed migration of "+metaGEODatasetId+" "+metaGEODatasetName);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			offset += batchSize-migratedDatasetsInBatch;
		} while (offset<=total);
	}
	
	private static JSONObject getUniqueObject(Synapse synapse, String queryString) throws SynapseException, JSONException {
		JSONObject queryResult = synapse.query(queryString);
		JSONArray array = queryResult.getJSONArray("results");
		if (array.length()==0) return null;
		if (array.length()>1) throw new IllegalStateException("Expected unique result by found "+array.length());
		return (JSONObject)array.get(0);
	}
	
	private static JSONObject createAnnot(String attr, String value) throws JSONException {
		Map<String,String> map = new HashMap<String,String>();
		map.put(attr,value);
		return createAnnot(map);
	}
	
	// creates an annotation JSON object for *string* annotations only
	private static JSONObject createAnnot(Map<String,String> data) throws JSONException {
		JSONObject av = new JSONObject();
		for (String attribute : data.keySet()) {
			JSONArray values = new JSONArray();
			values.put(data.get(attribute));
			av.put(attribute, values);
		}
		JSONObject newAnnots = new JSONObject();
		newAnnots.put("stringAnnotations", av);
		return newAnnots;
	}
	
	public static void updateLayerName(String id, String name, String user, String pw) throws Exception {
		Synapse synapse = getSynapse(user, pw);
		String layerUri = "/layer/"+id;
		JSONObject layer = new JSONObject();
		layer.put("name", name);
		synapse.updateEntity(layerUri, layer);
	}

	public static void deleteDatasets(Integer[] ids, String user, String pw) throws Exception {
		Synapse synapse = getSynapse(user, pw);
		for (Integer id : ids) {
			String uri = "/dataset/"+id;
			synapse.deleteEntity(uri);
		}
	}
	
	// given a list of source layers, find the target dataset for each layer
	// and delete the annotations that are placed there upon completion.
	// This will cause the MetaGenomics crawler to rerun
	public static void rerunCompletedLayers(Integer[] layerIds, int targetProject, String user, String pw) throws Exception {
		Synapse synapse = getSynapse(user, pw);
		for (Integer sourceLayer : layerIds) {
			// get the layer
			JSONObject srcLayer = synapse.getEntity("/layer/"+sourceLayer);
			// get the parent
			String srcLayerId = srcLayer.getString("id");
			JSONObject srcDataset = synapse.getEntity("/dataset/"+srcLayer.getString("parentId"));
			// find the dataset in the target project
			String dsName = srcDataset.getString("name");
			JSONObject tgtDataset = getUniqueObject(synapse, "select * from dataset where parentId=="+targetProject+" AND name==\""+dsName+"\"");
			// get the target annotations
			String annotUri = "/dataset/"+tgtDataset.getString("dataset.id")+"/annotations";
			JSONObject tgtAnnots = synapse.getEntity(annotUri);
			JSONObject stringAnnots = tgtAnnots.getJSONObject("stringAnnotations");
			stringAnnots.remove(srcLayerId+LAST_UPDATE_SUFFIX);
			stringAnnots.remove(srcLayerId+MD5SUM_SUFFIX);
			// not sure if 'stringAnnotations' is a copy or the original, so to be safe let's put it back
			tgtAnnots.put("stringAnnotations", stringAnnots);
			synapse.putEntity(annotUri, tgtAnnots);
		}
	}
	
	public static void main(String[] args) throws Exception {
		 Logger.getLogger(Synapse.class.getName()).setLevel(Level.WARN);
		updateNOSAnnotations(args[0], args[1]);
//		deleteStaleProvenanceString(args[0], args[1]);
		
//		// migrate from 4492 to 102610 all datasets having "TCGA" in their name except for dataset 4513
//		int origProjectID = 4492;
//		int newProjectID = 102610;
//		String datasetNameSubstring = "TCGA";
//		Collection<Integer> exceptions = Arrays.asList(new Integer[]{4513});
//		migrateDatasets(origProjectID, newProjectID, datasetNameSubstring,
//				exceptions, args[0], args[1]);
		 
//		int projectID = 102611;
//		Collection<Integer> exceptions = Arrays.asList(new Integer[]{104472});
//		deleteDatasets(projectID, exceptions, args[0], args[1]);
		 
//		 projectStats(102610, args[0], args[1]); // Sage Commons Repository
//		 projectStats(102611, args[0], args[1]); // MetaGenomics
		 
//		 layerModifiedOn(24040, args[0], args[1]);
		 
//		 checksum(102611, args[0], args[1]);
		 
//		 parseLog("C:\\Users\\bhoff\\Desktop\\2011Dec23ActivitySodo.txt", "C:\\Users\\bhoff\\Desktop\\2011Dec23MessagesSodo.txt");

//		 checkCheckSums(args[0], args[1], new String[]{"13388","13481","13406","13444","13358","14554","15015","14356","14975","15367","13783"});

//		 geoMigrator(args[0], args[1], 16114, 102610, 102611);
		
//		 updateLayerName(""+23994, "GSE10024", args[0], args[1]);
//		 deleteDatasets(new Integer[]{105645,105644,105643}, args[0], args[1]);
		 
//		 reconcileProject(102610, 102611, args[0], args[1], /*no GEO*/true);
		 
//		 parseLog(new String[]{"C:\\Users\\bhoff\\Desktop\\2012Jan02Activity.txt"}, 
//				 "C:\\Users\\bhoff\\Desktop\\2012Jan02Activity_Messages.xls");
		 
		 
		 
//		rerunCompletedLayers(new Integer[] {
//				13636,
//				13780,	 
//		 }, 102611, args[0], args[1]);

	}
}
