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
		HierarchyInfo info = HierarchyInfo.parseHierachyInfoJson(json).get();
		assertEquals(new HierarchyInfo().setPath("foo/bar").setBenefactorId(123L).setProjectId(444L), info);

	}

	@Test
	public void testParseHierachyInfoJsonWithNullJSON() {
		String json = null;
		// call under test
		assertEquals(Optional.empty(), HierarchyInfo.parseHierachyInfoJson(json));
	}

	@Test
	public void testParseHierachyInfoJsonWithNullProjectId() {
		JSONObject start = new JSONObject();
		start.put("path", "foo/bar");
		start.put("benefactorId", 123L);
		String json = start.toString();
		// call under test
		HierarchyInfo info = HierarchyInfo.parseHierachyInfoJson(json).get();
		assertEquals(new HierarchyInfo().setPath("foo/bar").setBenefactorId(123L).setProjectId(null), info);
		assertEquals(Optional.empty(), info.getProjectId());
		assertEquals(Optional.of(123L), info.getBenefactorId());
	}

	@Test
	public void testParseHierachyInfoJsonWithNullBenefactorId() {
		JSONObject start = new JSONObject();
		start.put("path", "foo/bar");
		start.put("projectId", 444L);
		String json = start.toString();
		// call under test
		HierarchyInfo info = HierarchyInfo.parseHierachyInfoJson(json).get();
		assertEquals(new HierarchyInfo().setPath("foo/bar").setBenefactorId(null).setProjectId(444L), info);
		assertEquals(Optional.of(444L), info.getProjectId());
		assertEquals(Optional.empty(), info.getBenefactorId());
	}
}
