package org.sagebionetworks.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.function.Consumer;

public class RandomTempFileUtil {

	/**
	 * Create a random temporary file of the provided size. The file will be passed
	 * to the provided consumer and then unconditionally deleted.
	 * 
	 * @param sizeBytes The number of random bytes of the new temporary file.
	 * @param prefix    The temporary file name prefix
	 * @param suffix    The temporary file name suffix.
	 * @param consumer  The consumer of the temporary file.
	 */
	public static void consumeRandomTempFile(int sizeBytes, String prefix, String suffix, Consumer<File> consumer) {
		try {
			Random rand = new Random();
			File temp = File.createTempFile(prefix, suffix);
			try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(temp))) {
				for (int i = 0; i < sizeBytes; i++) {
					out.write(rand.nextInt());
				}
			}
			try {
				consumer.accept(temp);
			} finally {
				temp.delete();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
