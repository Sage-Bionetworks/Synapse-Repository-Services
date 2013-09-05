package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class PhenotypeDataTest {
	
	@Test
	public void testRoundTripLayer() throws JSONObjectAdapterException {
		PhenotypeData l1 = new PhenotypeData();
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

		l1.setVersionComment("versionComment");
		l1.setVersionLabel("versionLabel");
		l1.setVersionNumber(1L);
		l1.setVersionUrl("/versions/1");
		l1.setVersions("/versions");
		l1.setContentType("text");
		l1.setMd5("01234567890123456789012345678901");
		
		List<LocationData> ll = new ArrayList<LocationData>();
		LocationData l = new LocationData();
		l.setPath("path");
		l.setType(LocationTypeNames.sage);
		ll.add(l);
		l1.setLocations(ll);

		l1.setNumSamples(1000L);
		
		l1.setNumSamples(100L);
		l1.setDisease("disease");
		l1.setSpecies("species");
		l1.setS3Token("S3token");

		adapter1 = l1.writeToJSONObject(adapter1);
		String s = adapter1.toJSONString();
		adapter2 = new JSONObjectAdapterImpl(s);
		PhenotypeData l2 = new PhenotypeData(adapter2);
		
		assertEquals(l1, l2);
	}

}
