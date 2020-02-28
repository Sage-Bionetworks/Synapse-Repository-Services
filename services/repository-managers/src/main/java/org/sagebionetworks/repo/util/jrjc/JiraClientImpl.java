package org.sagebionetworks.repo.util.jrjc;

import org.sagebionetworks.StackConfigurationSingleton;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.sagebionetworks.simpleHttpClient.*;
import org.sagebionetworks.url.HttpMethod;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Mini JIRA REST client
 */
public class JiraClientImpl implements JiraClient {
	private static final String JIRA_URL = "https://sagebionetworks.jira.com";
	private static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
	public static final String NAME_KEY = "name";
	public static final String ID_KEY = "id";

	private final SimpleHttpClient httpClient;
	private String USERNAME;
	private String APIKEY;
	private static final Integer TIME_OUT = 30 * 1000; // 30 seconds
	private static final String JIRA_API_PROJECT_URL = "/rest/api/3/project/";
	private static final String JIRA_API_FIELDS_URL = "/rest/api/3/field/";
	private static final String JIRA_API_ISSUE_URL = "/rest/api/3/issue/";
	private static final String USER_AGENT = "Synapse";
	private static final String JIRA_PROJECT_ISSUE_TYPES_KEY = "issueTypes";
	private static final String JIRA_PROJECT_ID_KEY = "id";

	public JiraClientImpl() {
		SimpleHttpClientConfig httpClientConfig = new SimpleHttpClientConfig();
		httpClientConfig.setSocketTimeoutMs(TIME_OUT);
		httpClient = new SimpleHttpClientImpl(httpClientConfig);
		USERNAME = StackConfigurationSingleton.singleton().getJiraUserEmail();
		APIKEY = StackConfigurationSingleton.singleton().getJiraUserApikey();
	}

	@Override
	public JSONObject getProjectInfo(String projectKey, String issueTypeName) {
		SimpleHttpRequest req = createRequest(JIRA_API_PROJECT_URL, "SG");

		String json = execRequest(HttpMethod.GET, req, null);

		Long issueTypeId = null;
		String projectId = null;
		try {
			JSONParser parser = new JSONParser();
			JSONObject pInfo = (JSONObject) parser.parse(json);
			JSONArray issueTypeNames = (JSONArray)pInfo.get(JIRA_PROJECT_ISSUE_TYPES_KEY);
			Iterator<JSONObject> it = issueTypeNames.iterator();
			while (it.hasNext()) {
				JSONObject issueType = it.next();
				String name = (String) issueType.get(NAME_KEY);
				if (issueTypeName.equalsIgnoreCase(name)) {
					if (issueType.get(ID_KEY) != null) {
						issueTypeId = Long.valueOf((String) issueType.get(ID_KEY));
					}
					break;
				}
			}
			if (issueTypeId == null) {
				throw new JiraClientException(String.format("Could not find the issue typeId for issue type {}", issueTypeName));
			}
			projectId = (String) pInfo.get(JIRA_PROJECT_ID_KEY);
		}
		catch (ParseException e) {
			throw new JiraClientException("JIRA client: error processing JSON", e);
		}
		JSONObject projInfo = new JSONObject();
		projInfo.put("issueTypeId", issueTypeId);
		projInfo.put("id", projectId);
		return projInfo;
	}

	@Override
	public Map<String,String> getFields() {
		SimpleHttpRequest req = createRequest(JIRA_API_FIELDS_URL, null);

		String json = execRequest(HttpMethod.GET, req, null);

		JSONArray fields = null;
		Map<String, String> fieldsMap = new HashMap<>();
		try {
			JSONParser parser = new JSONParser();
			fields = (JSONArray) parser.parse(json);
			Iterator<JSONObject> it = fields.iterator();
			while (it.hasNext()) {
				JSONObject fieldDetail = it.next();
				String fieldName = (String) fieldDetail.get(NAME_KEY);
				String fieldId = (String) fieldDetail.get(ID_KEY);
				fieldsMap.put(fieldName, fieldId);
			}
		}
		catch (ParseException e) {
			throw new JiraClientException("JIRA client: error processing JSON", e);
		}
		return fieldsMap;
	}

	@Override
	public JSONObject createIssue(JSONObject issue) {
		SimpleHttpRequest req = createRequest(JIRA_API_ISSUE_URL, null);

		String json = execRequest(HttpMethod.POST, req, issue);

		JSONObject createdIssue = null;
		try {
			JSONParser parser = new JSONParser();
			createdIssue = (JSONObject) parser.parse(json);
		}
		catch (ParseException e) {
			throw new JiraClientException("JIRA client: error processing JSON", e);
		}
		return createdIssue;
	}

	SimpleHttpRequest createRequest(String path, String resource) {
		if (! (path.startsWith("/") && path.endsWith("/"))) {
			throw new JiraClientException(new IllegalArgumentException("Path needs to begin and end with '/'"));
		}

		SimpleHttpRequest request = new SimpleHttpRequest();
		if (resource == null) {
			request.setUri(JIRA_URL + path);
		} else {
			request.setUri(JIRA_URL + path + resource);
		}
		request.setHeaders(getHeaders(USERNAME, APIKEY));

		return request;
	}

	String execRequest(HttpMethod method, SimpleHttpRequest req, JSONObject body) {
		SimpleHttpResponse resp = null;
		try {
			switch (method) {
				case GET:
					resp = httpClient.get(req);
					break;
				case POST:
					if (body == null) {
						throw new JiraClientException(new IllegalArgumentException("Body cannot be null"));
					}
					resp = httpClient.post(req, body.toJSONString());
					break;
				default:
					throw new JiraClientException(new IllegalArgumentException("Method can only be 'GET' or 'POST'"));
			}
		} catch (IOException e) {
			throw new JiraClientException(e.getMessage(), e);
		}
		String json = null;
		handleResponseStatus(resp.getStatusCode());
		json = resp.getContent();
		return json;
	}

	static void handleResponseStatus(int status) {
		switch (status) {
			case HttpStatus.SC_BAD_REQUEST:
				throw new JiraClientException("JIRA Client: invalid request" + status);
			case HttpStatus.SC_NOT_FOUND:
				throw new JiraClientException("JIRA Client: not found" + status);
			case HttpStatus.SC_UNAUTHORIZED:
				throw new JiraClientException("JIRA Client: credentials wrong or missing" + status);
			case HttpStatus.SC_FORBIDDEN:
				throw new JiraClientException("JIRA Client: insufficient permission" + status);
			case HttpStatus.SC_OK:
			case HttpStatus.SC_CREATED:
				return;
			default:
				throw new JiraClientException("JIRA: unexpected status received - " + status);
		}
	}

	static Map<String, String> getHeaders(String userName, String apiKey) {
		Map<String, String> headers = new HashMap<>();
		try {
			headers.put(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((userName + ":" + apiKey).getBytes("utf-8")));
		} catch (UnsupportedEncodingException e) {
			throw new JiraClientException("Error encoding credentials", e);
		}
		headers.put(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON);
		headers.put(HttpHeaders.USER_AGENT, USER_AGENT);
		return headers;
	}
}
