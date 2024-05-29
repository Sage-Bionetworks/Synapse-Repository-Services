package org.sagebionetworks.database.semaphore;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;

import org.apache.commons.io.IOUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public class Utils {

	/**
	 * Simple utility to load a class path file as a string.
	 * 
	 * @param fileName
	 * @return
	 */
	public static String loadStringFromClassPath(String fileName) {
		InputStream in = Utils.class.getClassLoader().getResourceAsStream(
				fileName);
		if (in == null) {
			throw new IllegalArgumentException("Cannot find: " + fileName
					+ " on the classpath");
		}
		try {
			return IOUtils.toString(in, "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Validate the result == 1, indicating a single row was updated.
	 * 
	 * @param key
	 * @param token
	 * @param result
	 * @throws LockKeyNotFoundException
	 *             for a result < 1
	 * @throws LockReleaseFailedException
	 *             for a result == 0
	 */
	public static void validateResults(final String key, final String token,
			int result) {
		if (result < 0) {
			throw new LockKeyNotFoundException("Key not found: " + key);
		} else if (result == 0) {
			throw new LockReleaseFailedException("Key: " + key + " token: "
					+ token + " has expired.");
		}
	}

	/**
	 * Validate the result == 1, indicating a single row was updated.
	 * 
	 * @param key
	 * @param token
	 * @param result
	 */
	public static void validateNotExpired(final String key, final String token,
			int result) {
		if (result < 0) {
			throw new LockKeyNotFoundException("Key not found: " + key);
		} else if (result == 0) {
			throw new LockExpiredException("Key: " + key + " token: " + token
					+ " has expired.");
		}
	}

	/**
	 * Create a READ_COMMITED transaction template.
	 * 
	 * @param transactionManager
	 *            The transaction manager used to actually manage the transactions.
	 * 
	 * @param name
	 *            The name of the template.
	 * @return
	 */
	public static TransactionTemplate createReadCommitedTransactionTempalte(
			PlatformTransactionManager transactionManager, String name) {
		if (transactionManager == null) {
			throw new IllegalArgumentException(
					"TransactionManager cannot be null");
		}
		DefaultTransactionDefinition transactionDef = new DefaultTransactionDefinition();
		transactionDef.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
		transactionDef.setReadOnly(false);
		transactionDef
				.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transactionDef.setName(name);
		return new TransactionTemplate(transactionManager, transactionDef);
	}
}
