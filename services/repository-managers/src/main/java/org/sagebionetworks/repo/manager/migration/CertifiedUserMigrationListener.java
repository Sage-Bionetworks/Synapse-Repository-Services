package org.sagebionetworks.repo.manager.migration;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOGroupMembers;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CertifiedUserMigrationListener implements MigrationTypeListener<DBOGroupMembers> {


    private final MigratableTableDAO migratableTableDAO;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    public CertifiedUserMigrationListener(MigratableTableDAO migratableTableDAO,
                                          NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                          JdbcTemplate jdbcTemplate) {
        this.migratableTableDAO = migratableTableDAO;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean supports(MigrationType type) {
        return MigrationType.GROUP_MEMBERS.equals(type);
    }

    @Override
    public void beforeCreateOrUpdate(JdbcTemplate migrationJdbcTemplate, List<DBOGroupMembers> batch) {
    }

    @Override
    public void afterCreateOrUpdate(JdbcTemplate migrationJdbcTemplate, List<DBOGroupMembers> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        List<Long> certifiedUsers = batch.stream()
                .filter(dboGroupMembers ->
                        dboGroupMembers.getGroupId()
                                .equals(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()))
                .map(dboGroupMembers -> dboGroupMembers.getMemberId())
                .collect(Collectors.toList());

        if (certifiedUsers.isEmpty()) {
            return;
        }

        try {
            migratableTableDAO.runWithKeyChecksIgnored(() -> {
                createOrUpdate(certifiedUsers);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createOrUpdate(List<Long> userIds) {
        String sql = "INSERT IGNORE INTO CERTIFIED_USERS (USER_ID) VALUES (?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                // This maps the specific Long at index i to the '?'
                ps.setLong(1, userIds.get(i));
            }

            @Override
            public int getBatchSize() {
                return userIds.size();
            }
        });
    }
}
