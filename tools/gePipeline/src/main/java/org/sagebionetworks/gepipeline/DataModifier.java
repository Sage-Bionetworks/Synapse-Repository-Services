package org.sagebionetworks.gepipeline;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.utils.HttpClientHelper;

/**
 * This class is for modifying data created by the GEPipeline workflow
 */
public class DataModifier {

	private static final String REPO_ENDPOINT = "https://repo-alpha.sagebase.org/repo/v1";
	private static final String AUTH_ENDPOINT = "https://auth-alpha.sagebase.org/auth/v1";

	private static final String DATASET_NOS_TAG = "Number_of_Samples";
	private static final String LAYER_NOS_TAG = "numSamples";
	private static final String INTERIM_NOS_TAG = "number_of_samples";
	
	/**
	 * The R client cannot currently write the datasets' number of samples into the Long field meant for this purpose.
	 * As a workaround we have the client write into another, String field, and use this ad hoc process to copy the
	 * data into the Long field.
	 */
	public static void updateNOSAnnotations(String user, String pw) throws Exception {
		Synapse synapse = new Synapse();
		HttpClientHelper.setGlobalConnectionTimeout(30000);
		HttpClientHelper.setGlobalSocketTimeout(30000);
		synapse.setAuthEndpoint(AUTH_ENDPOINT);
		synapse.setRepositoryEndpoint(REPO_ENDPOINT);

		synapse.login(user, pw);
		int offset=1;
		int total=0;
		int batchSize = 20;
		do {
			try {
				JSONObject o = synapse.query("select * from dataset where parentId==16114 ORDER BY Number_of_Samples LIMIT "+batchSize+" OFFSET "+offset);
				total = (int)o.getLong("totalNumberOfResults");
				System.out.println(""+offset+"->"+(offset+batchSize-1)+" of "+total);
				JSONArray a = o.getJSONArray("results");
	//			System.out.println("this batch has "+a.length()+" result(s) out of "+total+".");
				for (int i=0; i<a.length(); i++) {
					JSONObject ds = (JSONObject)a.get(i);
					String id = ds.getString("dataset.id");
					String name = ds.getString("dataset.name");
					String annotUri = "/dataset/"+id+"/annotations";
					JSONObject annots = synapse.getSynapseEntity(REPO_ENDPOINT, annotUri);
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
			synapse.updateSynapseEntity(REPO_ENDPOINT, annotUri, newAnnots);
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
		Synapse synapse = new Synapse();
		HttpClientHelper.setGlobalConnectionTimeout(30000);
		HttpClientHelper.setGlobalSocketTimeout(30000);
		synapse.setAuthEndpoint(AUTH_ENDPOINT);
		synapse.setRepositoryEndpoint(REPO_ENDPOINT);

		synapse.login(user, pw);
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
									} catch (IOException ioe) {
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
	
	public static void main(String[] args) throws Exception {
		updateNOSAnnotations(args[0], args[1]);
//		deleteStaleProvenanceString(args[0], args[1]);
	}
}
