package org.sagebionetworks.repo.manager.principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;

public class NewUserUtilsTest {

	@Test
	public void testNull() {
		assertThrows(IllegalArgumentException.class, () -> {
			NewUserUtils.validateAndTrim(null);
		});
	}

	@Test
	public void testEmailNull() {
		NewUser user = new NewUser();
		user.setUserName("userName");
		user.setEmail(null);

		assertThrows(IllegalArgumentException.class, () -> {
			NewUserUtils.validateAndTrim(user);
		});
	}

	@Test
	public void testUserNameNull() {
		NewUser user = new NewUser();
		user.setUserName(null);
		user.setEmail("email");

		assertThrows(IllegalArgumentException.class, () -> {
			NewUserUtils.validateAndTrim(user);
		});
	}

	@Test
	public void testSubjectNullForProvider() {
		NewUser user = new NewUser();
		user.setUserName("username");
		user.setEmail("email");
		user.setOauthProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		user.setSubject(null);

		assertThrows(IllegalArgumentException.class, () -> {
			NewUserUtils.validateAndTrim(user);
		});
	}

	@Test
	public void testTrim() {
		NewUser user = new NewUser();
		user.setUserName(" NameNeedsTrim ");
		user.setEmail(" email@trim.please ");
		user = NewUserUtils.validateAndTrim(user);
		assertNotNull(user);
		assertEquals("NameNeedsTrim", user.getUserName());
		assertEquals("email@trim.please", user.getEmail());
	}
}
