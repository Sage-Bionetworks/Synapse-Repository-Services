package org.sagebionetworks.repo.manager.oauth;

import java.util.Objects;

public class ProvidedUserInfo {

	private String subject;
	private String usersVerifiedEmail;
	private String email;
	private String emailVerified;
	private String firstName;
	private String lastName;
	private AliasAndType aliasAndType;


	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(String emailVerified) {
		this.emailVerified = emailVerified;
	}

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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aliasAndType == null) ? 0 : aliasAndType.hashCode());
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((emailVerified == null) ? 0 : emailVerified.hashCode());
		result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
		result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		result = prime * result + ((usersVerifiedEmail == null) ? 0 : usersVerifiedEmail.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProvidedUserInfo other = (ProvidedUserInfo) obj;
		if (aliasAndType == null) {
			if (other.aliasAndType != null)
				return false;
		} else if (!aliasAndType.equals(other.aliasAndType))
			return false;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (emailVerified == null) {
			if (other.emailVerified != null)
				return false;
		} else if (!emailVerified.equals(other.emailVerified))
			return false;
		if (firstName == null) {
			if (other.firstName != null)
				return false;
		} else if (!firstName.equals(other.firstName))
			return false;
		if (lastName == null) {
			if (other.lastName != null)
				return false;
		} else if (!lastName.equals(other.lastName))
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		if (usersVerifiedEmail == null) {
			if (other.usersVerifiedEmail != null)
				return false;
		} else if (!usersVerifiedEmail.equals(other.usersVerifiedEmail))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ProvidedUserInfo [subject=" + subject + ", usersVerifiedEmail=" + usersVerifiedEmail + ", email="
				+ email + ", emailVerified=" + emailVerified + ", firstName=" + firstName + ", lastName=" + lastName
				+ ", aliasAndType=" + aliasAndType + "]";
	}

}
