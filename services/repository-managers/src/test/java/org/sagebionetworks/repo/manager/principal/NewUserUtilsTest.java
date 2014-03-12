package org.sagebionetworks.repo.manager.principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.repo.model.auth.NewUser;

public class NewUserUtilsTest {

	@Test (expected=IllegalArgumentException.class)
	public void testNull(){
		NewUserUtils.validateAndTrim(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testEmailNull(){
		NewUser user = new NewUser();
		user.setUserName("userName");
		user.setEmail(null);
		NewUserUtils.validateAndTrim(user);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUserNameNull(){
		NewUser user = new NewUser();
		user.setUserName(null);
		user.setEmail("email");
		NewUserUtils.validateAndTrim(user);
	}
	
	@Test
	public void testTrim(){
		NewUser user = new NewUser();
		user.setUserName(" NameNeedsTrim ");
		user.setEmail(" email@trim.please ");
		user = NewUserUtils.validateAndTrim(user);
		assertNotNull(user);
		assertEquals("NameNeedsTrim", user.getUserName());
		assertEquals("email@trim.please", user.getEmail());
	}
	
	@Test
	public void testCreateDisplayNameBothNull(){
		NewUser user = new NewUser();
		user.setFirstName(null);
		user.setLastName(null);
		String result = NewUserUtils.createDisplayName(user);
		assertEquals("", result);
	}
	
	@Test
	public void testCreateDisplayNameLastNull(){
		NewUser user = new NewUser();
		user.setFirstName("First");
		user.setLastName(null);
		String result = NewUserUtils.createDisplayName(user);
		assertEquals("First", result);
	}
	
	@Test
	public void testCreateDisplayNameFristNull(){
		NewUser user = new NewUser();
		user.setFirstName(null);
		user.setLastName("Last");
		String result = NewUserUtils.createDisplayName(user);
		assertEquals("Last", result);
	}
	
	@Test
	public void testCreateDisplay(){
		NewUser user = new NewUser();
		user.setFirstName("First");
		user.setLastName("Last");
		String result = NewUserUtils.createDisplayName(user);
		assertEquals("First Last", result);
	}
}
