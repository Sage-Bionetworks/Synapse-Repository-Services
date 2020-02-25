package org.sagebionetworks.repo.util.jrjc;

import java.net.URI;
import java.util.concurrent.ExecutionException;

import org.sagebionetworks.StackConfigurationSingleton;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.simpleHttpClient.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
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

	private final SimpleHttpClient httpClient;
	private String USERNAME;
	private String APIKEY;
	private static final Integer TIME_OUT = 30 * 1000; // 30 seconds
	private static final String JIRA_API_PROJECT_URL = "/rest/api/3/project/";
	private static final String JIRA_API_FIELDS_URL = "/rest/api/3/field/";
	private static final String JIRA_API_ISSUE_URL = "/rest/api/3/issue/";
	private static final String USER_AGENT = "Synapse";
	private static JSONParser parser;
	private static final String JIRA_PROJECT_ISSUE_TYPES_KEY = "issueTypes";
	private static final String JIRA_PROJECT_ID_KEY = "id";

	public JiraClientImpl() {
		SimpleHttpClientConfig httpClientConfig = new SimpleHttpClientConfig();
		httpClientConfig.setSocketTimeoutMs(TIME_OUT);
		httpClient = new SimpleHttpClientImpl(httpClientConfig);

		USERNAME = StackConfigurationSingleton.singleton().getJiraUserEmail();
		APIKEY = StackConfigurationSingleton.singleton().getJiraUserApikey();
		parser = new JSONParser();
	}

	@Override
	public JSONObject getProjectInfo(String projectKey, String issueTypeName) {
		SimpleHttpRequest req = createRequest(JIRA_API_PROJECT_URL, "SG", "application/json");
		SimpleHttpResponse resp = null;
		try {
			resp = httpClient.get(req);
		} catch (IOException e) {
			throw new RuntimeException();
		}
		handleResponseStatus(resp.getStatusCode());
		String json = null;
		json = resp.getContent();

		Long issueTypeId = -1L;
		String projectId = null;
		try (Reader reader = new StringReader(json)) {
			JSONObject pInfo = (JSONObject) parser.parse(reader);
			JSONArray issueTypeNames = (JSONArray)pInfo.get(JIRA_PROJECT_ISSUE_TYPES_KEY);
			Iterator<JSONObject> it = issueTypeNames.iterator();
			while (it.hasNext()) {
				JSONObject issueType = it.next();
				String name = (String) issueType.get("name");
				if (issueTypeName.equalsIgnoreCase(name)) {
					issueTypeId = Long.parseLong((String) issueType.get("id"));
					break;
				}
			}
			if (issueTypeId == -1L) {
				throw new IllegalStateException(String.format("Could not find the issue typeId for issue type {}", issueTypeName));
			}
			projectId = (String) pInfo.get(JIRA_PROJECT_ID_KEY);
		}
		catch (IOException | ParseException e) {
			throw new RuntimeException("JIRA client: error processing JSON");
		}
		JSONObject projInfo = new JSONObject();
		projInfo.put("issueTypeId", issueTypeId);
		projInfo.put("id", projectId);
		return projInfo;
	}

	@Override
	public Map<String,String> getFields() {
		SimpleHttpRequest req = createRequest(JIRA_API_FIELDS_URL, null, "application/json");
		SimpleHttpResponse resp = null;
		try {
			resp = httpClient.get(req);
		} catch (IOException e) {
			throw new RuntimeException();
		}
		handleResponseStatus(resp.getStatusCode());
		String json = null;
		json = resp.getContent();

		JSONArray fields = null;
		Map<String, String> fieldsMap = new HashMap<>();
		try (Reader reader = new StringReader(json)) {
			fields = (JSONArray) parser.parse(reader);
			Iterator<JSONObject> it = fields.iterator();
			while (it.hasNext()) {
				JSONObject fieldDetail = it.next();
				String fieldName = (String) fieldDetail.get("name");
				String fieldId = (String) fieldDetail.get("id");
				fieldsMap.put(fieldName, fieldId);
			}
		}
		catch (IOException | ParseException e) {
			throw new RuntimeException(("JIRA client: error processing JSON\n"));
		}
		return fieldsMap;
	}

	@Override
	public JSONObject createIssue(JSONObject issue) {
		SimpleHttpRequest req = createRequest(JIRA_API_ISSUE_URL, null, "application/json");
		SimpleHttpResponse resp = null;
		try {
			resp = httpClient.post(req, issue.toJSONString());
		} catch (IOException e) {
			throw new RuntimeException();
		}
		handleResponseStatus(resp.getStatusCode());

		String json = null;
		json = resp.getContent();

		JSONObject createdIssue = null;
		try (Reader reader = new StringReader(json)) {
			createdIssue = (JSONObject) parser.parse(reader);
		}
		catch (IOException | ParseException e) {
			throw new RuntimeException("JIRA client: error processing JSON\n");
		}
		return createdIssue;
	}

	SimpleHttpRequest createRequest(String path, String resource, String contentType) {
		if (! (path.startsWith("/") && path.endsWith("/"))) {
			throw new IllegalArgumentException("Path needs to begin and end with '/'");
		}

		SimpleHttpRequest request = new SimpleHttpRequest();
		if (resource == null) {
			request.setUri(JIRA_URL + path);
		} else {
			request.setUri(JIRA_URL + path + resource);
		}
		Map<String, String> headers = new HashMap<>();
		try {
			headers.put(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + APIKEY).getBytes("utf-8")));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Error encoding credentials");
		}
		if (contentType != null) {
			headers.put(HttpHeaders.CONTENT_TYPE, contentType);
		}
		headers.put(HttpHeaders.USER_AGENT, USER_AGENT);
		request.setHeaders(headers);

		return request;
	}

	static void handleResponseStatus(int status) {
		switch (status) {
			case HttpStatus.SC_BAD_REQUEST:
				throw new RuntimeException("JIRA: invalid request");
			case HttpStatus.SC_NOT_FOUND:
				throw new RuntimeException("JIRA: not found");
			case HttpStatus.SC_UNAUTHORIZED:
				throw new RuntimeException("JIRA: credentials wrong or missing");
			case HttpStatus.SC_FORBIDDEN:
				throw new RuntimeException("JIRA: insufficient permission");
			case HttpStatus.SC_OK:
			case HttpStatus.SC_CREATED:
				return;
			default:
				throw new RuntimeException("JIRA: unexpected status received - " + status);
		}
	}
}
