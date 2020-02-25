package org.sagebionetworks.repo.util.jrjc;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import java.util.Base64;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JiraClientImplTest {

    @Mock
    private SimpleHttpClient mockHttpClient;
    @Mock
    private SimpleHttpResponse mockResponse;

    private JiraClientImpl jiraClient;
    private static final String USERNAME = "userName";
    private static final String USERAPIKEY = "userApiKey";

    @Before
    public void setUp() throws Exception {
        jiraClient = new JiraClientImpl();
        ReflectionTestUtils.setField(jiraClient, "httpClient", mockHttpClient);
        ReflectionTestUtils.setField(jiraClient, "USERNAME", USERNAME);
        ReflectionTestUtils.setField(jiraClient, "APIKEY", USERAPIKEY);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getProjectInfo() throws Exception {
        String expectedJson =
                "{  \"id\": \"10000\"," +
                "  \"issueTypes\": [" +
                "    {" +
                "      \"self\": \"https://your-domain.atlassian.net/rest/api/3/issueType/3\"," +
                "      \"id\": \"3\"," +
                "      \"description\": \"A task that needs to be done.\"," +
                "      \"iconUrl\": \"https://your-domain.atlassian.net//secure/viewavatar?size=xsmall&avatarId=10299&avatarType=issuetype\\\",\"," +
                "      \"name\": \"Task\"," +
                "      \"subtask\": false," +
                "      \"avatarId\": 1" +
                "    }," +
                "    {" +
                "      \"self\": \"https://your-domain.atlassian.net/rest/api/3/issueType/1\"," +
                "      \"id\": \"1\"," +
                "      \"description\": \"A problem with the software.\"," +
                "      \"iconUrl\": \"https://your-domain.atlassian.net/secure/viewavatar?size=xsmall&avatarId=10316&avatarType=issuetype\\\",\"," +
                "      \"name\": \"Bug\"," +
                "      \"subtask\": false," +
                "      \"avatarId\": 10002," +
                "      \"entityId\": \"9d7dd6f7-e8b6-4247-954b-7b2c9b2a5ba2\"," +
                "      \"scope\": {" +
                "        \"type\": \"PROJECT\"," +
                "        \"project\": {" +
                "          \"id\": \"10000\"," +
                "          \"key\": \"KEY\"," +
                "          \"name\": \"Next Gen Project\"" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}";

        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockResponse.getContent()).thenReturn(expectedJson);
        when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

        // Call under test
        JSONObject pInfo = jiraClient.getProjectInfo("SG", "Task");

        assertNotNull(pInfo);
        String projId = (String) pInfo.get("id");
        assertEquals("10000", projId);
        Long issueTypeId = (Long) pInfo.get("issueTypeId");
        assertTrue(3L==issueTypeId);
    }

    @Test(expected=IllegalStateException.class)
    public void getProjectInfoIssueTypeNotFound() throws Exception {
        String expectedJson =
                "{  \"id\": \"10000\"," +
                        "  \"issueTypes\": [" +
                        "    {" +
                        "      \"self\": \"https://your-domain.atlassian.net/rest/api/3/issueType/3\"," +
                        "      \"id\": \"3\"," +
                        "      \"description\": \"A task that needs to be done.\"," +
                        "      \"iconUrl\": \"https://your-domain.atlassian.net//secure/viewavatar?size=xsmall&avatarId=10299&avatarType=issuetype\\\",\"," +
                        "      \"name\": \"Flag\"," +
                        "      \"subtask\": false," +
                        "      \"avatarId\": 1" +
                        "    }," +
                        "    {" +
                        "      \"self\": \"https://your-domain.atlassian.net/rest/api/3/issueType/1\"," +
                        "      \"id\": \"1\"," +
                        "      \"description\": \"A problem with the software.\"," +
                        "      \"iconUrl\": \"https://your-domain.atlassian.net/secure/viewavatar?size=xsmall&avatarId=10316&avatarType=issuetype\\\",\"," +
                        "      \"name\": \"Bug\"," +
                        "      \"subtask\": false," +
                        "      \"avatarId\": 10002," +
                        "      \"entityId\": \"9d7dd6f7-e8b6-4247-954b-7b2c9b2a5ba2\"," +
                        "      \"scope\": {" +
                        "        \"type\": \"PROJECT\"," +
                        "        \"project\": {" +
                        "          \"id\": \"10000\"," +
                        "          \"key\": \"KEY\"," +
                        "          \"name\": \"Next Gen Project\"" +
                        "        }" +
                        "      }" +
                        "    }" +
                        "  ]}";

        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockResponse.getContent()).thenReturn(expectedJson);
        when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

        // Call under test
        JSONObject pInfo = jiraClient.getProjectInfo("SG", "Task");

    }

    @Test(expected=RuntimeException.class)
    public void getProjectInfoInvalidJson() throws Exception {
        String expectedJson =
                "{  \"id\": \"10000\"," +
                        "  \"issueTypes\": [" +
                        "    {" +
                        "      \"self\": \"https://your-domain.atlassian.net/rest/api/3/issueType/3\"," +
                        "      \"id\": \"3\"," +
                        "      \"description\": \"A task that needs to be done.\"," +
                        "      \"iconUrl\": \"https://your-domain.atlassian.net//secure/viewavatar?size=xsmall&avatarId=10299&avatarType=issuetype\\\",\"," +
                        "      \"name\": \"Flag\"," +
                        "      \"subtask\": false," +
                        "      \"avatarId\": 1" +
                        "    ," +
                        "    {" +
                        "      \"self\": \"https://your-domain.atlassian.net/rest/api/3/issueType/1\"," +
                        "      \"id\": \"1\"," +
                        "      \"description\": \"A problem with the software.\"," +
                        "      \"iconUrl\": \"https://your-domain.atlassian.net/secure/viewavatar?size=xsmall&avatarId=10316&avatarType=issuetype\\\",\"," +
                        "      \"name\": \"Bug\"," +
                        "      \"subtask\": false," +
                        "      \"avatarId\": 10002," +
                        "      \"entityId\": \"9d7dd6f7-e8b6-4247-954b-7b2c9b2a5ba2\"," +
                        "      \"scope\": {" +
                        "        \"type\": \"PROJECT\"," +
                        "        \"project\": {" +
                        "          \"id\": \"10000\"," +
                        "          \"key\": \"KEY\"," +
                        "          \"name\": \"Next Gen Project\"" +
                        "        }" +
                        "      }" +
                        "    }" +
                        "  ]}";

        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockResponse.getContent()).thenReturn(expectedJson);
        when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

        // Call under test
        JSONObject pInfo = jiraClient.getProjectInfo("SG", "Task");
    }

    @Test
    public void getFields() throws Exception {
        String expectedJson =
                "[" +
                "  {" +
                "    \"id\": \"description\"," +
                "    \"name\": \"Description\"," +
                "    \"custom\": false," +
                "    \"orderable\": true," +
                "    \"navigable\": true," +
                "    \"searchable\": true," +
                "    \"clauseNames\": [" +
                "      \"description\"" +
                "    ]," +
                "    \"schema\": {" +
                "      \"type\": \"string\"," +
                "      \"system\": \"description\"" +
                "    }" +
                "  }," +
                "  {" +
                "    \"id\": \"summary\"," +
                "    \"key\": \"summary\"," +
                "    \"name\": \"Summary\"," +
                "    \"custom\": false," +
                "    \"orderable\": true," +
                "    \"navigable\": true," +
                "    \"searchable\": true," +
                "    \"clauseNames\": [" +
                "      \"summary\"" +
                "    ]," +
                "    \"schema\": {" +
                "      \"type\": \"string\"," +
                "      \"system\": \"summary\"" +
                "    }" +
                "  }" +
                "]";

        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockResponse.getContent()).thenReturn(expectedJson);
        when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

        // Call under test
        Map<String, String> m = jiraClient.getFields();

        assertNotNull(m);
        assertEquals(2, m.size());
        assertEquals("description", m.get("Description"));
        assertEquals("summary", m.get("Summary"));
    }

    @Test(expected=RuntimeException.class)
    public void getFieldsInvalidJson() throws Exception {
        String expectedJson =
                "[" +
                        "  {" +
                        "    \"id\": \"description\"," +
                        "    \"name\": \"Description\"," +
                        "    \"custom\": false," +
                        "    \"orderable\": true," +
                        "    \"navigable\": true," +
                        "    \"searchable\": true," +
                        "    \"clauseNames\": [" +
                        "      \"description\"" +
                        "    ]," +
                        "    \"schema\": {" +
                        "      \"type\": \"string\"," +
                        "      \"system\": \"description\"" +
                        "    }" +
                        "  ," +
                        "  {" +
                        "    \"id\": \"summary\"," +
                        "    \"key\": \"summary\"," +
                        "    \"name\": \"Summary\"," +
                        "    \"custom\": false," +
                        "    \"orderable\": true," +
                        "    \"navigable\": true," +
                        "    \"searchable\": true," +
                        "    \"clauseNames\": [" +
                        "      \"summary\"" +
                        "    ]," +
                        "    \"schema\": {" +
                        "      \"type\": \"string\"," +
                        "      \"system\": \"summary\"" +
                        "    }" +
                        "  }" +
                        "]";

        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockResponse.getContent()).thenReturn(expectedJson);
        when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);

        // Call under test
        Map<String, String> m = jiraClient.getFields();
    }

    @Test
    public void createIssue() throws Exception {
        String expectedJson =
            "{" +
            "  \"id\": \"10000\"," +
            "  \"key\": \"SG-24\"," +
            "  \"self\": \"https://your-domain.atlassian.net/rest/api/3/issue/10000\"," +
            "  \"transition\": {" +
            "    \"status\": 200," +
            "    \"errorCollection\": {" +
            "      \"errorMessages\": []," +
            "      \"errors\": {}" +
            "    }" +
            "  }" +
            "}";
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        when(mockResponse.getContent()).thenReturn(expectedJson);
        when(mockHttpClient.post(any(SimpleHttpRequest.class), anyString())).thenReturn(mockResponse);

        JSONObject issue = new JSONObject();
        
        // Call under test
        JSONObject o = jiraClient.createIssue(issue);

        assertNotNull(o);
        assertEquals("SG-24", o.get("key"));
    }

    @Test
    public void createRequest() throws Exception {

        SimpleHttpRequest req;
        req = jiraClient.createRequest("/aPath/", "aResource", "application/json");
        assertEquals("https://sagebionetworks.jira.com/aPath/aResource", req.getUri());
        req = jiraClient.createRequest("/aPath/", null, "application/json");
        assertEquals("https://sagebionetworks.jira.com/aPath/", req.getUri());

        Map<String, String> headers = req.getHeaders();
        assertEquals("Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + USERAPIKEY).getBytes("utf-8")), headers.get(HttpHeaders.AUTHORIZATION));
        assertEquals("application/json", headers.get(HttpHeaders.CONTENT_TYPE));
        assertEquals("Synapse", headers.get(HttpHeaders.USER_AGENT));

    }

    @Test(expected=IllegalArgumentException.class)
    public void createRequestBadPath() {
        jiraClient.createRequest("/aPath", "aResource", "application/json");
    }

    @Test
    public void handleResponseStatusOK() {
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        JiraClientImpl.handleResponseStatus(mockResponse.getStatusCode()); // Should not fail
    }

    @Test(expected=RuntimeException.class)
    public void handleResponseStatusError() {
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        JiraClientImpl.handleResponseStatus(mockResponse.getStatusCode());
    }
}