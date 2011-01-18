package org.sagebionetworks.repo.model;

import java.util.Date;

public interface Base {
	public void setId(String id);
	public String getId();
	public void setCreationDate(Date createDate);
	public Date getCreationDate();
}
