package org.sagebionetworks.repopipeline;

import java.util.Date;


public class CelInfo {
	private Date scanDate;
	private String platform;
	/**
	 * @return the scanDate
	 */
	public Date getScanDate() {
		return scanDate;
	}
	/**
	 * @param scanDate the scanDate to set
	 */
	public void setScanDate(Date scanDate) {
		this.scanDate = scanDate;
	}
	/**
	 * @return the platform
	 */
	public String getPlatform() {
		return platform;
	}
	/**
	 * @param platform the platform to set
	 */
	public void setPlatform(String platform) {
		this.platform = platform;
	}
	public CelInfo(Date scanDate, String platform) {
		super();
		this.scanDate = scanDate;
		this.platform = platform;
	}
	
	
}
