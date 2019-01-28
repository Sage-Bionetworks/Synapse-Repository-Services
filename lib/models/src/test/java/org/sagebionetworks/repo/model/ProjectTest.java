package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class ProjectTest {
	
	@Test
	public void testRoundTripProject() throws JSONObjectAdapterException {
		Project p1 = new Project();
		JSONObjectAdapter adapter1 = new JSONObjectAdapterImpl();
		JSONObjectAdapter adapter2 = new JSONObjectAdapterImpl();
		Date d = new Date();

		p1.setCreatedBy("createdBy");
		p1.setCreatedOn(d);
		p1.setDescription("description");
		p1.setEtag("etag");
		p1.setId("1");
		p1.setModifiedBy("modifiedBy");
		p1.setModifiedOn(d);
		p1.setName("name");
		p1.setParentId("0");

		adapter1 = p1.writeToJSONObject(adapter1);
		String s = adapter1.toJSONString();
		adapter2 = new JSONObjectAdapterImpl(s);
		Project p2 = new Project(adapter2);
		
		assertEquals(p1, p2);
	}
}
