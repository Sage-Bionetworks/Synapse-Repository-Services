package org.sagebionetworks.lib.dbuserhelper;

import org.sagebionetworks.StackConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Static utility for creating database read-only users.
 * Used by both the main database (DBOBasicDaoImpl) and tables database (ConnectionFactoryImpl).
 */
public class DBUserHelper {
	private static final String CREATE_USER = "CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED BY '%s'";
	private static final String GRANT_PROCESS_SELECT_USER = "GRANT PROCESS, SELECT ON *.* TO '%s'@'%%'";
	public static final String GRANT_EXECUTE_USER = "GRANT EXECUTE ON %s.* TO '%s'@'%%'";

	// Private constructor to prevent instantiation
	private DBUserHelper() {
	}

	/**
	 * Creates a read-only database user using credentials from StackConfiguration.
	 */
	public static void createDbReadOnlyUser(JdbcTemplate template, StackConfiguration stackConfiguration) {
		String userName = stackConfiguration.getDbReadOnlyUserName();
		String password = stackConfiguration.getDbReadOnlyPassword();
		createReadOnlyUser(template, userName, password, stackConfiguration);
	}

	/**
	 * Creates a read-only database user with the specified credentials.
	 */
	public static void createReadOnlyUser(JdbcTemplate template, String userName, String password,
			StackConfiguration stackConfiguration) {
		String sqlCreateUSer = String.format(CREATE_USER, userName, password);
		template.update(sqlCreateUSer);
		String sqlGrantUser = String.format(GRANT_PROCESS_SELECT_USER, userName);
		template.update(sqlGrantUser);
		String stack = stackConfiguration.getStack();
		String stackInstance = stackConfiguration.getStackInstance();
		String dbName = String.format("%s%s", stack, stackInstance);
		String sqlGrantExecute = String.format(GRANT_EXECUTE_USER, dbName, userName);
		template.update(sqlGrantExecute);
	}
}
