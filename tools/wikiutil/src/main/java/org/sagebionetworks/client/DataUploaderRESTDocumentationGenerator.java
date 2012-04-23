package org.sagebionetworks.client;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.sagebionetworks.client.DataUploaderImpl;
import org.sagebionetworks.client.SynapseRESTDocumentationGenerator.MARKUP;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.S3Token;

/**
 * @author deflaux
 *
 */
public class DataUploaderRESTDocumentationGenerator extends DataUploaderImpl {

	private static final Logger log = Logger
			.getLogger(DataUploaderRESTDocumentationGenerator.class.getName());

	private SynapseRESTDocumentationGenerator.MARKUP markup;

	/**
	 * @param markup
	 */
	public DataUploaderRESTDocumentationGenerator(
			SynapseRESTDocumentationGenerator.MARKUP markup) {
		this.markup = markup;
	}

	@Override
	public void uploadDataMultiPart(S3Token s3Token, File dataFile)
			throws SynapseException {

		byte[] encoded;
		String base64Md5;
		try {
			encoded = Base64.encodeBase64(Hex.decodeHex(s3Token.getMd5()
					.toCharArray()));
			base64Md5 = new String(encoded, "ASCII");
		} catch (Exception e) {
			throw new SynapseException(e);
		}

		Map<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("x-amz-acl", "bucket-owner-full-control");
		headerMap.put("Content-MD5", base64Md5);
		headerMap.put("Content-Type", s3Token.getContentType());

		String curl = "curl -i ";

		for (Entry<String, String> header : headerMap.entrySet()) {
			curl += " -H " + header.getKey() + ":" + header.getValue();
		}

		curl += " '" + s3Token.getPresignedUrl() + "'";

		if (markup.equals(SynapseRESTDocumentationGenerator.MARKUP.WIKI)) {
			log.info("*Request* {code}" + curl + "{code}");
			log.info("*Response* {code}");
		} else if (markup.equals(SynapseRESTDocumentationGenerator.MARKUP.HTML)) {
			log.info("<br><span class=\"request\">Request</span> <pre>" + curl
					+ "</pre><br>");
			log.info("<span class=\"response\">Response</span> <pre>");
		} else {
			log.info("REQUEST " + curl + "");
			log.info("RESPONSE");
		}

		super.uploadDataSingle(s3Token, dataFile);

		String response = "";
		if (markup.equals(SynapseRESTDocumentationGenerator.MARKUP.WIKI)) {
			log.info(response + "{code}");
		} else if (markup.equals(SynapseRESTDocumentationGenerator.MARKUP.HTML)) {
			log.info(response + "</pre><br>");
		} else {
			log.info(response);
			log.info("");
		}

	}

}
