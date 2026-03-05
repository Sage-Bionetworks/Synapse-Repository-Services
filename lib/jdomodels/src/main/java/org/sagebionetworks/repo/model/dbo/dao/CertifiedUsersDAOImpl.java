package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.CertifiedUsersDAO;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;


@Repository
public class CertifiedUsersDAOImpl implements CertifiedUsersDAO {

    private static final String USER_IDS = "UserIds";


    private JdbcTemplate jdbcTemplate;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;


    @Autowired
    public CertifiedUsersDAOImpl(JdbcTemplate jdbcTemplate,
                                 NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public static final String ADD_CERTIFIED_USERS =
            "INSERT IGNORE INTO " + SqlConstants.TABLE_CERTIFIED_USERS +
                    "(" + SqlConstants.COL_CERTIFIED_USERS_USER_ID + ")  VALUES (?)";

    private static final String DELETE_CERTIFIED_USERS =
            "DELETE FROM " + SqlConstants.TABLE_CERTIFIED_USERS +
                    " WHERE " + SqlConstants.COL_CERTIFIED_USERS_USER_ID + "=?";

    private static final String GET_CERTIFIED_USERS =
            "SELECT COUNT(*) FROM " + SqlConstants.TABLE_CERTIFIED_USERS +
                    " WHERE " + SqlConstants.COL_CERTIFIED_USERS_USER_ID + " IN (:" + USER_IDS + ")";

    @Override
    public void addCertifiedUser(Long userId, boolean isIndividual) throws IllegalArgumentException {
        if (!isIndividual) {
            throw new IllegalArgumentException("Only individuals can be added as certified users.");
        }
        jdbcTemplate.update(ADD_CERTIFIED_USERS, userId);
    }

    @Override
    public void removeCertifiedUser(Long userId) {
            jdbcTemplate.update(DELETE_CERTIFIED_USERS, userId);
    }

    @Override
    public boolean areAllCertifiedUsers(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return false;
        }
        try {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(USER_IDS, userIds);
            Integer count = namedParameterJdbcTemplate.queryForObject(GET_CERTIFIED_USERS, params, Integer.class);
            return count.equals(userIds.size());
        } catch (EmptyResultDataAccessException e) {
            // If any of the userIds do not exist, they cannot be certified users
            return false;
        }

    }

    @Override
    public boolean isCertifiedUser(String userId) {
        if (userId == null) {
            return false;
        }
        return areAllCertifiedUsers(Set.of(userId));
    }
}
