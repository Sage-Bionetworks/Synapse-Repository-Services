package org.sagebionetworks.usagemetrics;

/**
 * Hello world!
 *
 */
public class AuthEvent 
{
	private String userName;
	private long timestamp;
	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}
	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public AuthEvent(String userName, long timestamp) {
		super();
		this.userName = userName;
		this.timestamp = timestamp;
	}
	
}
