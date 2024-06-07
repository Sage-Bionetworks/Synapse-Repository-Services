package org.sagebionetworks.repo.manager.password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class PasswordValidatorUnitTest {

	@Mock
	Resource mockSpringResource;

	@InjectMocks
	PasswordValidatorImpl passwordValidator;

	@BeforeEach
	public void setup() throws Exception {
		// Mock the banned password list
		String delimiter = "\n";
		String[] passwords = { "p@ssw0rd", "2short", "CapitalLetters3*", "nodigits$", "12345667890!@#$%^" };
		InputStream inputStream = IOUtils.toInputStream(String.join(delimiter, passwords), "UTF-8");
		when(mockSpringResource.getInputStream()).thenReturn(inputStream);

		// usually called by Spring after injecting dependencies. Since this is a unit
		// test, call it manually.
		passwordValidator.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSet_bannedPasswordSetGeneration() {
		// test that passwords that are too short are not loaded and all loaded
		// passwords are toLower()ed
		Set<String> expected = Sets.newHashSet("p@ssw0rd", "capitalletters3*");

		// method under test is afterPropertiesSet() and was called in the setup()
		assertEquals(expected, passwordValidator.bannedPasswordSet);
	}

	@Test
	public void testValidatePassword_nullPassword() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			passwordValidator.validatePassword(null);
		}).getMessage();
		assertEquals("password is required.", message);

	}

	@Test
	public void testValidatePassword_lengthTooShort() {
		String invalidPassword = "abc123!";
		String message = assertThrows(InvalidPasswordException.class, () -> {
			passwordValidator.validatePassword(invalidPassword);
		}).getMessage();
		assertEquals(PasswordValidatorImpl.INVALID_PASSWORD_MESSAGE, message);

	}

	@Test
	public void testValidatePasswordWithCaseInsensitiveBannedPassword() {
		String invalidPassword = "P@SsW0Rd";
		String message = assertThrows(InvalidPasswordException.class, () -> {
			passwordValidator.validatePassword(invalidPassword);
		}).getMessage();
		assertEquals("This password is known to be a commonly used password. Please choose another password!", message);
	}
	
	@Test
	public void testValidatePasswordWithSynapse() {
		String invalidPassword = "contains-Synapse^123";
		String message = assertThrows(InvalidPasswordException.class, () -> {
			passwordValidator.validatePassword(invalidPassword);
		}).getMessage();
		assertEquals("This password is known to be a commonly used password. Please choose another password!", message);
	}

	@Test
	public void testValidatePasswordWithVaid() {
		List<String> valid = List.of("foo.bar123");
		valid.forEach(p -> {
			passwordValidator.validatePassword(p);
		});
	}

	@Test
	public void testContainsLetterAndDigitAndSpecial() {
		assertTrue(PasswordValidatorImpl.containsLetterAndDigitAndSpecial("one2three%"));
		assertTrue(PasswordValidatorImpl.containsLetterAndDigitAndSpecial("ONE2THREE%"));
		assertFalse(PasswordValidatorImpl.containsLetterAndDigitAndSpecial("onethree%"));
		assertFalse(PasswordValidatorImpl.containsLetterAndDigitAndSpecial("one2three"));
		assertFalse(PasswordValidatorImpl.containsLetterAndDigitAndSpecial("12345678!@#%^&"));
	}

	@Test
	public void testContainsLetterAndDigitAndSpecialWithEachLower() {
		for (char c = 'a'; c <= 'z'; c++) {
			assertTrue(PasswordValidatorImpl.containsLetterAndDigitAndSpecial("2%" + c));
		}
	}

	@Test
	public void testContainsLetterAndDigitAndSpecialWithEachUpper() {
		for (char c = 'A'; c <= 'Z'; c++) {
			assertTrue(PasswordValidatorImpl.containsLetterAndDigitAndSpecial("2%" + c));
		}
	}

	@Test
	public void testContainsLetterAndDigitAndSpecialWithEachDigit() {
		for (char c = '0'; c <= '9'; c++) {
			assertTrue(PasswordValidatorImpl.containsLetterAndDigitAndSpecial("A%" + c));
		}
	}

	@Test
	public void testContainsLetterAndDigitAndSpecialWithEachSpecial() {
		PasswordValidatorImpl.SPECIAL.forEach(c -> {
			assertTrue(PasswordValidatorImpl.containsLetterAndDigitAndSpecial("a2" + c));
		});
	}
}
