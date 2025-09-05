package org.sagebionetworks.repo.manager.agent.handler;

public enum HttpCode {
	ok(200), created(201), no_content(204);

	private HttpCode(int code) {
		this.code = code;
	}

	private int code;

	public int getCode() {
		return code;
	}
	
}
