package org.sagebionetworks.repo.model.jdo.aw;

import org.springframework.beans.factory.InitializingBean;

public interface JDOBootstrapper extends InitializingBean {

	public void bootstrap() throws Exception;
}
