package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class FileTest {
	
	@Test
	public void testRoundTripFile() throws JSONObjectAdapterException {
		File f1 = new File();
		JSONObjectAdapter adapter1 = new JSONObjectAdapterImpl();
		JSONObjectAdapter adapter2 = new JSONObjectAdapterImpl();
		Date d = new Date();
		
		f1.setAccessControlList("/acl");
		f1.setAnnotations("/annotations");
		f1.setCreatedBy("createdBy");
		f1.setCreatedOn(d);
		f1.setDescription("description");
		f1.setEtag("1");
		f1.setId("1");
		f1.setModifiedBy("modifiedBy");
		f1.setModifiedOn(d);
		f1.setName("name");
		f1.setParentId("0");
		f1.setUri("uri");

		f1.setVersionComment("versionComment");
		f1.setVersionLabel("versionLabel");
		f1.setVersionNumber(1L);
		f1.setVersionUrl("/versions/1");
		f1.setVersions("/versions");
		f1.setContentType("text");
		f1.setMd5("01234567890123456789012345678901");
		
		List<LocationData> ll = new ArrayList<LocationData>();
		LocationData l = new LocationData();
		l.setPath("path");
		l.setType(LocationTypeNames.sage);
		ll.add(l);
		f1.setLocations(ll);

		f1.setS3Token("S3token");

		adapter1 = f1.writeToJSONObject(adapter1);
		String s = adapter1.toJSONString();
		adapter2 = new JSONObjectAdapterImpl(s);
		File f2 = new File(adapter2);
		
		assertEquals(f1, f2);
	}

}