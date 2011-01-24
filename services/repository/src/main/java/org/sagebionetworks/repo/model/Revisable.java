package org.sagebionetworks.repo.model;

public interface Revisable extends Base {
	public void setVersion(String version);

	public String getVersion();
}
