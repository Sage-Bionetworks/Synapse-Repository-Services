package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.util.TemporaryCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

@TemporaryCode(author = "john.hill@sagebase.org", comment = "One time migration of AR names.  Can be removed after all ARs have a name.")
public class AccessRequirementMigration {

	private JdbcTemplate template;

	public AccessRequirementMigration(JdbcTemplate template) {
		super();
		this.template = template;
	}

	/**
	 * Stream over all of the AR ids.
	 * 
	 * @return
	 */
	public Stream<Long> streamOverIds() {
		return this.template.queryForStream("SELECT ID FROM ACCESS_REQUIREMENT ORDER BY ID ASC",
				(ResultSet rs, int rowNum) -> rs.getLong("ID"));
	}

	/**
	 * Migrate one AR at a time.
	 * 
	 * @param arId
	 */
	public void migrate(Long arId) {
		AccessRequirementData data = getAccessRequirementData(arId);
		if (!StringUtils.isBlank(data.getDescription()) && !data.getName().equals(data.getDescription())) {
			System.out.println(String.format("Migrating AR: '%s'...", arId));
			try {
				this.template.update("UPDATE ACCESS_REQUIREMENT SET NAME = ?, ETAG = UUID() WHERE ID = ?",
						data.getDescription(), data.getId());
			} catch (DuplicateKeyException e) {
				// for duplicates we just log and keep the ID as the name
				System.out.println(String.format("Duplicate description: '%s' found for  AR: '%s'...",
						data.getDescription(), arId));
			}
		}
	}

	/**
	 * Get the AccessRequirementData for the given AR ID.
	 * 
	 * @param arId
	 * @return
	 */
	public AccessRequirementData getAccessRequirementData(Long arId) {
		return this.template.queryForObject(
				"SELECT ID, NAME, SERIALIZED_ENTITY" + " FROM ACCESS_REQUIREMENT A JOIN ACCESS_REQUIREMENT_REVISION R"
						+ " ON (A.ID = R.OWNER_ID AND A.CURRENT_REV_NUM = R.NUMBER) WHERE A.ID = ?",
				(ResultSet rs, int rowNum) -> new AccessRequirementData(rs, rowNum), arId);
	}

	/**
	 * Running this main will migrate the name of each access requirement for the
	 * configured stack.
	 * <li>args[0] = url ( jdbc:mysql://host/stack )</li>
	 * <li>args[1] = username</li>
	 * <li>args[2] = password</li>
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		BasicDataSource mainDataSource = new BasicDataSource();
		String connectionUrl = args[0];
		mainDataSource.setUrl(args[0]);
		mainDataSource.setUsername(args[1]);
		mainDataSource.setPassword(args[2]);

		System.out.println("Connecting to: " + connectionUrl + " ...");

		AccessRequirementMigration migration = new AccessRequirementMigration(new JdbcTemplate(mainDataSource));
		migration.streamOverIds().forEach((id) -> migration.migrate(id));
		System.out.println("Done");

	}

	public static class AccessRequirementData {
		private Long id;
		private String name;
		private String description;

		public AccessRequirementData(ResultSet rs, int rowNum) throws SQLException {
			this.id = rs.getLong("ID");
			this.name = rs.getString("NAME");
			this.description = AccessRequirementUtils.readSerializedField(rs.getBytes("SERIALIZED_ENTITY"))
					.getDescription();
		}

		public AccessRequirementData(Long id, String name, String description) {
			super();
			this.id = id;
			this.name = name;
			this.description = description;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		@Override
		public int hashCode() {
			return Objects.hash(description, id, name);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof AccessRequirementData)) {
				return false;
			}
			AccessRequirementData other = (AccessRequirementData) obj;
			return Objects.equals(description, other.description) && Objects.equals(id, other.id)
					&& Objects.equals(name, other.name);
		}

		@Override
		public String toString() {
			return "AccessRequirementData [id=" + id + ", name=" + name + ", description=" + description + "]";
		}

	}
}
