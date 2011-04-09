package org.sagebionetworks.web.unitclient.security.user;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.web.client.security.user.UserData;

public class UserDataTest {
	
	@Test
	public void testToCookie(){
		UserData user = new UserData("10", "magic", "abcxyz");
		String cookieString = user.getCookieString();
		System.out.println(cookieString);
		UserData copy = UserData.createFromCookieString(cookieString);
		assertNotNull(copy);
		assertEquals(user, copy);
	}

}
