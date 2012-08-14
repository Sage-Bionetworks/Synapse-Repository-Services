package org.sagebionetworks.sweeper;

import java.io.IOException;

public interface EC2IdProvider {

	public abstract String getEC2InstanceId() throws IOException;

}
