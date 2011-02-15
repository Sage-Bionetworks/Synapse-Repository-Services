package org.sagebionetworks.auth;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class User {
	private String userId;
	private String pw;
	private String email;
	private String fname;
	private String lname;
	private String displayName;
	
	public User() {}
	
//	public User(String user, String pw) {
//		this.user=user;
//		this.pw=pw;
//	}
//	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getPw() {
		return pw;
	}
	public void setPw(String pw) {
		this.pw = pw;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFname() {
		return fname;
	}

	public void setFname(String fname) {
		this.fname = fname;
	}

	public String getLname() {
		return lname;
	}

	public void setLname(String lname) {
		this.lname = lname;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
}
