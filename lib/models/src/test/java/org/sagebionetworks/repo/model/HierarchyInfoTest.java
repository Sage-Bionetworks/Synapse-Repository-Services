package org.sagebionetworks.repo.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class HierarchyInfoTest {

	@Test
	public void testParseHierachyInfoJson() {
		JSONObject start = new JSONObject();
		start.put("path", "foo/bar");
		start.put("benefactorId", 123L);
		start.put("projectId", 444L);
		String json = start.toString();
		// call under test
		Optional<HierarchyInfo> info = HierarchyInfo.parseHierachyInfoJson(json);
		assertEquals(
				Optional.of(new HierarchyInfo().setPath("foo/bar").setBenefactorId("syn123").setProjectId("syn444")),
				info);
	}

	@Test
	public void testParseHierachyInfoJsonWithNullJSON() {
		String json = null;
		// call under test
		Optional<HierarchyInfo> info = HierarchyInfo.parseHierachyInfoJson(json);
		assertEquals(Optional.empty(), info);
	}

	@Test
	public void testParseHierachyInfoJsonWithNullProjectId() {
		JSONObject start = new JSONObject();
		start.put("path", "foo/bar");
		start.put("benefactorId", 123L);
		String json = start.toString();
		// call under test
		Optional<HierarchyInfo> info = HierarchyInfo.parseHierachyInfoJson(json);
		assertEquals(Optional.of(new HierarchyInfo().setPath("foo/bar").setBenefactorId("syn123").setProjectId(null)),
				info);
	}

	@Test
	public void testParseHierachyInfoJsonWithNullBenefactorId() {
		JSONObject start = new JSONObject();
		start.put("path", "foo/bar");
		start.put("projectId", 444L);
		String json = start.toString();
		// call under test
		Optional<HierarchyInfo> info = HierarchyInfo.parseHierachyInfoJson(json);
		assertEquals(Optional.of(new HierarchyInfo().setPath("foo/bar").setBenefactorId(null).setProjectId("syn444")),
				info);
	}
}
