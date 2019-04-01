package org.sagebionetworks.repo.manager.password;

import static org.junit.Assert.fail;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class PasswordValidatorAutowiredTest {
	@Autowired
	PasswordValidator passwordValidator;

	@Test
	public void testPasswordListWasLoaded(){
		passwordValidator.validatePassword(RandomStringUtils.randomAlphanumeric(PasswordValidatorImpl.PASSWORD_MIN_LENGTH));

		try {
			//test for some password that definitely should be in the banned password set (or it would be a terribly curated set)
			passwordValidator.validatePassword("password");
			fail("expected exception");
		}catch (InvalidPasswordException e){
			//expected
		}
	}
}
