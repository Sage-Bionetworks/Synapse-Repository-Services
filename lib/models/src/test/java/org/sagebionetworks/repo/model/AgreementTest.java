package org.sagebionetworks.repo.model;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class AgreementTest {

	@Test
	public void testRoundTripAgreement() throws JSONObjectAdapterException {
		Agreement a1 = new Agreement();
		JSONObjectAdapter adapter1 = new JSONObjectAdapterImpl();
		JSONObjectAdapter adapter2 = new JSONObjectAdapterImpl();
		
		a1.setAccessControlList("/acl");
		a1.setAnnotations("/annotations");
		a1.setCreatedBy("createdBy");
		a1.setDatasetId("1");
		a1.setDatasetVersionNumber(1L);
		a1.setDescription("description");
		a1.setEtag("1");
		a1.setEulaVersionNumber(1L);
		a1.setId("1");
		a1.setModifiedBy("modifiedBy");
		a1.setName("name");
		a1.setParentId("0");
		a1.setUri("uri");
		a1.setVersion("1.0.0");
		
		adapter1 = a1.writeToJSONObject(adapter1);
		String s = adapter1.toJSONString();
		System.out.println(s);
		adapter2 = new JSONObjectAdapterImpl(s);
		Agreement a2 = new Agreement(adapter2);
		
		assertEquals(a1, a2);
		
		return;
	}
}
