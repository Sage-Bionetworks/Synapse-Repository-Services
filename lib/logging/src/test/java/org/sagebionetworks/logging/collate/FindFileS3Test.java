package org.sagebionetworks.logging.collate;

import static org.junit.Assert.*;

import org.junit.Test;

public class FindFileS3Test {

	@Test
	public void test() {
		new FindFileS3().checkS3Bucket();
	}

}
