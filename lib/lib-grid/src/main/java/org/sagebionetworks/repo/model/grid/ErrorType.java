package org.sagebionetworks.repo.model.grid;

public enum ErrorType {
	
	BAD_REQUEST("Bad Request", 400),
	SERVER_ERROR("Internal Server Error", 500);
	
	ErrorType(String code, Integer errno) {
		this.code = code;
		this.errno = errno;
	}
	private String code;
	private Integer errno;
	
	public String getCode() {
		return code;
	}
	public Integer getErrno() {
		return errno;
	}

	
}
