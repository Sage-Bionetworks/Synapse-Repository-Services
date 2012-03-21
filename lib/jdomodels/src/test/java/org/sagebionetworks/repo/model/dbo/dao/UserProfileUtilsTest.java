package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class UserProfileUtilsTest {

	@Test
	public void testRoundtrip() throws Exception {
		UserProfile dto = new UserProfile();
		dto.setOwnerId(KeyFactory.keyToString(101L));
		dto.setFirstName("foo");
		dto.setLastName("bar");
		dto.setRStudioUrl("http://rstudio.com");
		dto.setDisplayName("foo bar");
		dto.setEtag("0");
		DBOUserProfile dbo = new DBOUserProfile();
		String jsonString = (String) UserProfile.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		UserProfileUtils.copyDtoToDbo(dto, dbo, schema);
		UserProfile dto2 = new UserProfile();
		UserProfileUtils.copyDboToDto(dbo, dto2, schema);
		assertEquals(dto, dto2);
	}

	@Test(expected = NumberFormatException.class)
	public void testInvalidEtag() throws Exception {
		UserProfile dto = new UserProfile();
		dto.setOwnerId("101");
		dto.setFirstName("foo");
		dto.setLastName("bar");
		dto.setRStudioUrl("http://rstudio.com");
		dto.setDisplayName("foo bar");
		dto.setEtag("not a number");
		DBOUserProfile dbo = new DBOUserProfile();
		String jsonString = (String) UserProfile.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		UserProfileUtils.copyDtoToDbo(dto, dbo, schema);
	}

	@Test
	public void testRoundtripWithNulls() throws Exception {
		UserProfile dto = new UserProfile();
		dto.setOwnerId(null);
		dto.setFirstName("foo");
		dto.setLastName("bar");
		dto.setRStudioUrl("http://rstudio.com");
		dto.setDisplayName("foo bar");
		dto.setEtag("0");
		DBOUserProfile dbo = new DBOUserProfile();
		String jsonString = (String) UserProfile.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		UserProfileUtils.copyDtoToDbo(dto, dbo, schema);
		UserProfile dto2 = new UserProfile();
		UserProfileUtils.copyDboToDto(dbo, dto2, schema);
		assertEquals(dto, dto2);
	}

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
		NamedAnnotations na = UserProfileUtils.mapDtoFieldsToAnnotations(dto, schema);
		
		UserProfile dto2 = new UserProfile();
		UserProfileUtils.mapAnnotationsToDtoFields(na, dto2, schema);

		assertEquals(dto, dto2);
	}

}
