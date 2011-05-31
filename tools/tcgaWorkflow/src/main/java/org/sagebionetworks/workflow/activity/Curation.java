package org.sagebionetworks.workflow.activity;

import java.net.URL;

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
	private static final int datasetIndex = 6;
	private static final int layerTypeIndex = 7;
	private static final int expressionPlatformIndex = 9;
	private static final int expressionFilenameIndex = 11;

	/**
	 * TCGA URLs are well-formed.  Extract metadata from the path and filename
	 * portions of the TCGA URL.
	 * 
	 * @param datasetId
	 * @param tcgaUrl
	 * @return the layerId for the layer metadata created
	 * @throws Exception
	 */
	public static Integer doCreateMetadataForTcgaSourceLayer(Integer datasetId,
			String tcgaUrl) throws Exception {

		Integer layerId = -1;

		URL parsedUrl = new URL(tcgaUrl);
		String pathComponents[] = parsedUrl.getPath().split("/");

		if (!"coad".equals(pathComponents[datasetIndex])) {
			throw new UnrecoverableException(
					"only able to handle coad data right now: "
							+ pathComponents[datasetIndex]);
		}

		String layerName;
		String layerType;
		String layerPlatform;

		if ("cgcc".equals(pathComponents[layerTypeIndex])) {
			layerType = "E";

			if (!pathComponents[expressionFilenameIndex].endsWith(".tar.gz")) {
				throw new UnrecoverableException("malformed filename: "
						+ pathComponents[expressionFilenameIndex]);
			}

			// Use the filename portion of the url as the layer name, it
			// includes
			// the TCGA version so in the future when we have versioning in
			// place,
			// we may want to remove the TCGA version from the layer name and
			// bump
			// the revision of the layer when the TCGA version has changed
			layerName = pathComponents[expressionFilenameIndex].substring(0,
					pathComponents[expressionFilenameIndex].length()
							- ".tar.gz".length());
			layerPlatform = pathComponents[expressionPlatformIndex];

		} else if ("bcr".equals(pathComponents[layerTypeIndex])) {
			layerType = "C";
			// Use the last part of the url as the layer name, but get rid of
			// any .txt or .tar.gz endings
			String filename = pathComponents[pathComponents.length - 1];
			String filenameComponents[] = filename.split("\\.");
			layerName = filenameComponents[0];
			layerPlatform = null;
		} else {
			throw new UnrecoverableException(
					"only able to handle expression and clinical data right now: "
							+ pathComponents[layerTypeIndex]);
		}

		// Create a layer in Synapse for this raw data from TCGA, if one does
		// not already exist
		Synapse synapse = ConfigHelper.createConfig().createSynapseClient();
		JSONObject results = synapse
				.query("select * from layer where layer.parentId == "
						+ datasetId + " and layer.name == '" + layerName + "'");
		int numLayersFound = results.getInt("totalNumberOfResults");

		if (0 == numLayersFound) {
			JSONObject layer = new JSONObject();
			layer.put("name", layerName);
			layer.put("type", layerType);
			layer.put("platform", layerPlatform);
			String layerUri = "/dataset/" + datasetId + "/layer";

			// TODO put a unique constraint on the layer name, and if we catch
			// an exception here for that, we should retry this workflow step
			JSONObject storedLayer = synapse.createEntity(layerUri, layer);
			layerId = storedLayer.getInt("id");
		} else {
			if (1 == numLayersFound) {
				layerId = results.getJSONArray("results").getJSONObject(0)
						.getInt("layer.id");
			} else {
				throw new UnrecoverableException("We have " + numLayersFound
						+ " layers with name " + layerName);
			}
		}
		return layerId;
	}
}
