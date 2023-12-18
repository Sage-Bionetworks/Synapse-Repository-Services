package org.sagebionetworks.repo.manager.oauth;

import java.util.Objects;

public class ProvidedUserInfo {

	private String subject;
	private String usersVerifiedEmail;
	private String firstName;
	private String lastName;
	private AliasAndType aliasAndType;

	public ProvidedUserInfo() {

	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getUsersVerifiedEmail() {
		return usersVerifiedEmail;
	}

	public void setUsersVerifiedEmail(String usersVerifiedEmail) {
		this.usersVerifiedEmail = usersVerifiedEmail;
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

	public AliasAndType getAliasAndType() {
		return aliasAndType;
	}

	public void setAliasAndType(AliasAndType aliasAndType) {
		this.aliasAndType = aliasAndType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(aliasAndType, firstName, lastName, subject, usersVerifiedEmail);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ProvidedUserInfo)) {
			return false;
		}
		ProvidedUserInfo other = (ProvidedUserInfo) obj;
		return Objects.equals(aliasAndType, other.aliasAndType) && Objects.equals(firstName, other.firstName)
				&& Objects.equals(lastName, other.lastName) && Objects.equals(subject, other.subject)
				&& Objects.equals(usersVerifiedEmail, other.usersVerifiedEmail);
	}

	@Override
	public String toString() {
		return "ProvidedUserInfo [subject=" + subject + ", usersVerifiedEmail=" + usersVerifiedEmail + ", firstName=" + firstName
				+ ", lastName=" + lastName + ", aliasAndType=" + aliasAndType + "]";
	}

}
