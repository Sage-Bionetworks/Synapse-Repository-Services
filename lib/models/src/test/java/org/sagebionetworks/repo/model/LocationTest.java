package org.sagebionetworks.repo.model;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class LocationTest {
	
	@Test
	public void testRoundTripLocation() throws JSONObjectAdapterException {
		Location l1 = new Location();
		JSONObjectAdapter adapter1 = new JSONObjectAdapterImpl();
		JSONObjectAdapter adapter2 = new JSONObjectAdapterImpl();
		Date d = new Date();
		
		l1.setAccessControlList("/acl");
		l1.setAnnotations("/annotations");
		l1.setCreatedBy("createdBy");
		l1.setCreatedOn(d);
		l1.setDescription("description");
		l1.setEtag("1");
		l1.setId("1");
		l1.setModifiedBy("modifiedBy");
		l1.setModifiedOn(d);
		l1.setName("name");
		l1.setParentId("0");
		l1.setUri("uri");
		l1.setVersion("1.0.0");

		l1.setVersion("1.0.0");
		l1.setVersionComment("versionComment");
		l1.setVersionLabel("versionLabel");
		l1.setVersionNumber(1L);
		l1.setVersionUrl("/versions/1");
		l1.setVersions("/versions");
		
		l1.setContentType("text");
		l1.setMd5sum("012345678901234567890123456789012");
		l1.setPath("path");

		adapter1 = l1.writeToJSONObject(adapter1);
		String s = adapter1.toJSONString();
		adapter2 = new JSONObjectAdapterImpl(s);
		Location l2 = new Location(adapter2);
		
		assertEquals(l1, l2);
		return;
	}
}
