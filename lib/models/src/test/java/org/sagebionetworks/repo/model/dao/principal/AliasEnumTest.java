package org.sagebionetworks.repo.model.dao.principal;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.sagebionetworks.repo.model.principal.AliasEnum;

public class AliasEnumTest {

	@Test
	public void testValidateAliasNull() {
		// Each type should not allow null
		for (AliasEnum ae : AliasEnum.values()) {
			try {
				ae.validateAlias(null);
				fail("Should have failed");
			} catch (IllegalArgumentException e) {
				// expected
			}
		}
	}

	@Test
	public void testValidatePrincipalNameUser() {
		AliasEnum.USER_NAME
				.validateAlias("1234567890.a-b_cdefghijklmnopqrstuvwxyz");
	}

	@Test
	public void testValidatePrincipalUserSpaces() {
		try {
			AliasEnum.USER_NAME.validateAlias("has spaces");
			fail("should have failed because it contains spaces.");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("letters"));
			assertTrue(e.getMessage().contains("numbers"));
			assertTrue(e.getMessage().contains("underscore"));
			assertTrue(e.getMessage().contains("dash"));
			assertTrue(e.getMessage().contains("dot"));
			assertTrue(e.getMessage().contains("3 characters long"));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidatePrincipalUserTooShort() {
		AliasEnum.USER_NAME.validateAlias("12");
	}

	@Test
	public void testValidatePrincipalUserLongEnough() {
		AliasEnum.USER_NAME.validateAlias("123");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidatePrincipalUserOtherChars() {
		AliasEnum.USER_NAME.validateAlias("has!@#$%^&*()otherchars");
	}

	@Test
	public void testValidatePrincipalNameTeam() {
		AliasEnum.TEAM_NAME
				.validateAlias("1234567890.a-b_c defghijklmnopqrstuvwxyzABCDEFGHIJKLMNOP");
	}

	@Test
	public void testValidatePrincipalTeamAt() {
		try {
			AliasEnum.TEAM_NAME.validateAlias("has@chars");
			fail("should have failed because it contains spaces.");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("letters"));
			assertTrue(e.getMessage().contains("numbers"));
			assertTrue(e.getMessage().contains("underscore"));
			assertTrue(e.getMessage().contains("dash"));
			assertTrue(e.getMessage().contains("dot"));
			assertTrue(e.getMessage().contains("space"));
			assertTrue(e.getMessage().contains("3 characters long"));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidatePrincipalTeamTooShort() {
		AliasEnum.TEAM_NAME.validateAlias("12");
	}

	@Test
	public void testValidatePrincipalTeamLongEnough() {
		AliasEnum.TEAM_NAME.validateAlias("123");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidatePrincipalTeamOtherChars() {
		AliasEnum.TEAM_NAME.validateAlias("has!@#$%^&*()otherchars");
	}

	@Test
	public void testValidateEmail(){
		AliasEnum.USER_EMAIL.validateAlias("foo.bar@company.com");
	}
}
