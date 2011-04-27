package org.sagebionetworks.repo.model.jdo.temp;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.jdo.JDOAnnotatable;

public interface TempAnnotationsDao {
	
	public Annotations getAnnotations(Class<JDOAnnotatable> ownerClass, String ownerId);
	
	public void setAnnoations(Class<JDOAnnotatable> ownerClass, String ownerId, Annotations newValues);


}
