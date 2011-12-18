package org.sagebionetworks.repo.model;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class EulaTest {

	@Test
	public void testRoundTripEula() throws JSONObjectAdapterException {
		Eula e1 = new Eula();
		JSONObjectAdapter adapter1 = new JSONObjectAdapterImpl();
		JSONObjectAdapter adapter2 = new JSONObjectAdapterImpl();
		Date d = new Date();
		
		e1.setAccessControlList("/acl");
		e1.setAnnotations("/annotations");
		e1.setCreatedBy("createdBy");
		e1.setCreatedOn(d);
		e1.setDescription("description");
		e1.setEtag("1");
		e1.setId("1");
		e1.setModifiedBy("modifiedBy");
		e1.setModifiedOn(d);
		e1.setName("name");
		e1.setParentId("0");
		e1.setUri("uri");
		e1.setVersion("1.0.0");

		e1.setAgreement("agreement");

		adapter1 = e1.writeToJSONObject(adapter1);
		String s = adapter1.toJSONString();
		adapter2 = JSONObjectAdapterImpl.createAdapterFromJSONString(s);
		Eula e2 = new Eula(adapter2);
		
		assertEquals(e1, e2);
		return;
	}
	
}
