package org.sagebionetworks.workflow.activity;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
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

	private static final Logger log = Logger.getLogger(Storage.class
			.getName());
	
	/**
	 * @param datasetId
	 * @param layerId
	 * @param localFilepath
	 * @param md5
	 * @throws Exception
	 * @throws Exception
	 */
	public static final void doUploadLayerToStorage(String datasetId,
			String layerId, String localFilepath, String md5) throws Exception {

		Synapse synapse = ConfigHelper.createSynapseClient();

		// See if the file is already on S3
//		JSONObject layer = synapse.getEntity("/layer/" + layerId);
//		JSONObject location = synapse.getEntity(uri)if(layer.has("locations"))
		
		File file = new File(localFilepath);
		String s3Path = file.getName();

		JSONObject s3LocationRequest = new JSONObject();
		s3LocationRequest.put("path", "/tcga/" + layerId + "/" + s3Path);  // See PLFM-212
		s3LocationRequest.put("md5sum", md5);
		s3LocationRequest.put("parentId", layerId);
		s3LocationRequest.put("type", "awss3");
		JSONObject s3Location = synapse.createEntity("/location",
				s3LocationRequest);

		// TODO find a more direct way to go from hex to base64
		byte[] encoded = Base64.encodeBase64(Hex.decodeHex(md5.toCharArray()));
		String base64Md5 = new String(encoded, "ASCII");
		
		Map<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("x-amz-acl", "bucket-owner-full-control");
		headerMap.put("Content-MD5", base64Md5);
		headerMap.put("Content-Type", "application/binary");
		
//		if(log.isDebugEnabled()) {
			log.warn("curl -v -X PUT -H Content-MD5:" + base64Md5
            + " -H x-amz-acl:bucket-owner-full-control " 
            + " -H Content-Type:application/binary "
            + " --data-binary @" + localFilepath
            + " '" + s3Location.getString("path") + "'");
//		}
	
		HttpClientHelper.uploadFile(s3Location.getString("path"),
				localFilepath, headerMap);
	}

}
