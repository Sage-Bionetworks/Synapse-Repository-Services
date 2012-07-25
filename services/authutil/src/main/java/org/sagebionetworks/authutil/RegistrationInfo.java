package org.sagebionetworks.authutil;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RegistrationInfo {
	private String registrationToken;
	private String password;
	
	public RegistrationInfo() {}
	
	public String getRegistrationToken() {
		return registrationToken;
	}
	public void setRegistrationToken(String registrationToken) {
		this.registrationToken = registrationToken;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RegistrationInfo [token=" + registrationToken + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((registrationToken == null) ? 0 : registrationToken.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof RegistrationInfo))
			return false;
		RegistrationInfo other = (RegistrationInfo) obj;
		if (registrationToken == null) {
			if (other.registrationToken != null)
				return false;
		} else if (!registrationToken.equals(other.registrationToken))
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		return true;
	}
	
}
