package org.sagebionetworks.client;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;

public class SynapseWikiGenerator extends SynapseAdministration {

	private static final Logger log = Logger
			.getLogger(SynapseWikiGenerator.class.getName());

	protected JSONObject dispatchSynapseRequest(String endpoint, String uri,
			String requestMethod, String requestContent,
			Map<String, String> requestHeaders) throws SynapseException {

		try {
			JSONObject requestObject = null;
			String curl = "curl -i ";

			if (null != requestContent) {
				requestObject = new JSONObject(requestContent);
			}

			if ("POST".equals(requestMethod)) {
				curl += " -d '" + requestObject.toString(JSON_INDENT) + "' ";
			} else if ("PUT".equals(requestMethod)) {
				curl += " -X PUT -d '" + requestObject.toString(JSON_INDENT)
						+ "' ";
			} else if ("DELETE".equals(requestMethod)) {
				curl += " -X DELETE ";
			}

			for (Entry<String, String> header : requestHeaders.entrySet()) {
				curl += " -H " + header.getKey() + ":" + header.getValue();
			}
			curl += " '" + uri + "'{code}";
			log.info("*Request* {code}" + curl);
			log.info("*Response* {code}");

			JSONObject responseObject = super.dispatchSynapseRequest(endpoint,
					uri, requestMethod, requestContent, requestHeaders);

			log.info(responseObject.toString(JSON_INDENT) + "{code}");

			return responseObject;
		} catch (JSONException e) {
			throw new SynapseException(e);
		}
	}

}
