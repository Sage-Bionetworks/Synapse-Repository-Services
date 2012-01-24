package org.sagebionetworks.cli;

import org.junit.Test;
import org.sagebionetworks.client.exceptions.SynapseException;

/**
 * @author deflaux
 *
 */
public class UploadFileTest {

	/**
	 * @throws Exception
	 */
	@Test(expected=SynapseException.class)
	public void noArgs() throws Exception {
		new UploadFile(null);
	}

}
