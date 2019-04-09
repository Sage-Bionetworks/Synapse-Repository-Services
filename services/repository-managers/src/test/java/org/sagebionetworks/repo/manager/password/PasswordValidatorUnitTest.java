package org.sagebionetworks.repo.manager.password;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.password.PasswordValidatorImpl.PASSWORD_MIN_LENGTH;

import java.io.InputStream;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class PasswordValidatorUnitTest {

	@Mock
	Resource mockSpringResource;

	@InjectMocks
	PasswordValidatorImpl passwordValidator;

	@Before
	public void setup() throws Exception {
		//Mock the banned password list
		String delimiter = "\n";
		String[] passwords = {"password", "2short", "CapitalLetters"};
		InputStream inputStream = IOUtils.toInputStream(String.join(delimiter, passwords), "UTF-8");
		when(mockSpringResource.getInputStream()).thenReturn(inputStream);

		// usually called by Spring after injecting dependencies. Since this is a unit test, call it manually.
		passwordValidator.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSet_bannedPasswordSetGeneration(){
		//test that passwords that are too short are not loaded and all loaded passwords are toLower()ed
		Set<String> expected = Sets.newHashSet("password", "capitalletters");

		//method under test is afterPropertiesSet() and was called in the setup()

		assertEquals(expected, passwordValidator.bannedPasswordSet);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidatePassword_nullPassword(){
		passwordValidator.validatePassword(null);
	}

	@Test(expected= InvalidPasswordException.class)
	public void testValidatePassword_lengthTooShort() {
		String invalidPassword = RandomStringUtils.randomAlphanumeric(PASSWORD_MIN_LENGTH-1);
		passwordValidator.validatePassword(invalidPassword);
	}

	@Test(expected = InvalidPasswordException.class)
	public void testValidatePassword_caseInsensitiveBannedPassword(){
		passwordValidator.validatePassword("PaSsWoRd");
	}

	@Test
	public void testValidatePassword_validPassword() {
		String validPassword = RandomStringUtils.randomAlphanumeric(PASSWORD_MIN_LENGTH);
		passwordValidator.validatePassword(validPassword);
	}
}
