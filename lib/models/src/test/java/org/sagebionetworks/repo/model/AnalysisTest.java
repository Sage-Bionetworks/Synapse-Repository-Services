package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class AnalysisTest {
	
	@Test
	public void testRoundTripAnalysis() throws JSONObjectAdapterException {
		Analysis a1 = new Analysis();
		JSONObjectAdapter adapter1 = new JSONObjectAdapterImpl();
		JSONObjectAdapter adapter2 = new JSONObjectAdapterImpl();
		Date d = new Date();
		
		a1.setAccessControlList("/acl");
		a1.setAnnotations("/annotations");
		a1.setCreatedBy("createdBy");
		a1.setCreatedOn(d);
		a1.setDescription("description");
		a1.setEtag("1");
		a1.setId("1");
		a1.setModifiedBy("modifiedBy");
		a1.setModifiedOn(d);
		a1.setName("name");
		a1.setParentId("0");
		a1.setUri("uri");

		a1.setSteps("/steps");
		a1.setStatus("status");

		adapter1 = a1.writeToJSONObject(adapter1);
		String s = adapter1.toJSONString();
		adapter2 = new JSONObjectAdapterImpl(s);
		Analysis a2 = new Analysis(adapter2);
		
		assertEquals(a1, a2);
		
		return;
	}
}
