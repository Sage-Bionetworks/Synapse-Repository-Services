package org.sagebionetworks.docusign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * These assertions are written against literal names on purpose. The names are the contract with the
 * template an ACT member authors in DocuSign, so a change to how they are composed has to fail here
 * rather than quietly rename what the code looks for.
 */
public class EDucTemplateRolesTest {

	@Test
	public void testRoleNames() {
		assertEquals("principal_investigator", EDucTemplateRoles.PRINCIPAL_INVESTIGATOR);
		assertEquals("signing_official", EDucTemplateRoles.SIGNING_OFFICIAL);
	}

	@Test
	public void testCollaborator() {
		assertEquals("collaborator_1", EDucTemplateRoles.collaborator(1));
		assertEquals("collaborator_98", EDucTemplateRoles.collaborator(98));
	}

	@Test
	public void testCollaboratorIndex() {
		assertEquals(1, EDucTemplateRoles.collaboratorIndex("collaborator_1"));
		assertEquals(98, EDucTemplateRoles.collaboratorIndex("collaborator_98"));
	}

	@Test
	public void testCollaboratorIndexWithRoleThatIsNotACollaborator() {
		assertEquals(-1, EDucTemplateRoles.collaboratorIndex("principal_investigator"));
		assertEquals(-1, EDucTemplateRoles.collaboratorIndex("signing_official"));
		// a name that merely starts with the prefix is not a collaborator role
		assertEquals(-1, EDucTemplateRoles.collaboratorIndex("collaborator_1_name"));
		assertEquals(-1, EDucTemplateRoles.collaboratorIndex("collaborator_"));
		assertEquals(-1, EDucTemplateRoles.collaboratorIndex("collaborator_x"));
	}

	@Test
	public void testIsCollaborator() {
		assertTrue(EDucTemplateRoles.isCollaborator("collaborator_1"));
		assertFalse(EDucTemplateRoles.isCollaborator("principal_investigator"));
		// This is why the prefix alone is not enough to recognize a role: a tab label starts with it too.
		assertFalse(EDucTemplateRoles.isCollaborator("collaborator_1_signature"));
	}

	@Test
	public void testTabLabelsForRolesTheRequestFillsIn() {
		assertEquals("signing_official_name", EDucTemplateRoles.nameTab(EDucTemplateRoles.SIGNING_OFFICIAL));
		assertEquals("signing_official_email", EDucTemplateRoles.emailTab(EDucTemplateRoles.SIGNING_OFFICIAL));
		assertEquals("signing_official_institution",
				EDucTemplateRoles.institutionTab(EDucTemplateRoles.SIGNING_OFFICIAL));
		assertEquals("principal_investigator_name",
				EDucTemplateRoles.nameTab(EDucTemplateRoles.PRINCIPAL_INVESTIGATOR));
		assertEquals("principal_investigator_email",
				EDucTemplateRoles.emailTab(EDucTemplateRoles.PRINCIPAL_INVESTIGATOR));
		assertEquals("principal_investigator_user_name",
				EDucTemplateRoles.userNameTab(EDucTemplateRoles.PRINCIPAL_INVESTIGATOR));
		assertEquals("collaborator_2_user_name", EDucTemplateRoles.userNameTab(EDucTemplateRoles.collaborator(2)));
		assertEquals("collaborator_2_name", EDucTemplateRoles.nameTab(EDucTemplateRoles.collaborator(2)));
	}

	@Test
	public void testTabLabelsTheSignerSupplies() {
		assertEquals("signing_official_signature", EDucTemplateRoles.signatureTab(EDucTemplateRoles.SIGNING_OFFICIAL));
		assertEquals("signing_official_date", EDucTemplateRoles.dateTab(EDucTemplateRoles.SIGNING_OFFICIAL));
		assertEquals("collaborator_3_signature", EDucTemplateRoles.signatureTab(EDucTemplateRoles.collaborator(3)));
		assertEquals("collaborator_3_date", EDucTemplateRoles.dateTab(EDucTemplateRoles.collaborator(3)));
	}

	@Test
	public void testMaxCollaborators() {
		// 100 DocuSign recipients, less the principal investigator and the signing official
		assertEquals(98, EDucTemplateRoles.MAX_COLLABORATORS);
	}
}
