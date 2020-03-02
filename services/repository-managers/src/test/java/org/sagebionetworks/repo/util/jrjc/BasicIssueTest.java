package org.sagebionetworks.repo.util.jrjc;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BasicIssueTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testToJSONObject() {
        BasicIssue issue = new BasicIssue();
        issue.setSummary("mySummary");
        issue.setProjectId("myProjectId");
        issue.setIssueTypeId(1001L);
        Map<String, String> fields = new HashMap<>();
        fields.put("myKey1", "myVal1");
        fields.put("myKey2", "myVal2");
        issue.setCustomFields(fields);
        JSONObject jsonIssue = issue.toJONObject();
        assertNotNull(jsonIssue);
        JSONObject jsonFields = (JSONObject)jsonIssue.get("fields");
        assertEquals(issue.getSummary(), jsonFields.get("summary"));
        assertEquals(issue.getProjectId(), ((JSONObject)jsonFields.get("project")).get("id"));
        assertEquals(issue.getIssueTypeId(), ((JSONObject)jsonFields.get("issuetype")).get("id"));
        for (String k: fields.keySet()) {
            assertEquals(fields.get(k), (jsonFields.get(k)));
        }
    }

    @Test
    void testToJSONObjectNoFields() {
        BasicIssue issue = new BasicIssue();
        issue.setSummary("mySummary");
        issue.setProjectId("myProjectId");
        issue.setIssueTypeId(1001L);
        JSONObject jsonIssue = issue.toJONObject();
        assertNotNull(jsonIssue);
        JSONObject jsonFields = (JSONObject)jsonIssue.get("fields");
        assertNotNull(jsonFields);
        assertEquals(3, jsonFields.size());
        assertEquals(issue.getSummary(), jsonFields.get("summary"));
        assertEquals(issue.getProjectId(), ((JSONObject)jsonFields.get("project")).get("id"));
        assertEquals(issue.getIssueTypeId(), ((JSONObject)jsonFields.get("issuetype")).get("id"));
    }

    @Test
    void testToJSONObjectEmptyFields() {
        BasicIssue issue = new BasicIssue();
        issue.setSummary("mySummary");
        issue.setProjectId("myProjectId");
        issue.setIssueTypeId(1001L);
        JSONObject jsonIssue = issue.toJONObject();
        assertNotNull(jsonIssue);
        JSONObject jsonFields = (JSONObject)jsonIssue.get("fields");
        assertNotNull(jsonFields);
        assertEquals(3, jsonFields.size());
        assertEquals(issue.getSummary(), jsonFields.get("summary"));
        assertEquals(issue.getProjectId(), ((JSONObject)jsonFields.get("project")).get("id"));
        assertEquals(issue.getIssueTypeId(), ((JSONObject)jsonFields.get("issuetype")).get("id"));
        assertNull(jsonFields.get("fields"));
    }


}