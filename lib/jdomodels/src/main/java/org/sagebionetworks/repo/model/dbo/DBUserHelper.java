package org.sagebionetworks.repo.model.dbo;

import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

public class DBUserHelper {

	private static final String CREATE_USER = "CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED BY '%s'";
	private static final String GRANT_SELECT_USER = "GRANT SELECT ON *.* TO '%s'@'%%'";
	private static final String DROP_USER = "DROP USER IF EXISTS '%s'@'%%'";
	private static final String CHECK_USER_EXISTS = "SELECT * FROM mysql.user WHERE user='%s'";
	private static final String SHOW_GRANTS_FOR_USER = "SHOW GRANTS for '%s'@'%%'";

	@Autowired
	StackConfiguration stackConfiguration;

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
	}

	public void dropUser(JdbcTemplate template, String userName) {
		String sqlDropUser = String.format(DROP_USER, userName);
		template.update(sqlDropUser);
	}

	public boolean doesUserExist(JdbcTemplate template, String userName) {
		String sql = String.format(CHECK_USER_EXISTS, userName);
		List<Map<String, Object>> list = template.queryForList(sql);
		return list.size() == 1; // Should only have 'userName'@'%'
	}

	public List<String> showGrantsForUser(JdbcTemplate template, String userName) {
		String sql = String.format(SHOW_GRANTS_FOR_USER, userName);
		List<String> list = template.queryForList(sql, String.class);
		return list;
	}

}
