package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFavorite;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.message.Settings;

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
	
}

