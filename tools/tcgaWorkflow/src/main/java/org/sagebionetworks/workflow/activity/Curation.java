package org.sagebionetworks.workflow.activity;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.workflow.UnrecoverableException;
import org.sagebionetworks.workflow.curation.ConfigHelper;

/**
 * Workflow activities relevant to curation - specifically the metadata creation
 * tasks that can be automated.
 * 
 * @author deflaux
 * 
 */
public class Curation {
	private static final int layerTypeIndex = 7;
	private static final int expressionPlatformIndex = 9;
	
	/**
	 * <domain>_<disease study>.<platform>.<level>.<revision>
	 */
	public static final Pattern TCGA_DATA_REGEXP = Pattern
			.compile("^([^_]+)_([^\\.]+)\\.([^\\.]+)\\.([^\\.]+)\\.(\\d+\\.\\d+\\.\\d+).*");

	/**
	 * TCGA URLs are well-formed. Extract metadata from the path and filename
	 * portions of the TCGA URL.
	 * 
	 * <domain>_<disease study>.<platform>.<serial index>.<revision>.<series>
	 * 
	 * @param datasetId
	 * @param tcgaUrl
	 * @return the layerId for the layer metadata created
	 * @throws Exception
	 */
	public static Integer doCreateSynapseMetadataForTcgaSourceLayer(
			Integer datasetId, String tcgaUrl) throws Exception {

		Integer layerId = -1;

		JSONObject layer = formulateLayerMetadataFromTcgaUrl(tcgaUrl);

		// Create a layer in Synapse for this raw data from TCGA, if one does
		// not already exist
		Synapse synapse = ConfigHelper.createConfig().createSynapseClient();
		JSONObject results = synapse
				.query("select * from layer where layer.parentId == "
						+ datasetId + " and layer.name == '"
						+ layer.getString("name") + "'");
		int numLayersFound = results.getInt("totalNumberOfResults");

		if (0 == numLayersFound) {
			String layerUri = "/dataset/" + datasetId + "/layer";

			// TODO put a unique constraint on the layer name, and if we catch
			// an exception here for that, we should retry this workflow step
			layer.put("parentId", datasetId);
			JSONObject storedLayer = synapse.createEntity(layerUri, layer);
			layerId = storedLayer.getInt("id");
		} else {
			if (1 == numLayersFound) {
				layerId = results.getJSONArray("results").getJSONObject(0)
						.getInt("layer.id");
			} else {
				throw new UnrecoverableException("We have " + numLayersFound
						+ " layers with name " + layer.getString("name"));
			}
		}
		return layerId;
	}

	/**
	 * @param tcgaUrl
	 * @return a JSONObject holding all the metadata we could reverse engineer from the TCGA Url
	 * @throws UnrecoverableException
	 * @throws MalformedURLException
	 * @throws JSONException
	 */
	public static JSONObject formulateLayerMetadataFromTcgaUrl(String tcgaUrl)
			throws UnrecoverableException, MalformedURLException, JSONException {

		URL parsedUrl = new URL(tcgaUrl);
		String pathComponents[] = parsedUrl.getPath().split("/");
		String filename = pathComponents[pathComponents.length - 1];

		JSONObject layer = new JSONObject();

		if ("cgcc".equals(pathComponents[layerTypeIndex])) {
			layer.put("type", "E");

			if (!filename.endsWith(".tar.gz")) {
				throw new UnrecoverableException("malformed filename: "
						+ filename);
			}

			// Use the filename portion of the url as the layer name, it
			// includes the TCGA version so in the future when we have
			// versioning in place, we may want to remove the TCGA version from
			// the layer name and bump the revision of the layer when the TCGA
			// version has changed
			layer.put("name", filename.substring(0, filename.length()
					- ".tar.gz".length()));

			Matcher matcher = TCGA_DATA_REGEXP.matcher(filename);
			if (matcher.matches()) {
				// TODO move commented out stuff to layer annotations
//				layer.put("domain", matcher.group(1));
//				layer.put("diseaseStudy", matcher.group(2));
				layer.put("platform", matcher.group(3));
//				layer.put("level", matcher.group(4));
//				layer.put("revision", matcher.group(5));
			} else {
				layer.put("platform", pathComponents[expressionPlatformIndex]);
			}

		} else if ("bcr".equals(pathComponents[layerTypeIndex])) {
			layer.put("type", "C");
			// Use the last part of the url as the layer name, but get rid of
			// any .txt or .tar.gz endings
			String filenameComponents[] = filename.split("\\.");
			layer.put("name", filenameComponents[0]);
		} else {
			throw new UnrecoverableException(
					"only able to handle expression and clinical data right now: "
							+ pathComponents[layerTypeIndex]);
		}

		return layer;
	}

	/**
	 * @param layerId
	 * @return a somewhat helpful message for use in notifications :-)
	 * @throws Exception
	 */
	public static String formulateLayerCreationMessage(Integer layerId) throws Exception {
		StringBuilder message = new StringBuilder();
		
		Synapse synapse = ConfigHelper.createConfig().createSynapseClient();
		JSONObject layerResults = synapse
		.query("select * from layer where layer.id == "
				+ layerId);
		if(0 == layerResults.getInt("totalNumberOfResults")) {
			throw new UnrecoverableException("Unable to formulate message for layer " + layerId);
		}
		JSONObject layerQueryResult = layerResults.getJSONArray("results").getJSONObject(0);
		
		JSONObject datasetResults = synapse
				.query("select * from dataset where dataset.id == "
						+ layerQueryResult.getString("layer.parentId"));
		if(0 == datasetResults.getInt("totalNumberOfResults")) {
			throw new UnrecoverableException("Unable to formulate message for dataset " + layerQueryResult.getString("layer.parentId")+ " and layer " + layerId);
		}

		message.append("Created new layer ").append(layerQueryResult.get("layer.name"));
		message.append(" for dataset ").append(datasetResults.getJSONArray("results").getJSONObject(0).get("dataset.name"));
		message.append("\n\nLayer\n").append(layerResults.toString(4));
		message.append("\n\nDataset\n").append(datasetResults.toString(4));
		return message.toString();
	}
}
