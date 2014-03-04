package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class DatasetTest {
	
	@Test
	public void testRoundTripDataset() throws JSONObjectAdapterException {
		Study ds1 = new Study();
		JSONObjectAdapter adapter1 = new JSONObjectAdapterImpl();
		JSONObjectAdapter adapter2 = new JSONObjectAdapterImpl();
		Date d = new Date();
		
		ds1.setAccessControlList("/acl");
		ds1.setAnnotations("annotations");
		ds1.setCreatedBy("createdBy");
		ds1.setCreatedOn(d);
		ds1.setDescription("description");
		ds1.setEtag("etag");
		ds1.setId("id");
		ds1.setModifiedBy("modifiedBy");
		ds1.setModifiedOn(d);
		ds1.setName("name");
		ds1.setUri("uri");


		ds1.setVersionComment("versionComment");
		ds1.setVersionLabel("versionLabel");
		ds1.setVersionNumber(1L);
		ds1.setVersionUrl("versionUrl");
		ds1.setVersions("versions");
		ds1.setContentType("txt");
		ds1.setMd5("abcdef");
		List<LocationData> ldl = new ArrayList<LocationData>();
		LocationData ld = new LocationData();
		ld.setPath("path");
		ld.setType(LocationTypeNames.sage);
		ldl.add(ld);
		ds1.setLocations(ldl);

		adapter1 = ds1.writeToJSONObject(adapter1);
		String s = adapter1.toJSONString();
		adapter2 = new JSONObjectAdapterImpl(s);
		Study ds2 = new Study(adapter2);
		
		assertEquals(ds1, ds2);
	}
}
