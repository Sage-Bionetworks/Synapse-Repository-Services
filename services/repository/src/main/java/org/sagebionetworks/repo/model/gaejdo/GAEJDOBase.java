package org.sagebionetworks.repo.model.gaejdo;

import java.util.Date;

import org.sagebionetworks.repo.model.Base;
import com.google.appengine.api.datastore.Key;

public interface GAEJDOBase {
	public void setId(Key id);
	public Key getId();
	public void setCreationDate(Date d);
	public Date getCreationDate();
}
