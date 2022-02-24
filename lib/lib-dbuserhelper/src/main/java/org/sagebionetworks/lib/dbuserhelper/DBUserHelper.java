package org.sagebionetworks.lib.dbuserhelper;

import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DBUserHelper {
	private static final String CREATE_USER = "CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED BY '%s'";
	private static final String GRANT_SELECT_USER = "GRANT SELECT ON *.* TO '%s'@'%%'";
	public static final String GRANT_EXECUTE_USER = "GRANT EXECUTE ON FUNCTION %s TO '%s'@'%%'";
	public static final String FCT_NAME_GETENTITYBENEFACTORID = "getEntityBenefactorId";
	public static final String FCT_NAME_GETENTITYPROJECTID = "getEntityProjectId";

	private final StackConfiguration stackConfiguration;

	@Autowired
	public DBUserHelper(StackConfiguration config) {
		this.stackConfiguration = config;
	}

	public void createDbReadOnlyUser(JdbcTemplate template) {
		String userName = stackConfiguration.getDbReadOnlyUserName();
		String password = stackConfiguration.getDbReadOnlyPassword();
		this.createReadOnlyUser(template, userName, password);
	}

	public void createReadOnlyUser(JdbcTemplate template, String userName, String password) {
		String sqlCreateUSer = String.format(CREATE_USER, userName, password);
		template.update(sqlCreateUSer);
		String sqlGrantUser = String.format(GRANT_SELECT_USER, userName);
		template.update(sqlGrantUser);
		String stack = stackConfiguration.getStack();
		String stackInstance = stackConfiguration.getStackInstance();
		String fctName = String.format("%s%s.%s", stack, stackInstance, FCT_NAME_GETENTITYPROJECTID);
		String sqlGrantExecute = String.format(GRANT_EXECUTE_USER, fctName, userName);
		template.update(sqlGrantExecute);
		fctName = String.format("%s%s.%s", stack, stackInstance, FCT_NAME_GETENTITYBENEFACTORID);
		sqlGrantExecute = String.format(GRANT_EXECUTE_USER, fctName, userName);
		template.update(sqlGrantExecute);
	}
}
