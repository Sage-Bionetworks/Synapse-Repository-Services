package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;


public class SchemaSerializationUtilsTest {
	@Test
	public void testAnnotationsRoundtrip() throws Exception {
		UserProfile dto = new UserProfile();
		dto.setOwnerId("101");
		dto.setFirstName("foo");
		dto.setLastName("bar");
		dto.setRStudioUrl("http://rstudio.com");
		dto.setDisplayName("foo bar");
		dto.setEtag("0");

		String jsonString = (String) UserProfile.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		byte[] na = SchemaSerializationUtils.mapDtoFieldsToAnnotations(dto, schema);
		
		UserProfile dto2 = new UserProfile();
		SchemaSerializationUtils.mapAnnotationsToDtoFields(na, dto2, schema);

		assertEquals(dto, dto2);
	}
}
