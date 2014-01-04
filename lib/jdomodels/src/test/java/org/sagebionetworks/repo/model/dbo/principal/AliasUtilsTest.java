package org.sagebionetworks.repo.model.dbo.principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;

public class AliasUtilsTest {

	@Test
	public void testRoundTrip() {
		// standard DTO to DBO round trip test.
		PrincipalAlias dto = new PrincipalAlias();
		dto.setAlias("alias");
		dto.setAliasId(new Long(123));
		dto.setEtag("etag");
		dto.setIsValidated(Boolean.TRUE);
		dto.setPrincipalId(new Long(456));
		dto.setType(AliasType.USER_NAME);

		DBOPrincipalAlias dbo = AliasUtils.createDBOFromDTO(dto);
		assertNotNull(dbo);
		assertNotNull(dbo.getAliasUnique());

		PrincipalAlias clone = AliasUtils.createDTOFromDBO(dbo);
		assertNotNull(clone);
		assertEquals(dto, clone);
	}

	@Test
	public void testGetUniquePrincipalNameCase() {
		String input = "BigTop";
		String expected = "bigtop";
		String result = AliasUtils.getUniqueAliasName(input);
		assertEquals(expected, result);
	}

	@Test
	public void testGetUniquePrincipalNameSpace() {
		String input = "Big Top";
		String expected = "bigtop";
		String result = AliasUtils.getUniqueAliasName(input);
		assertEquals(expected, result);
	}

	@Test
	public void testGetUniquePrincipalNameDash() {
		String input = "Big-Top";
		String expected = "bigtop";
		String result = AliasUtils.getUniqueAliasName(input);
		assertEquals(expected, result);
	}

	@Test
	public void testGetUniquePrincipalNameAll() {
		String input = "1.2 3-4_567890AbCdEfGhIJklmnoPqRSTUvwxyz";
		String expected = "1234567890abcdefghijklmnopqrstuvwxyz";
		String result = AliasUtils.getUniqueAliasName(input);
		assertEquals(expected, result);
	}

	@Test
	public void testTransformOldUserGroupTeam() {
		DBOUserGroup dbo = new DBOUserGroup();
		dbo.setId(123L);
		dbo.setName("Alzheimer's Challenge #1 Approved Participants");
		dbo.setIsIndividual(Boolean.FALSE);
		// Transform it
		PrincipalAlias alias = AliasUtils.transformOldUserGroup(dbo);
		assertNotNull(alias);
		assertEquals(dbo.getId(), alias.getPrincipalId());
		assertEquals(AliasType.TEAM_NAME, alias.getType());
		// The name should be transformed
		String expectedName = "Alzheimers Challenge 1 Approved Participants";
		assertEquals(expectedName, alias.getAlias());
	}
	
	@Test
	public void testTransformOldUserGroupUser() {
		DBOUserGroup dbo = new DBOUserGroup();
		dbo.setId(123L);
		dbo.setName("me@you.com");
		dbo.setIsIndividual(Boolean.TRUE);
		// Transform it
		PrincipalAlias alias = AliasUtils.transformOldUserGroup(dbo);
		assertNotNull(alias);
		assertEquals(dbo.getId(), alias.getPrincipalId());
		assertEquals(AliasType.USER_EMAIL, alias.getType());
		// The name should be transformed
		String expectedName = "me@you.com";
		assertEquals(expectedName, alias.getAlias());
	}

}
