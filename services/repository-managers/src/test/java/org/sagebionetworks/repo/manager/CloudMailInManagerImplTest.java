package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;

import org.junit.Test;

public class CloudMailInManagerImplTest {

	@Test
	public void testCreateEmailBody() throws Exception {
		MessageToUserAndBody mtub = CloudMailInManagerImpl.
				createEmailBody("this is a test", null, null, "0", "https://www.synapse.org/#");
		System.out.println(mtub.getMimeType());
	}

}
