package org.sagebionetworks.workflow.curation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.sagebionetworks.utils.MD5ChecksumHelper;
import org.sagebionetworks.workflow.Constants;
import org.sagebionetworks.workflow.UnrecoverableException;

/**
 * Workflow activities relevant to curation - specifically the metadata creation
 * tasks that can be automated.
 * 
 * @author deflaux
 * 
 */
public class TcgaCuration {

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
	 * portions of the TCGA URL.
	 * 
	 * <domain>_<disease study>.<platform>.<serial index>.<revision>.<series>
	 * 
	 * @param doneIfExists
	 *            if true exit workflow early, otherwise proceed
	 * @param datasetId
	 * @param tcgaUrl
	 * @return the layerId for the layer metadata created
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws UnrecoverableException
	 * @throws NoSuchAlgorithmException
	 * @throws ClientProtocolException
	 * @throws SynapseException
	 * @throws JSONException
	 */
	public static String createMetadata(String datasetId, String tcgaUrl,
			Boolean doneIfExists) throws ClientProtocolException,
			NoSuchAlgorithmException, UnrecoverableException, IOException,
			HttpClientHelperException, SynapseException, JSONException {

		Map<String, String> metadata = formulateMetadataFromTcgaUrl(tcgaUrl);

		Synapse synapse = TcgaWorkflowConfigHelper.getSynapseClient();
		JSONObject results = synapse
				.query("select * from layer where layer.parentId == \""
						+ datasetId + "\" and layer.name == '"
						+ metadata.get("name") + "'");
		int numLayersFound = results.getInt("totalNumberOfResults");
		Layer layer = null;
		if (1 == numLayersFound) {
			layer = synapse.getEntity(results.getJSONArray("results")
					.getJSONObject(0).getString("layer.id"), Layer.class);
			if (doneIfExists && (null != layer.getMd5())
					&& (layer.getType() != LayerTypeNames.C)) {
				// if the layer has an md5 and isn't a clinical layer, if it
				// exists, no need to do anything else
				return Constants.WORKFLOW_DONE;
			}
		} else if (1 < numLayersFound) {
			throw new UnrecoverableException("We have " + numLayersFound
					+ " layers with name " + metadata.get("name"));
		} else {
			layer = new Layer();
			layer.setParentId(datasetId);
		}

		// Transfer primary field values, if this is an update and users
		// modified any of these by hand, they will be overwritten
		layer.setName(metadata.get("name"));
		layer.setStatus(metadata.get("status"));
		layer.setType(LayerTypeNames.valueOf(metadata.get("type")));
		if (metadata.containsKey("platform"))
			layer.setPlatform(metadata.get("platform"));

		// Create or update the layer in Synapse, as appropriate
		if (null == layer.getId()) {
			layer = synapse.createEntity(layer);
		} else {
			layer = synapse.putEntity(layer);
		}

		// Get the annotations container, and overwrite these annotations
		JSONObject annotations = synapse.getEntity(layer.getAnnotations());
		JSONObject stringAnnotations = annotations
				.getJSONObject("stringAnnotations");
		annotationHelper(metadata, stringAnnotations, "format");
		annotationHelper(metadata, stringAnnotations, "tcgaDomain");
		annotationHelper(metadata, stringAnnotations, "tcgaDiseaseStudy");
		annotationHelper(metadata, stringAnnotations, "tcgaLevel");
		annotationHelper(metadata, stringAnnotations, "tcgaArchiveSerialIndex");
		annotationHelper(metadata, stringAnnotations, "tcgaRevision");
		annotationHelper(metadata, stringAnnotations, "tcgaSeries");

		// Update the annotations in Synapse
		synapse.putEntity(layer.getAnnotations(), annotations);

		return layer.getId();
	}

	private static void annotationHelper(Map<String, String> metadata,
			JSONObject annots, String key) throws JSONException {
		if (!metadata.containsKey(key))
			return;

		JSONArray annotationValue = annots.optJSONArray(key);
		if (null == annotationValue) {
			annotationValue = new JSONArray();
		}
		annotationValue.put(0, metadata.get(key));
		annots.put(key, annotationValue);
	}

	/**
	 * Given a layer id and a TCGA source url, update the location
	 * 
	 * @param tcgaUrl
	 * @param layerId
	 * @return whether we actually needed to update the location
	 * @throws ClientProtocolException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 * @throws SynapseException
	 * @throws JSONException
	 */
	public static boolean updateLocation(String tcgaUrl, String layerId)
			throws ClientProtocolException, NoSuchAlgorithmException,
			UnrecoverableException, IOException, HttpClientHelperException,
			SynapseException, JSONException {

		Synapse synapse = TcgaWorkflowConfigHelper.getSynapseClient();
		Layer layer = synapse.getEntity(layerId, Layer.class);
		String md5;

		try {
			String md5FileContents = HttpClientHelper.getContent(
					TcgaWorkflowConfigHelper.getHttpClient(), tcgaUrl + ".md5");
			String fileInfo[] = md5FileContents.split("\\s+");
			if (2 != fileInfo.length) {
				throw new UnrecoverableException(
						"malformed md5 file from tcga: " + md5FileContents);
			}
			md5 = fileInfo[0];
			if (md5.equals(layer.getMd5())) {
				return false;
			}

			layer.setMd5(md5);
			List<LocationData> locations = new ArrayList<LocationData>();
			LocationData location = new LocationData();
			location.setPath(tcgaUrl);
			location.setType(LocationTypeNames.external);
			locations.add(location);
			layer.setLocations(locations);
			layer = synapse.putEntity(layer);

		} catch (HttpClientHelperException e) {
			// 404s are okay, not all TCGA files have a corresponding md5 file
			// (e.g., clinical data), later on we will download the file and
			// compute the md5 checksum
			if (404 != e.getHttpStatus()) {
				throw e;
			}
			// If this was unversionsed data from TCGA, download it and import
			// it to our datastore
			URL parsedUrl = new URL(tcgaUrl);
			String originalFilename = parsedUrl.getPath().substring(
					parsedUrl.getPath().lastIndexOf("/") + 1);
			File tempFile = File.createTempFile("tcga", originalFilename);
			tempFile = HttpClientHelper.getContent(TcgaWorkflowConfigHelper
					.getHttpClient(), tcgaUrl, tempFile);
			md5 = MD5ChecksumHelper.getMD5Checksum(tempFile.getAbsolutePath());

			if (md5.equals(layer.getMd5())) {
				String currentLocation = layer.getLocations().get(0).getPath();
				if (-1 < currentLocation.indexOf(originalFilename)) {
					// The MD5 has not changed and we don't need to repair the
					// filepath due to bug PLFM-1110 so just skip the upload
					return false;
				}
			}

			layer = (Layer) synapse.uploadLocationableToSynapse(layer,
					tempFile, md5);
			tempFile.delete();
		}

		return true;
	}

	/**
	 * Given a TCGA url, parse it to extract metadata out of it
	 * 
	 * @param tcgaUrl
	 * @return a map holding all the metadata we could reverse engineer from the
	 *         TCGA Url
	 * @throws UnrecoverableException
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws ClientProtocolException
	 */
	public static Map<String, String> formulateMetadataFromTcgaUrl(
			String tcgaUrl) throws UnrecoverableException,
			ClientProtocolException, NoSuchAlgorithmException, IOException,
			HttpClientHelperException {

		Map<String, String> metadata = new HashMap<String, String>();

		URL parsedUrl = new URL(tcgaUrl);
		String pathComponents[] = parsedUrl.getPath().split("/");
		String filename = pathComponents[pathComponents.length - 1];

		metadata.put("status", TCGA_STATUS);
		metadata.put("format", TCGA_FORMAT);

		// This is in-elegant logic to infer some metadata from a TCGA url
		if (("cgcc".equals(pathComponents[LAYER_TYPE_INDEX]))
				|| ("gsc".equals(pathComponents[LAYER_TYPE_INDEX]))) {

			if (!filename.endsWith(".tar.gz")) {
				throw new UnrecoverableException("malformed filename: "
						+ filename);
			}

			// Use the filename portion of the url as the layer name, it
			// includes the TCGA version so in the future when we have
			// versioning in place, we may want to remove the TCGA version from
			// the layer name and bump the revision of the layer when the TCGA
			// version has changed
			metadata.put("name", filename.substring(0, filename.length()
					- ".tar.gz".length()));

			Matcher matcher = TCGA_DATA_REGEXP.matcher(filename);
			if (matcher.matches()) {
				metadata.put("tcgaDomain", matcher.group(1));
				metadata.put("tcgaDiseaseStudy", matcher.group(2));
				metadata.put("platform", matcher.group(3));
				metadata.put("tcgaLevel", matcher.group(4));
				metadata.put("tcgaArchiveSerialIndex", matcher.group(5));
				metadata.put("tcgaRevision", matcher.group(6));
				metadata.put("tcgaSeries", matcher.group(7));
			} else {
				metadata.put("platform", pathComponents[PLATFORM_INDEX]);
			}

			if ((-1 < tcgaUrl.indexOf("snp"))
					|| (-1 < tcgaUrl.indexOf("DNASeq"))) {
				metadata.put("type", LayerTypeNames.G.name());
			} else {
				metadata.put("type", LayerTypeNames.E.name());
			}
		} else if ("bcr".equals(pathComponents[LAYER_TYPE_INDEX])) {
			metadata.put("type", LayerTypeNames.C.name());
			// Use the last part of the url as the layer name, but get rid of
			// any .txt or .tar.gz endings
			String filenameComponents[] = filename.split("\\.");
			metadata.put("name", filenameComponents[0]);
		} else {
			throw new UnrecoverableException(
					"Not yet able to form metadata for data of type(" + tcgaUrl
							+ "): " + pathComponents[LAYER_TYPE_INDEX]);
		}

		return metadata;
	}

	/**
	 * Given a layerId, formulate a human readable notification message
	 * 
	 * @param layerId
	 * @return a somewhat helpful message for use in notifications :-)
	 * @throws SynapseException
	 * @throws UnrecoverableException
	 * @throws JSONException
	 */
	public static String formulateLayerNotificationMessage(String layerId)
			throws SynapseException, JSONException, UnrecoverableException {
		StringBuilder message = new StringBuilder();

		Synapse synapse = TcgaWorkflowConfigHelper.getSynapseClient();
		JSONObject layerResults = synapse
				.query("select * from layer where layer.id == \"" + layerId
						+ "\"");
		if (0 == layerResults.getInt("totalNumberOfResults")) {
			throw new UnrecoverableException(
					"Unable to formulate message for layer " + layerId);
		}
		JSONObject layerQueryResult = layerResults.getJSONArray("results")
				.getJSONObject(0);

		JSONObject datasetResults = synapse
				.query("select * from dataset where dataset.id == \""
						+ layerQueryResult.getString("layer.parentId") + "\"");
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
		message.append("\n").append(
				TcgaWorkflowConfigHelper.getPortalEndpoint())
				.append("/#Layer:").append(layerQueryResult.get("layer.id"))
				.append(";Dataset:").append(
						layerQueryResult.get("layer.parentId"));
		message.append("\n\nLayer\n").append(layerResults.toString(4));
		message.append("\n\nDataset\n").append(datasetResults.toString(4));
		return message.toString();
	}
}
