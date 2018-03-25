package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFavorite;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.schema.generator.EffectiveSchemaUtil;

public class UserProfileUtilsTest {

	@Test
	public void testRoundtrip() throws Exception {
		UserProfile dto = new UserProfile();
		dto.setOwnerId("101");
		dto.setFirstName("foo");
		dto.setLastName("bar");
		dto.setRStudioUrl("http://rstudio.com");
		dto.setEtag("0");
		dto.setCompany("my company");
		dto.setIndustry("my industry");
		dto.setLocation("Seattle area");
		dto.setSummary("My summary");
		dto.setTeamName("Team A");
		dto.setUrl("http://link.to.my.page/");
		dto.setProfilePicureFileHandleId("456");
		AttachmentData picData = new AttachmentData();
		picData.setName("Fake name");
		picData.setTokenId("Fake token ID");
		picData.setMd5("Fake MD5");
		dto.setPic(picData);
		dto.setNotificationSettings(new Settings());
		dto.getNotificationSettings().setSendEmailNotifications(false);
		
		DBOUserProfile dbo = new DBOUserProfile();
		UserProfileUtils.copyDtoToDbo(dto, dbo);
		assertEquals(new Long(456),dbo.getPictureId());
		UserProfile dto2 = UserProfileUtils.convertDboToDto(dbo);
		assertEquals(dto, dto2);
	}
	
	@Test
	public void testSetNullProfilePictureFileHandleId() {
		UserProfile dto = new UserProfile();
		dto.setProfilePicureFileHandleId("456");
		DBOUserProfile dbo = new DBOUserProfile();
		UserProfileUtils.copyDtoToDbo(dto, dbo);
		assertNotNull(dbo.getPictureId());
		assertEquals(new Long(456),dbo.getPictureId());
		dto.setProfilePicureFileHandleId(null);
		UserProfileUtils.copyDtoToDbo(dto, dbo);
		assertNull(dbo.getPictureId());
	}
	
	/**
	 * Make sure the conversion schema does not fail when the UserProfile blob holds a NamedAnnotations blob
	 * This test can be removed when the older serialization method is removed
	 */
	@Test
	public void testRoundTripSchemaToJDO() throws Exception {
		UserProfile dto = new UserProfile();
		dto.setOwnerId("101");
		dto.setFirstName("foo");
		dto.setLastName("bar");
		dto.setRStudioUrl("http://rstudio.com");
		dto.setEtag("0");
		dto.setCompany("my company");
		dto.setIndustry("my industry");
		dto.setLocation("Seattle area");
		dto.setSummary("My summary");
		dto.setTeamName("Team A");
		dto.setUrl("http://link.to.my.page/");
		dto.setNotificationSettings(new Settings());
		dto.getNotificationSettings().setSendEmailNotifications(false);
		AttachmentData picData = new AttachmentData();
		picData.setName("Fake name");
		picData.setTokenId("Fake token ID");
		picData.setMd5("Fake MD5");
		dto.setPic(picData);
		
		DBOUserProfile dbo = new DBOUserProfile();
		UserProfileUtils.copyDtoToDbo(dto, dbo);
		
		// Replace the blob with the older serialization method
		String jsonString = EffectiveSchemaUtil.loadEffectiveSchemaFromClasspath(UserProfile.class);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		dbo.setProperties(mapDtoFieldsToAnnotations(dto, schema));
		
		UserProfile dto2 = UserProfileUtils.convertDboToDto(dbo);
		
		assertEquals(dto, dto2);
	}

	@Deprecated
	@Test
	public void testAnnotationsRoundtrip() throws Exception {
		UserProfile dto = new UserProfile();
		dto.setOwnerId("101");
		dto.setFirstName("foo");
		dto.setLastName("bar");
		dto.setRStudioUrl("http://rstudio.com");
		dto.setEtag("0");

		String jsonString = EffectiveSchemaUtil.loadEffectiveSchemaFromClasspath(UserProfile.class);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		byte[] na = mapDtoFieldsToAnnotations(dto, schema);
		
		UserProfile dto2 = new UserProfile();
		SchemaSerializationUtils.mapAnnotationsToDtoFields(na, dto2, schema);

		assertEquals(dto, dto2);
	}

	/**
	 * Copied from SchemaSerializationUtils
	 */
	@Deprecated
	@SuppressWarnings("rawtypes")
	public static byte[] mapDtoFieldsToAnnotations(Object dto, ObjectSchema schema) throws DatastoreException {
		Map<String, ObjectSchema> schemaProperties = schema.getProperties();
		NamedAnnotations properties = new NamedAnnotations();
		Annotations a = properties.getPrimaryAnnotations();
		for (String propertyName : schemaProperties.keySet()) {
				try {
					Field field = dto.getClass().getDeclaredField(propertyName);
					field.setAccessible(true);
					Map<String, List<String>> stringAnnots = a.getStringAnnotations();
					Class fieldType = field.getType();
					if (!(fieldType.equals(String.class) || fieldType.equals(AttachmentData.class))) {
						// throw new RuntimeException("Unsupported field type "+fieldType);
						continue; // Skip fields that are not supported
					}
					if (fieldType.equals(AttachmentData.class))
					{
						AttachmentData attachment = (AttachmentData)field.get(dto);
						if (attachment != null)
						{
							stringAnnots.put(propertyName, Arrays.asList(new String[]{EntityFactory.createJSONStringForEntity(attachment)}));
						}
						else
							stringAnnots.put(propertyName, Arrays.asList(new String[]{""}));
					}
					else //String
						stringAnnots.put(propertyName, Arrays.asList(new String[]{(String)field.get(dto)}));
				} catch (NoSuchFieldException e) {
					// since the object is generated by the schema, this should never happen
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				} catch (JSONObjectAdapterException e) {
					throw new RuntimeException(e);
				}
		}
		try {
			return JDOSecondaryPropertyUtils.compressAnnotations(properties);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		
	}

	@Test
	public void testRoundtripWithNulls() throws Exception {
		UserProfile dto = new UserProfile();
		dto.setOwnerId(null);
		dto.setFirstName("foo");
		dto.setLastName("bar");
		dto.setRStudioUrl("http://rstudio.com");
		dto.setEtag("0");
		DBOUserProfile dbo = new DBOUserProfile();
		UserProfileUtils.copyDtoToDbo(dto, dbo);
		UserProfile dto2 = UserProfileUtils.convertDboToDto(dbo);
		// Notification setting should be added
		assertNotNull(dto2.getNotificationSettings());
		// default to send email.
		assertTrue(dto2.getNotificationSettings().getSendEmailNotifications());
		dto2.setNotificationSettings(null);
		assertEquals(dto, dto2);
	}
	
	@Test
	public void testGetFavoriteId() {
		String pId = "principal";
		String eId = "syn123";
		Favorite favorite = new Favorite();
		favorite.setPrincipalId(pId);
		favorite.setEntityId(eId);
		String id = UserProfileUtils.getFavoriteId(favorite);
		
		assertEquals(pId+"-"+eId, id);

		Favorite f2 = new Favorite();
		id = UserProfileUtils.getFavoriteId(f2);
		assertNull(id);
	}
	
	@Test
	public void testGetFavoritePrincipalIdFromId() {
		String pId = "principal";
		String eId = "syn123";
		String id = pId+"-"+eId;
		assertEquals(pId, UserProfileUtils.getFavoritePrincipalIdFromId(id));		
	}
	
	@Test
	public void testGetFavoriteEntityIdFromId() {
		String pId = "principal";
		String eId = "syn123";
		String id = pId+"-"+eId;
		assertEquals(eId, UserProfileUtils.getFavoriteEntityIdFromId(id));		
	}

	@Test
	public void testFavoriteRoundTrip() {
		Favorite fav = new Favorite();
		fav.setPrincipalId("123");
		fav.setEntityId("syn456");
		fav.setCreatedOn(new Date());
		DBOFavorite dbo = new DBOFavorite();
		UserProfileUtils.copyDtoToDbo(fav, dbo);
		Favorite clone = UserProfileUtils.copyDboToDto(dbo);
		assertEquals(fav, clone);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFavoriteRoundTripNull() {
		Favorite fav = new Favorite();
		fav.setPrincipalId(null);
		fav.setEntityId("syn456");
		fav.setCreatedOn(new Date());
		DBOFavorite dbo = new DBOFavorite();
		UserProfileUtils.copyDtoToDbo(fav, dbo);
		fail("principalId can not be null");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFavoriteRoundTripNull2() {
		Favorite fav = new Favorite();
		fav.setPrincipalId("123");
		fav.setEntityId(null);
		fav.setCreatedOn(new Date());
		DBOFavorite dbo = new DBOFavorite();
		UserProfileUtils.copyDtoToDbo(fav, dbo);
		fail("principalId can not be null");
	}
	
	@Test
	public void testProfileMigrationTranslation() throws IOException{
		// For this test the properties contain all data that must be expanded to the DTO
		UserProfile hasAllData = new UserProfile();
		hasAllData.setFirstName("firstName");
		hasAllData.setLastName("lastName");
		Settings settings = new Settings();
		settings.setSendEmailNotifications(true);
		hasAllData.setNotificationSettings(settings);
		// all of this data is only in the properties.
		DBOUserProfile backup = new DBOUserProfile();
		backup.setProperties(JDOSecondaryPropertyUtils.compressObject(hasAllData));
		// translate
		DBOUserProfile translated = new DBOUserProfile().getTranslator().createDatabaseObjectFromBackup(backup);
		assertEquals(hasAllData.getFirstName(), new String(translated.getFirstName(), "UTF-8"));
		assertEquals(hasAllData.getLastName(), new String(translated.getLastName(), "UTF-8"));
		assertTrue(translated.isEmailNotification());
	}
	
	@Test
	public void testProfileMigrationTranslationNull() throws IOException{
		// For this test the properties contain all data that must be expanded to the DTO
		UserProfile hasAllData = new UserProfile();
		hasAllData.setFirstName(null);
		hasAllData.setLastName(null);
		hasAllData.setNotificationSettings(null);
		// all of this data is only in the properties.
		DBOUserProfile backup = new DBOUserProfile();
		backup.setProperties(JDOSecondaryPropertyUtils.compressObject(hasAllData));
		// translate
		DBOUserProfile translated = new DBOUserProfile().getTranslator().createDatabaseObjectFromBackup(backup);
		assertNull(translated.getFirstName());
		assertNull(translated.getLastName());
		// should default to true
		assertTrue(translated.isEmailNotification());
	}
}

