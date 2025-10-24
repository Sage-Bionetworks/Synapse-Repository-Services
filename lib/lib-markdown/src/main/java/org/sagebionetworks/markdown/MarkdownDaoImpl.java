package org.sagebionetworks.markdown;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

import java.nio.charset.StandardCharsets;

@Service
public class MarkdownDaoImpl implements MarkdownDao{

	public static final String MARKDOWN = "markdown";
	public static final String OUTPUT = "output";
	public static final String RESULT = "result";
	public static final String BASE_URL = "baseURL";
	public static final String FUNCTION_NAME_FMT = "%s-markdownit:prod";

	private LambdaClient lambdaClient;
	private String synapseBaseUrl;
	private String stack;

	public MarkdownDaoImpl(LambdaClient lambdaClient, String synapseBaseUrl, String stack) {
		this.lambdaClient = lambdaClient;
		this.synapseBaseUrl = synapseBaseUrl;
		this.stack = stack;
	}

//	public void setLambdaClient(LambdaClient lambdaClient) { this.lambdaClient = lambdaClient; }
//	public void setSynapseBaseUrl(String synapseBaseUrl) {
//		this.synapseBaseUrl = synapseBaseUrl;
//	}
//	public void setStack(String stack) { this.stack = stack; }

	@Override
	public String convertMarkdown(String rawMarkdown, String outputType) throws JSONException, MarkdownClientException {
		if (rawMarkdown == null) {
			throw new IllegalArgumentException("rawMarkdown cannot be null");
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
			InvokeRequest invokeRequest = InvokeRequest.builder()
					.functionName(String.format(FUNCTION_NAME_FMT, stack))
					.payload(SdkBytes.fromUtf8String(request))
					.build();

			InvokeResponse response = lambdaClient.invoke(invokeRequest);

			if (response.functionError() != null) {
				throw new MarkdownClientException(500, "Lambda execution failed: " + response.functionError());
			}

			String responseData = response.payload().asUtf8String();
			JSONObject responseJson = new JSONObject(responseData);

			return responseJson.getString(RESULT);
		} catch (LambdaException | JSONException e) {
			throw new MarkdownClientException(e);
		}

    }

}
