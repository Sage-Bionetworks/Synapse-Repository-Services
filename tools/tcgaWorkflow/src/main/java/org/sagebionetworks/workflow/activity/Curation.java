package org.sagebionetworks.workflow.activity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.sagebionetworks.workflow.UnrecoverableException;
import org.sagebionetworks.workflow.activity.DataIngestion.DownloadResult;
import org.sagebionetworks.workflow.curation.ConfigHelper;

/**
 * Workflow activities relevant to curation - specifically the metadata creation
 * tasks that can be automated.
 * 
 * @author deflaux
 * 
 */
public class Curation {
	private static final String TCGA_STATUS = "raw";
	private static final String TCGA_FORMAT = "tsv";
	private static final int LAYER_TYPE_INDEX = 7;
	private static final int PLATFORM_INDEX = 9;

	/**
	 * <domain>_<disease study>.<platform>.<level>.<revision>
	 */
	public static final Pattern TCGA_DATA_REGEXP = Pattern
			.compile("^([^_]+)_([^\\.]+)\\.([^\\.]+)\\.([^\\.]+)\\.(\\d+)\\.(\\d+)\\.(\\d+).*");
	
	/**
	 * Create or update metadata for TCGA layers
	 * 
	 * TCGA URLs are well-formed. Extract metadata from the path and filename
	 * portions of the TCGA URL and also the md5checksum file.
	 * 
	 * <domain>_<disease study>.<platform>.<serial index>.<revision>.<series>
	 * 
	 * @param doneIfExists if true exit workflow early, otherwise proceed
	 * @param datasetId
	 * @param tcgaUrl
	 * @return the layerId for the layer metadata created
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static String doCreateSynapseMetadataForTcgaSourceLayer(
			Boolean doneIfExists, String datasetId, String tcgaUrl)
			throws Exception {

		JSONObject layerAnnotations = new JSONObject();
		JSONObject layer = formulateLayerMetadataFromTcgaUrl(tcgaUrl,
				layerAnnotations);
		JSONObject location = formulateLocationMetadataFromTcgaUrl(tcgaUrl);
		JSONObject storedLayer = null;
		JSONObject storedLocation = null;

		Synapse synapse = ConfigHelper.createSynapseClient();
		JSONObject results = synapse
				.query("select * from layer where layer.parentId == "
						+ datasetId + " and layer.name == '"
						+ layer.getString("name") + "'");
		int numLayersFound = results.getInt("totalNumberOfResults");
		if (1 == numLayersFound) {
			// TODO revise this layer here
			storedLayer = synapse.updateEntity("/layer/"
					+ results.getJSONArray("results").getJSONObject(0)
					.getString("layer.id"), layer);
			JSONObject locations = synapse.getEntity(storedLayer
					.getString("locations"));
			for (int i = 0; i < locations.getInt("totalNumberOfResults"); i++) {
				JSONObject currentLocation = locations.getJSONArray("results")
						.getJSONObject(i);
				if (location.getString("path").equals(
						currentLocation.getString("path"))) {
					if (location.getString("md5sum").equals(
							currentLocation.getString("md5sum"))) {
						storedLocation = currentLocation;
						break;
					} else {
						// TODO revise this location once we start using versioning
						storedLocation = synapse.updateEntity(currentLocation.getString("uri"), location);
					}
				}
			}
		} else if (1 < numLayersFound) {
			throw new UnrecoverableException("We have " + numLayersFound
					+ " layers with name " + layer.getString("name"));
		}

		if (doneIfExists && (null != storedLayer) && (null != storedLocation)) {
			return Constants.WORKFLOW_DONE;
		}

		if (null == storedLayer) {
			// TODO put a unique constraint on the layer name, and if we catch
			// an exception here for that, we should retry this workflow step
			layer.put("parentId", datasetId);
			storedLayer = synapse.createEntity("/layer", layer);
		}

		// TODO write a helper for this and stick it in the Synapse client
		JSONObject stringAnnotations = new JSONObject();
		Iterator<String> iter = layerAnnotations.keys();
		while (iter.hasNext()) {
			String annotationKey = iter.next();
			JSONArray annotationValue = new JSONArray();
			annotationValue.put(layerAnnotations.getString(annotationKey));
			stringAnnotations.put(annotationKey, annotationValue);
		}
		JSONObject annotations = new JSONObject();
		annotations.put("stringAnnotations", stringAnnotations);

		synapse.updateEntity(storedLayer.getString("annotations"), annotations);

		if (null == storedLocation) {
			location.put("parentId", storedLayer.getString("id"));
			synapse.createEntity("/location", location);
		}

		return storedLayer.getString("id");
	}

	/**
	 * @param tcgaUrl
	 * @param annotations
	 *            output parameter!!!
	 * @return a JSONObject holding all the metadata we could reverse engineer
	 *         from the TCGA Url
	 * @throws UnrecoverableException
	 * @throws MalformedURLException
	 * @throws JSONException
	 */
	public static JSONObject formulateLayerMetadataFromTcgaUrl(String tcgaUrl,
			JSONObject annotations) throws UnrecoverableException,
			MalformedURLException, JSONException {

		URL parsedUrl = new URL(tcgaUrl);
		String pathComponents[] = parsedUrl.getPath().split("/");
		String filename = pathComponents[pathComponents.length - 1];

		JSONObject layer = new JSONObject();
		layer.put("status", TCGA_STATUS);
		annotations.put("format", TCGA_FORMAT);

		// This is in-elegant logic to infer some metadata from a TCGA url
		if (("cgcc".equals(pathComponents[LAYER_TYPE_INDEX])) || ("gsc".equals(pathComponents[LAYER_TYPE_INDEX]))) {

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
				annotations.put("tcgaDomain", matcher.group(1));
				annotations.put("tcgaDiseaseStudy", matcher.group(2));
				layer.put("platform", matcher.group(3));
				annotations.put("tcgaLevel", matcher.group(4));
				annotations.put("tcgaArchiveSerialIndex", matcher.group(5));
				annotations.put("tcgaRevision", matcher.group(6));
				annotations.put("tcgaSeries", matcher.group(7));
			} else {
				layer.put("platform", pathComponents[PLATFORM_INDEX]);
			}

			if ((-1 < tcgaUrl.indexOf("snp")) || (-1 < tcgaUrl.indexOf("DNASeq"))) {
				layer.put("type", "G");
			} else {
				layer.put("type", "E");
			}
		} else if ("bcr".equals(pathComponents[LAYER_TYPE_INDEX])) {
			layer.put("type", "C");
			// Use the last part of the url as the layer name, but get rid of
			// any .txt or .tar.gz endings
			String filenameComponents[] = filename.split("\\.");
			layer.put("name", filenameComponents[0]);
		} else {
			throw new UnrecoverableException(
					"Not yet able to form metadata for data of type(" + tcgaUrl + "): "
							+ pathComponents[LAYER_TYPE_INDEX]);
		}

		return layer;
	}

	/**
	 * @param tcgaUrl
	 * @return location metadata
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws UnrecoverableException
	 * @throws HttpClientHelperException
	 * @throws JSONException
	 * @throws NoSuchAlgorithmException
	 */
	public static JSONObject formulateLocationMetadataFromTcgaUrl(String tcgaUrl)
			throws ClientProtocolException, IOException,
			UnrecoverableException, HttpClientHelperException, JSONException,
			NoSuchAlgorithmException {
		JSONObject location = new JSONObject();
		String md5checksum = null;
		try {
			String md5FileContents = HttpClientHelper.getFileContents(tcgaUrl
					+ ".md5");

			// TODO put a real regexp here to validate the format
			String fileInfo[] = md5FileContents.split("\\s+");
			if (2 != fileInfo.length) {
				throw new UnrecoverableException(
						"malformed md5 file from tcga: " + md5FileContents);
			}
			md5checksum = fileInfo[0];
		} catch (HttpClientHelperException e) {
			if (404 == e.getHttpStatus()) {
				// 404s are okay, not all TCGA files have a corresponding md5
				// file (e.g., clinical data), download the file and compute the
				// md5 checksum
				DownloadResult downloadResult = DataIngestion
						.doDownloadFromTcga(tcgaUrl);
				md5checksum = downloadResult.getMd5();
			} else {
				throw e;
			}
		}

		location.put("path", tcgaUrl);
		location.put("md5sum", md5checksum);
		location.put("type", "external"); // TODO use model object here?
		return location;
	}

	/**
	 * @param layerId
	 * @return a somewhat helpful message for use in notifications :-)
	 * @throws Exception
	 */
	public static String formulateLayerCreationMessage(String layerId)
			throws Exception {
		StringBuilder message = new StringBuilder();

		Synapse synapse = ConfigHelper.createSynapseClient();
		JSONObject layerResults = synapse
				.query("select * from layer where layer.id == " + layerId);
		if (0 == layerResults.getInt("totalNumberOfResults")) {
			throw new UnrecoverableException(
					"Unable to formulate message for layer " + layerId);
		}
		JSONObject layerQueryResult = layerResults.getJSONArray("results")
				.getJSONObject(0);

		JSONObject datasetResults = synapse
				.query("select * from dataset where dataset.id == "
						+ layerQueryResult.getString("layer.parentId"));
		if (0 == datasetResults.getInt("totalNumberOfResults")) {
			throw new UnrecoverableException(
					"Unable to formulate message for dataset "
							+ layerQueryResult.getString("layer.parentId")
							+ " and layer " + layerId);
		}

		message.append("Created new layer ").append(
				layerQueryResult.get("layer.name"));
		message.append(" for dataset ").append(
				datasetResults.getJSONArray("results").getJSONObject(0).get(
						"dataset.name"));
		message.append(
				"\n").append(ConfigHelper.getPortalEndpoint()).append("/#Layer:")
				.append(layerQueryResult.get("layer.id")).append(";Dataset:")
				.append(layerQueryResult.get("layer.parentId"));
		message.append("\n\nLayer\n").append(layerResults.toString(4));
		message.append("\n\nDataset\n").append(datasetResults.toString(4));
		return message.toString();
	}
}
