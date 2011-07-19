package org.sagebionetworks.web.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class LicenseAgreement implements IsSerializable {

	private String licenseHtml;
	private String citationHtml;
	private String eulaId;
	
	/**
	 * Default constructor
	 */
	public LicenseAgreement() {		
	}


	public String getLicenseHtml() {
		return licenseHtml;
	}


	public void setLicenseHtml(String licenseHtml) {
		this.licenseHtml = licenseHtml;
	}


	public String getCitationHtml() {
		return citationHtml;
	}


	public void setCitationHtml(String citationHtml) {
		this.citationHtml = citationHtml;
	}


	public String getEulaId() {
		return eulaId;
	}


	public void setEulaId(String eulaId) {
		this.eulaId = eulaId;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((citationHtml == null) ? 0 : citationHtml.hashCode());
		result = prime * result + ((eulaId == null) ? 0 : eulaId.hashCode());
		result = prime * result
				+ ((licenseHtml == null) ? 0 : licenseHtml.hashCode());
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
		LicenseAgreement other = (LicenseAgreement) obj;
		if (citationHtml == null) {
			if (other.citationHtml != null)
				return false;
		} else if (!citationHtml.equals(other.citationHtml))
			return false;
		if (eulaId == null) {
			if (other.eulaId != null)
				return false;
		} else if (!eulaId.equals(other.eulaId))
			return false;
		if (licenseHtml == null) {
			if (other.licenseHtml != null)
				return false;
		} else if (!licenseHtml.equals(other.licenseHtml))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "LicenseAgreement [licenseHtml=" + licenseHtml
				+ ", citationHtml=" + citationHtml + ", eulaId=" + eulaId + "]";
	}
	
}
