package org.sagebionetworks.workflow.activity;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.net.BCodec;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.workflow.curation.ConfigHelper;

/**
 * Workflow activities relevant to synapse storage.
 * 
 * TODO move the implementations here into the Synapse client.
 * 
 * TODO if/when we want additional storage mechanisms, use the strategy pattern
 * here for S3, Google Storage, Sage Bionetworks local storage, etc. These
 * activity implementations must be static so we cannot use interfaces.
 * 
 * @author deflaux
 * 
 */
public class Storage {

	/**
	 * @param datasetId
	 * @param layerId
	 * @param localFilepath
	 * @param md5
	 * @throws Exception
	 * @throws Exception
	 */
	public static final void doUploadLayerToStorage(Integer datasetId,
			Integer layerId, String localFilepath, String md5) throws Exception {

		Synapse synapse = ConfigHelper.createConfig().createSynapseClient();
		String datasetUri = "/dataset/" + datasetId;

		File file = new File(localFilepath);
		String s3Path = file.getName();


		JSONObject s3LocationRequest = new JSONObject();
		s3LocationRequest.put("path", s3Path);
		s3LocationRequest.put("md5sum", md5);
		String layerS3LocationUri = datasetUri + "/layer/" + layerId
				+ "/awsS3Location";
		JSONObject s3Location = synapse.createEntity(layerS3LocationUri,
				s3LocationRequest);

		// TODO can we do a conditional PUT, fail if the file already exists?
		Map<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("x-amz-acl", "bucket-owner-full-control");
		// TODO find a more direct way to go from hex to base64
		// Base64.encode(Hex.decodeHex(md5.toCharArray())));
		BCodec encoder = new BCodec();
		headerMap.put("Content-MD5", encoder.encode(md5));
		HttpClientHelper.uploadFile(s3Location.getString("path"),
				localFilepath, headerMap);
	}

}
