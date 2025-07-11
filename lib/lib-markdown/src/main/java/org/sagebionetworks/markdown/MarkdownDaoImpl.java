package org.sagebionetworks.markdown;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;

public class MarkdownDaoImpl implements MarkdownDao{

	public static final String MARKDOWN = "markdown";
	public static final String OUTPUT = "output";
	public static final String RESULT = "result";
	public static final String BASE_URL = "baseURL";
	public static final String FUNCTION_NAME = "dev-markdown-it-function-mdlambda-i5s7dyGFXgaC";

	@Autowired
	private AWSLambda lambdaClient;

	String synapseBaseUrl;

	public void setSynapseBaseUrl(String synapseBaseUrl) {
		this.synapseBaseUrl = synapseBaseUrl;
	}

	@Override
	public String convertMarkdown(String rawMarkdown, String outputType) throws JSONException, MarkdownClientException {
		if (rawMarkdown == null) {
			throw new IllegalArgumentException("rawMarkdown cannot be null");
		}
		if (outputType == null) {
			throw new IllegalArgumentException("outputType cannot be null");
		}
		JSONObject request = new JSONObject();
		request.put(MARKDOWN, rawMarkdown);
		request.put(BASE_URL, synapseBaseUrl);
		request.put(OUTPUT, outputType);
		String response = convertToMarkdownWithLambda(request.toString());

		return response;
	}

	private String convertToMarkdownWithLambda(String request) throws MarkdownClientException {
		try {
			InvokeRequest invokeRequest = new InvokeRequest()
					.withFunctionName(FUNCTION_NAME)
					.withPayload(request);

			InvokeResult result = lambdaClient.invoke(invokeRequest);

			if (result.getFunctionError() != null) {
				throw new MarkdownClientException(500, "Lambda execution failed: " + result.getFunctionError());
			}

			String responseData = new String(result.getPayload().array(), StandardCharsets.UTF_8);
			JSONObject responseJson = new JSONObject(responseData);

			return responseJson.getString(RESULT);
		} catch (AWSLambdaException | JSONException e) {
			throw new MarkdownClientException(e);
		}

    }

}
