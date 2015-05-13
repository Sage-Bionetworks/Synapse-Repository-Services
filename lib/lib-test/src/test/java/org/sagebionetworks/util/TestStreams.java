package org.sagebionetworks.util;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;

public class TestStreams {
	public static InputStream randomStream(final long size) {
		return randomStream(size, 0L);
	}

	public static InputStream randomStream(final long size, final long seed) {
		return new InputStream() {
			Random random = seed == 0 ? new Random() : new Random(seed);
			long count = size;

			@Override
			public int read() throws IOException {
				if (count-- > 0) {
					return random.nextInt(256);
				} else {
					return -1;
				}
			}
		};
	}
	
	public static byte[] randomByteArray(final long size, final long seed) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			IOUtils.copy(randomStream(size, seed), baos);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				baos.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void assertEquals(InputStream expected, InputStream actual) {
		try {
			long index = 0;
			for (;;) {
				int expectedByte = expected.read();
				int actualByte = actual.read();
				if (expectedByte == -1) {
					Assert.assertEquals("expected stream of length " + index + " but was longer", -1, actualByte);
					break;
				} else if (actualByte == -1) {
					fail("actual stream is length " + index + " but was expected to be longer");
				}
				Assert.assertEquals("streams differ at index " + index, expectedByte, actualByte);
			}
		} catch (IOException e) {
			fail("Failed with exception " + e.getMessage());
		}
	}
}
