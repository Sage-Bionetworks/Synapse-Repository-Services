package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class CodeTest {
	
	@Test
	public void testRoundTripCode() throws JSONObjectAdapterException {
		Code c1 = new Code();
		JSONObjectAdapter adapter1 = new JSONObjectAdapterImpl();
		JSONObjectAdapter adapter2 = new JSONObjectAdapterImpl();
		Date d = new Date();
		
		c1.setAccessControlList("/acl");
		c1.setAnnotations("/annotations");
		c1.setCreatedBy("createdBy");
		c1.setCreatedOn(d);
		c1.setDescription("description");
		c1.setEtag("1");
		c1.setId("1");
		c1.setModifiedBy("modifiedBy");
		c1.setModifiedOn(d);
		c1.setName("name");
		c1.setParentId("0");
		c1.setUri("uri");

		c1.setStartDate(d);
		c1.setEndDate(d);
		
		c1.setVersionComment("versionComment");
		c1.setVersionLabel("versionLabel");
		c1.setVersionNumber(1L);
		c1.setVersionUrl("/versions/1");
		c1.setVersions("/versions");
		c1.setContentType("text");
		c1.setMd5("01234567890123456789012345678901");
		
		List<LocationData> ll = new ArrayList<LocationData>();
		LocationData l = new LocationData();
		l.setPath("path");
		l.setType(LocationTypeNames.sage);
		ll.add(l);
		c1.setLocations(ll);

		adapter1 = c1.writeToJSONObject(adapter1);
		String s = adapter1.toJSONString();
		adapter2 = new JSONObjectAdapterImpl(s);
		Code c2 = new Code(adapter2);
		
		assertEquals(c1, c2);
		return;
	}
}
