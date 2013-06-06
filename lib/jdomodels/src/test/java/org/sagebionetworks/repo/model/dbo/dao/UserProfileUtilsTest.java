package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFavorite;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class UserProfileUtilsTest {

	@Test
	public void testRoundtrip() throws Exception {
		UserProfile dto = new UserProfile();
		dto.setOwnerId("101");
		dto.setFirstName("foo");
		dto.setLastName("bar");
		dto.setRStudioUrl("http://rstudio.com");
		dto.setDisplayName("foo bar");
		dto.setEtag("0");
		dto.setCompany("my company");
		dto.setIndustry("my industry");
		dto.setLocation("Seattle area");
		dto.setSummary("My summary");
		dto.setTeamName("Team A");
		dto.setUrl("http://link.to.my.page/");
		AttachmentData picData = new AttachmentData();
		picData.setName("Fake name");
		picData.setTokenId("Fake token ID");
		picData.setMd5("Fake MD5");
		dto.setPic(picData);
		
		DBOUserProfile dbo = new DBOUserProfile();
		String jsonString = (String) UserProfile.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		UserProfileUtils.copyDtoToDbo(dto, dbo, schema);
		UserProfile dto2 = new UserProfile();
		UserProfileUtils.copyDboToDto(dbo, dto2, schema);
		assertEquals(dto, dto2);
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
	
}

