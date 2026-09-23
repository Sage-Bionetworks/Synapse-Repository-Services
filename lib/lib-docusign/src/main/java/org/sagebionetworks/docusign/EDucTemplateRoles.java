package org.sagebionetworks.docusign;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The names an eDUC template gives its signer roles, and the tab labels belonging to each.
 * <p>
 * This is the vocabulary shared by the code that fills a template in and the code that validates a
 * template supplies what will be filled in. Both have to agree on every one of these names, so they
 * are declared once here rather than written out at each use.
 * <p>
 * A collaborator role is numbered, one per accessor the template can carry.
 */
public class EDucTemplateRoles {

	public static final String PRINCIPAL_INVESTIGATOR = "principal_investigator";
	public static final String SIGNING_OFFICIAL = "signing_official";

	private static final String COLLABORATOR_PREFIX = "collaborator_";

	/**
	 * The highest collaborator index a template may define. A DocuSign envelope allows 100 recipients,
	 * two of which are taken by the principal investigator and the signing official.
	 */
	public static final int MAX_COLLABORATORS = 98;

	private static final Pattern COLLABORATOR_PATTERN = Pattern.compile(COLLABORATOR_PREFIX + "(\\d+)");

	// Every role's tabs are named for the role, so each label is derived rather than restated.
	private static final String NAME_SUFFIX = "_name";
	private static final String USER_NAME_SUFFIX = "_user_name";
	private static final String EMAIL_SUFFIX = "_email";
	private static final String INSTITUTION_SUFFIX = "_institution";
	private static final String SIGNATURE_SUFFIX = "_signature";
	private static final String DATE_SUFFIX = "_date";

	/**
	 * The name of the collaborator role at the given index, counting from one.
	 */
	public static String collaborator(int index) {
		return COLLABORATOR_PREFIX + index;
	}

	/**
	 * The index of the given collaborator role, counting from one.
	 *
	 * @param roleName
	 * @return the index, or -1 if the name is not a collaborator role
	 */
	public static int collaboratorIndex(String roleName) {
		Matcher matcher = COLLABORATOR_PATTERN.matcher(roleName);
		if (matcher.matches()) {
			return Integer.parseInt(matcher.group(1));
		} else {
			return -1;
		}
	}

	/**
	 * Whether the given role name is one of the numbered collaborator roles.
	 */
	public static boolean isCollaborator(String roleName) {
		return collaboratorIndex(roleName) > 0;
	}

	/** The label of the tab holding the given role's full name. */
	public static String nameTab(String roleName) {
		return roleName + NAME_SUFFIX;
	}

	/** The label of the tab holding the given role's Synapse user name. */
	public static String userNameTab(String roleName) {
		return roleName + USER_NAME_SUFFIX;
	}

	/** The label of the tab holding the given role's email address. */
	public static String emailTab(String roleName) {
		return roleName + EMAIL_SUFFIX;
	}

	/** The label of the tab holding the given role's institution. */
	public static String institutionTab(String roleName) {
		return roleName + INSTITUTION_SUFFIX;
	}

	/** The label of the tab the given role signs. */
	public static String signatureTab(String roleName) {
		return roleName + SIGNATURE_SUFFIX;
	}

	/** The label of the tab holding the date the given role signed. */
	public static String dateTab(String roleName) {
		return roleName + DATE_SUFFIX;
	}
}
