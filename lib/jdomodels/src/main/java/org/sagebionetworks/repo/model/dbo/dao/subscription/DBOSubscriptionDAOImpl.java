package org.sagebionetworks.repo.model.dbo.dao.subscription;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_ALIAS_DISPLAY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NOTIFICATION_EMAIL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_SUBSCRIBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_EMAIL_NOTIFICATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_FIRST_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_LAST_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NOTIFICATION_EMAIL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SUBSCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_PROFILE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FORUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DISCUSSION_THREAD;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_IS_DELETED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_FORUM_ID;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DBOSubscriptionDAOImpl implements SubscriptionDAO{

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private NamedParameterJdbcTemplate namedTemplate;
	@Autowired
	private IdGenerator idGenerator;
	
	private static final String SQL_GET_EMAIL_SUBSCRIBERS = "SELECT S."
			+ COL_SUBSCRIPTION_ID + ", S." + COL_SUBSCRIPTION_SUBSCRIBER_ID
			+ ", U." + COL_USER_PROFILE_FIRST_NAME + ", U."
			+ COL_USER_PROFILE_LAST_NAME + ", A1." + COL_BOUND_ALIAS_DISPLAY
			+ " AS 'EMAIL', A2." + COL_BOUND_ALIAS_DISPLAY
			+ " AS 'USERNAME' FROM " + TABLE_SUBSCRIPTION + " S, "
			+ TABLE_USER_PROFILE + " U, " + TABLE_NOTIFICATION_EMAIL + " N, "
			+ TABLE_PRINCIPAL_ALIAS + " A1, " + TABLE_PRINCIPAL_ALIAS
			+ " A2 WHERE S." + COL_SUBSCRIPTION_OBJECT_ID + " = ? AND S."
			+ COL_SUBSCRIPTION_OBJECT_TYPE + " = ? AND S."
			+ COL_SUBSCRIPTION_SUBSCRIBER_ID + " = U." + COL_USER_PROFILE_ID
			+ " AND U." + COL_USER_PROFILE_EMAIL_NOTIFICATION
			+ " = true AND A1." + COL_PRINCIPAL_ALIAS_PRINCIPAL_ID + " = S."
			+ COL_SUBSCRIPTION_SUBSCRIBER_ID + " AND N."
			+ COL_NOTIFICATION_EMAIL_ALIAS_ID + " = A1."
			+ COL_PRINCIPAL_ALIAS_ID + " AND A2."
			+ COL_PRINCIPAL_ALIAS_PRINCIPAL_ID + " = S."
			+ COL_SUBSCRIPTION_SUBSCRIBER_ID + " AND A2."
			+ COL_PRINCIPAL_ALIAS_TYPE + " = '" + AliasType.USER_NAME.name()
			+ "'";

	private static final String SQL_INSERT_IGNORE = "INSERT IGNORE INTO "
			+ TABLE_SUBSCRIPTION + " ( "
			+ COL_SUBSCRIPTION_ID + ", "
			+ COL_SUBSCRIPTION_SUBSCRIBER_ID + ", "
			+ COL_SUBSCRIPTION_OBJECT_ID + ", "
			+ COL_SUBSCRIPTION_OBJECT_TYPE + ", "
			+ COL_SUBSCRIPTION_CREATED_ON + ") "
			+ " VALUES (?, ?, ?, ?, ?)";

	private static final String SQL_GET = "SELECT * "
			+ "FROM "+TABLE_SUBSCRIPTION+" "
			+ "WHERE "+COL_SUBSCRIPTION_ID+" = ?";

	private static final String SQL_GET_BY_PRIMARY_KEY = "SELECT * "
			+ "FROM "+TABLE_SUBSCRIPTION+" "
			+ "WHERE "+COL_SUBSCRIPTION_SUBSCRIBER_ID+" = ? "
			+ "AND "+COL_SUBSCRIPTION_OBJECT_ID+" = ? "
			+ "AND "+COL_SUBSCRIPTION_OBJECT_TYPE+" = ?";

	private static final String SQL_GET_PROJECT_FORUM_SUB = "SELECT "+TABLE_FORUM+"."+COL_FORUM_PROJECT_ID+" "
			+"FROM "+TABLE_SUBSCRIPTION+", "+TABLE_FORUM+" "
			+ "WHERE "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_OBJECT_ID+" = "+TABLE_FORUM+"."+COL_FORUM_ID+" "
			+ "AND "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_SUBSCRIBER_ID+" = ? ";

	private static final String SQL_GET_PROJECT_THREAD_SUB = "SELECT "+TABLE_FORUM+"."+COL_FORUM_PROJECT_ID+" "
			+ "FROM "+TABLE_SUBSCRIPTION+", "+TABLE_DISCUSSION_THREAD+", "+TABLE_FORUM+" "
			+ "WHERE "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_OBJECT_ID+" = "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID+" "
			+ "AND "+TABLE_FORUM+"."+COL_FORUM_ID+" = "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_FORUM_ID+" "
			+ "AND "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_IS_DELETED+" = "+Boolean.FALSE.toString()+" "
			+ "AND "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_SUBSCRIBER_ID+" = ? ";

	private static final String SQL_GET_ALL_FORUM_SUB = "SELECT * "
			+ "FROM "+TABLE_SUBSCRIPTION+", "+TABLE_FORUM+" "
			+ "WHERE "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_OBJECT_ID+" = "+TABLE_FORUM+"."+COL_FORUM_ID+" "
			+ "AND "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_SUBSCRIBER_ID+" = :subscriberId "
			+ "AND "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_OBJECT_TYPE+" = \""+SubscriptionObjectType.FORUM.toString()+"\" "
			+ "AND "+TABLE_FORUM+"."+COL_FORUM_PROJECT_ID+" IN (:projectIds) ";

	private static final String SQL_GET_ALL_THREAD_SUB = "SELECT * "
			+ "FROM "+TABLE_SUBSCRIPTION+", "+TABLE_DISCUSSION_THREAD+", "+TABLE_FORUM+" "
			+ "WHERE "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_OBJECT_ID+" = "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID+" "
			+ "AND "+TABLE_FORUM+"."+COL_FORUM_ID+" = "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_FORUM_ID+" "
			+ "AND "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_IS_DELETED+" = "+Boolean.FALSE.toString()+" "
			+ "AND "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_SUBSCRIBER_ID+" = :subscriberId "
			+ "AND "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_OBJECT_TYPE+" = \""+SubscriptionObjectType.THREAD.toString()+"\" "
			+ "AND "+TABLE_FORUM+"."+COL_FORUM_PROJECT_ID+" IN (:projectIds) ";

	private static final String LIMIT_OFFSET = "LIMIT :limit OFFSET :offset";

	private static final String SQL_GET_FORUM_SUB_LIST = "SELECT * "
			+ "FROM "+TABLE_SUBSCRIPTION+", "+TABLE_FORUM+" "
			+ "WHERE "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_OBJECT_ID+" = "+TABLE_FORUM+"."+COL_FORUM_ID+" "
			+ "AND "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_SUBSCRIBER_ID+" = :subscriberId "
			+ "AND "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_OBJECT_TYPE+" = :objectType "
			+ "AND "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_OBJECT_ID+" IN ( :ids )";

	private static final String SQL_GET_THREAD_SUB_LIST = "SELECT * "
			+ "FROM "+TABLE_SUBSCRIPTION+", "+TABLE_DISCUSSION_THREAD+" "
			+ "WHERE "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_OBJECT_ID+" = "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_ID+" "
			+ "AND "+TABLE_DISCUSSION_THREAD+"."+COL_DISCUSSION_THREAD_IS_DELETED+" = "+Boolean.FALSE.toString()+" "
			+ "AND "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_SUBSCRIBER_ID+" = :subscriberId "
			+ "AND "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_OBJECT_TYPE+" = :objectType "
			+ "AND "+TABLE_SUBSCRIPTION+"."+COL_SUBSCRIPTION_OBJECT_ID+" IN ( :ids )";

	private static final String SQL_DELETE = "DELETE FROM "+TABLE_SUBSCRIPTION+" "
			+ "WHERE "+COL_SUBSCRIPTION_ID+" = ?";

	private static final String SQL_DELETE_ALL = "DELETE FROM "+TABLE_SUBSCRIPTION+" "
			+ "WHERE "+COL_SUBSCRIPTION_SUBSCRIBER_ID+" = ?";

	private static final String QUERY_FOR_TOPIC = "FROM "+TABLE_SUBSCRIPTION+" "
			+ "WHERE "+COL_SUBSCRIPTION_OBJECT_ID+" = ? "
			+ "AND "+COL_SUBSCRIPTION_OBJECT_TYPE+" = ?";

	private static final String SQL_GET_SUBSCRIBERS = "SELECT "+COL_SUBSCRIPTION_SUBSCRIBER_ID+" "
			+ QUERY_FOR_TOPIC;

	private static final String SQL_GET_SUBSCRIBERS_LIMIT_AND_OFFSET = SQL_GET_SUBSCRIBERS
			+" LIMIT ? OFFSET ?";

	private static final String SQL_GET_SUBSCRIBER_COUNT =
			"SELECT COUNT(DISTINCT "+COL_SUBSCRIPTION_SUBSCRIBER_ID+") "
			+ QUERY_FOR_TOPIC;

	private static final RowMapper<Subscription> ROW_MAPPER = new RowMapper<Subscription>(){

		@Override
		public Subscription mapRow(ResultSet rs, int rowNum) throws SQLException {
			Subscription subscription = new Subscription();
			subscription.setSubscriptionId(""+rs.getLong(COL_SUBSCRIPTION_ID));
			subscription.setSubscriberId(""+rs.getLong(COL_SUBSCRIPTION_SUBSCRIBER_ID));
			subscription.setObjectId(""+rs.getLong(COL_SUBSCRIPTION_OBJECT_ID));
			subscription.setObjectType(SubscriptionObjectType.valueOf(rs.getString(COL_SUBSCRIPTION_OBJECT_TYPE)));
			subscription.setCreatedOn(new Date(rs.getLong(COL_SUBSCRIPTION_CREATED_ON)));
			return subscription;
		}
	};

	@WriteTransactionReadCommitted
	@Override
	public Subscription create(String subscriberId, String objectId,
			SubscriptionObjectType objectType) {
		ValidateArgument.required(subscriberId, "subscriberId");
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		long subscriptionId = idGenerator.generateNewId(IdType.SUBSCRIPTION_ID);
		jdbcTemplate.update(SQL_INSERT_IGNORE, subscriptionId, subscriberId, objectId, objectType.name(), new Date().getTime());
		return get(subscriberId, objectId, objectType);
	}

	private Subscription get(String subscriberId, String objectId,
			SubscriptionObjectType objectType) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_BY_PRIMARY_KEY, new Object[]{subscriberId, objectId, objectType.name()}, ROW_MAPPER);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(e.getMessage());
		}
	}

	@Override
	public Subscription get(long subscriptionId) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET, new Object[]{subscriptionId}, ROW_MAPPER);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(e.getMessage());
		}
	}

	@Override
	public SubscriptionPagedResults getAll(String subscriberId, Long limit,
			Long offset, SubscriptionObjectType objectType, Set<Long> projectIds) {
		ValidateArgument.required(subscriberId, "subscriberId");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(projectIds, "projectIds");
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		if (projectIds.isEmpty()) {
			results.setResults(new ArrayList<Subscription>());
			results.setTotalNumberOfResults(0L);
			return results;
		}
		String query = getAllQuery(objectType);
		String countQuery = getCountQuery(query);
		MapSqlParameterSource parameters = new MapSqlParameterSource("projectIds", projectIds);
		parameters.addValue("subscriberId", subscriberId);
		results.setTotalNumberOfResults(namedTemplate.queryForObject(countQuery, parameters, Long.class));
		parameters.addValue("limit", limit);
		parameters.addValue("offset", offset);
		results.setResults(namedTemplate.query(query+LIMIT_OFFSET, parameters, ROW_MAPPER));
		return results;
	}

	public String getCountQuery(String query) {
		return query.replace("*", "COUNT(*)");
	}

	public String getAllQuery(SubscriptionObjectType objectType) {
		switch (objectType) {
			case FORUM:
				return SQL_GET_ALL_FORUM_SUB;
			case THREAD:
				return SQL_GET_ALL_THREAD_SUB;
			default:
				throw new RuntimeException("Unsopported type "+objectType.name());
		}
	}

	@Override
	public SubscriptionPagedResults getSubscriptionList(String subscriberId,
			SubscriptionObjectType objectType, List<String> ids) {
		ValidateArgument.required(subscriberId, "subscriberId");
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(ids, "ids");
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		if (ids.isEmpty()) {
			results.setResults(new ArrayList<Subscription>(0));
			results.setTotalNumberOfResults(0L);
		} else {
			String query = getListQuery(objectType);
			MapSqlParameterSource parameters = new MapSqlParameterSource("ids", ids);
			parameters.addValue("subscriberId", subscriberId);
			parameters.addValue("objectType", objectType.name());
			List<Subscription> subscriptions = namedTemplate.query(query, parameters, ROW_MAPPER);
			results.setResults(subscriptions);
			results.setTotalNumberOfResults((long) subscriptions.size());
		}
		return results;
	}

	public String getListQuery(SubscriptionObjectType objectType) {
		ValidateArgument.required(objectType, "objectType");
		switch (objectType) {
			case FORUM:
				return SQL_GET_FORUM_SUB_LIST;
			case THREAD:
				return SQL_GET_THREAD_SUB_LIST;
			default:
				throw new RuntimeException("Unsopported type "+objectType.name());
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public void delete(long subscriptionId) {
		jdbcTemplate.update(SQL_DELETE, subscriptionId);
	}

	@WriteTransactionReadCommitted
	@Override
	public void deleteAll(Long userId) {
		jdbcTemplate.update(SQL_DELETE_ALL, userId);
	}

	@WriteTransactionReadCommitted
	@Override
	public void subscribeAllUsers(Set<String> subscribers, final String objectId, final SubscriptionObjectType objectType) {
		ValidateArgument.required(subscribers, "subscribers");
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		if (subscribers.isEmpty()) {
			return;
		}
		jdbcTemplate.batchUpdate(SQL_INSERT_IGNORE, subscribers, subscribers.size(), new ParameterizedPreparedStatementSetter<String>(){

			@Override
			public void setValues(PreparedStatement ps, String subscriberId)
					throws SQLException {
				ps.setLong(1, Long.parseLong(idGenerator.generateNewId(IdType.SUBSCRIPTION_ID).toString()));
				ps.setLong(2, Long.parseLong(subscriberId));
				ps.setLong(3, Long.parseLong(objectId));
				ps.setString(4, objectType.name());
				ps.setLong(5, new Date().getTime());
			}
		});
	}

	@Override
	public List<String> getAllSubscribers(String objectId, SubscriptionObjectType objectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		return jdbcTemplate.queryForList(SQL_GET_SUBSCRIBERS, new Object[]{objectId, objectType.name()}, String.class);
	}

	@Override
	public List<Subscriber> getAllEmailSubscribers(String objectId,
			SubscriptionObjectType objectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		return jdbcTemplate.query(SQL_GET_EMAIL_SUBSCRIBERS, new RowMapper<Subscriber>(){

			@Override
			public Subscriber mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				Subscriber sub = new Subscriber();
				sub.setFirstName(rs.getString(COL_USER_PROFILE_FIRST_NAME));
				sub.setLastName(rs.getString(COL_USER_PROFILE_LAST_NAME));
				sub.setSubscriberId(rs.getString(COL_SUBSCRIPTION_SUBSCRIBER_ID));
				sub.setSubscriptionId(rs.getString(COL_SUBSCRIPTION_ID));
				sub.setNotificationEmail(rs.getString("EMAIL"));
				sub.setUsername(rs.getString("USERNAME"));
				return sub;
			}},objectId, objectType.name());
	}

	@Override
	public Set<Long> getAllProjects(String userId, SubscriptionObjectType objectType) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(objectType, "objectType");
		Set<Long> results = new HashSet<Long>();
		results.addAll(jdbcTemplate.queryForList(getProjectQuery(objectType), Long.class, userId));
		return results;
	}

	public String getProjectQuery(SubscriptionObjectType objectType) {
		ValidateArgument.required(objectType, "objectType");
		switch (objectType) {
			case FORUM:
				return SQL_GET_PROJECT_FORUM_SUB;
			case THREAD:
				return SQL_GET_PROJECT_THREAD_SUB;
			default:
				throw new RuntimeException("Unsopported type "+objectType.name());
		}
	}

	@Override
	public List<String> getSubscribers(String objectId, SubscriptionObjectType objectType, long limit, long offset) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		return jdbcTemplate.queryForList(SQL_GET_SUBSCRIBERS_LIMIT_AND_OFFSET,
				new Object[]{objectId, objectType.name(), limit, offset}, String.class);
	}

	@Override
	public long getSubscriberCount(String objectId, SubscriptionObjectType objectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		return jdbcTemplate.queryForObject(SQL_GET_SUBSCRIBER_COUNT, Long.class, objectId, objectType.name());
	}
}
