package org.sagebionetworks.repo.manager.password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class PasswordValidatorAutowiredTest {

	@Autowired
	PasswordValidator passwordValidator;

	@Test
	public void testValidatePasswordWithValid() {
		// call under test
		passwordValidator.validatePassword("bat$90cat");
	}

	@Test
	public void testValidatePasswordWithInvaid() {
		String message = assertThrows(InvalidPasswordException.class, () -> {
			// call under test
			passwordValidator.validatePassword("password");
		}).getMessage();
		assertEquals("A valid password must be at least 8 characters long and must include"
				+ " letters, digits (0-9), and special characters ~!@#$%^&*_-+=`|\\(){}[]:;\"'<>,.?/", message);
	}

	@Test
	public void testValidatePasswordWithCommonPasswrod() {
		String message = assertThrows(InvalidPasswordException.class, () -> {
			// call under test
			passwordValidator.validatePassword("p@ssw0rd");
		}).getMessage();
		assertEquals("This password is known to be a commonly used password. Please choose another password!", message);
	}
}
