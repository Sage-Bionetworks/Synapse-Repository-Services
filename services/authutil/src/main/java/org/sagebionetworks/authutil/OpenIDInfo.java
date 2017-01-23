package org.sagebionetworks.authutil;

import java.util.List;
import java.util.Map;

/**
 * Contains information returned via OpenID's simple registration extension
 *   or the attribute exchange extension
 */
public class OpenIDInfo {
	private String identifier;
	private String firstName;
	private String lastName;
	private String fullName;
	private String email;

	public static final String AX_EMAIL = "Email";
	public static final String AX_FIRST_NAME = "FirstName";
	public static final String AX_LAST_NAME = "LastName";
	
	public static final String AX_REG_EMAIL = "email";
	public static final String AX_REG_FULL_NAME = "fullname";
	
	public static final String CREATE_USER_IF_NECESSARY_PARAM_NAME = "org.sagebionetworks.createUserIfNecessary";

	public OpenIDInfo() { }

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Parses the result of an attribute exchange result and stores the values
	 */
	public void setMap(Map<String, List<String>> map) {
		List<String> emails = map.get(AX_EMAIL);
		List<String> fnames = map.get(AX_FIRST_NAME);
		List<String> lnames = map.get(AX_LAST_NAME);
		String email = (emails == null || emails.size() < 1 ? null : emails.get(0));
		String fname = (fnames == null || fnames.size() < 1 ? null : fnames.get(0));
		String lname = (lnames == null || lnames.size() < 1 ? null : lnames.get(0));

		this.email = email;
		this.firstName = fname;
		this.lastName = lname;
		if (fname != null && lname != null) {
			this.fullName = fname + " " + lname;
		}
	}
	
	public String getFullName() {
		return fullName;
	}
	
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	
	public String getFirstName() {
		return firstName;
	}
	
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
}
