package org.sagebionetworks.repo.model.ses;

import java.util.Objects;

import org.sagebionetworks.repo.model.json.CatchAllJsonObject;

public class SESJsonRecipient extends CatchAllJsonObject {

	private String status;
	private String action;
	private String diagnosticCode;
	private String emailAddress;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getDiagnosticCode() {
		return diagnosticCode;
	}

	public void setDiagnosticCode(String diagnosticCode) {
		this.diagnosticCode = diagnosticCode;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(action, diagnosticCode, emailAddress, status);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SESJsonRecipient other = (SESJsonRecipient) obj;
		return Objects.equals(action, other.action) && Objects.equals(diagnosticCode, other.diagnosticCode)
				&& Objects.equals(emailAddress, other.emailAddress) && Objects.equals(status, other.status);
	}

	@Override
	public String toString() {
		return "SESJsonRecipient [status=" + status + ", action=" + action + ", diagnosticCode=" + diagnosticCode + ", emailAddress="
				+ emailAddress + "]";
	}

}
