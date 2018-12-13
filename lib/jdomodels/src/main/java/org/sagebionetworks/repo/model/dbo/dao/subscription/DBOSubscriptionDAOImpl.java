package org.sagebionetworks.repo.model.dbo.dao.subscription;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_FORUM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DISCUSSION_THREAD_IS_DELETED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NOTIFICATION_EMAIL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_DISPLAY;
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
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DISCUSSION_THREAD;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FORUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NOTIFICATION_EMAIL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SUBSCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_PROFILE;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionListRequest;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.subscription.SortByType;
import org.sagebionetworks.repo.model.subscription.SortDirection;
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

	public static final String OFFSET = "offset";
	public static final String LIMIT = "limit";
	public static final String PROJECT_IDS = "projectIds";
	public static final String OBJECT_IDS = "objectIds";
	public static final String OBJECT_TYPE = "objectType";
	public static final String SUBSCRIBER_ID = "subscriberId";
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private NamedParameterJdbcTemplate namedTemplate;
	@Autowired
	private IdGenerator idGenerator;
	
	private static final String SQL_GET_EMAIL_SUBSCRIBERS = "SELECT S."
			+ COL_SUBSCRIPTION_ID + ", S." + COL_SUBSCRIPTION_SUBSCRIBER_ID
			+ ", U." + COL_USER_PROFILE_FIRST_NAME + ", U."
			+ COL_USER_PROFILE_LAST_NAME + ", A1." + COL_PRINCIPAL_ALIAS_DISPLAY
			+ " AS 'EMAIL', A2." + COL_PRINCIPAL_ALIAS_DISPLAY
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
		ValidateArgument.required(subscriberId, SUBSCRIBER_ID);
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, OBJECT_TYPE);
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



	public String getCountQuery(String query) {
		return query.replace("*", "COUNT(*)");
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
		ValidateArgument.required(objectType, OBJECT_TYPE);
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
		ValidateArgument.required(objectType, OBJECT_TYPE);
		return jdbcTemplate.queryForList(SQL_GET_SUBSCRIBERS, new Object[]{objectId, objectType.name()}, String.class);
	}

	@Override
	public List<Subscriber> getAllEmailSubscribers(String objectId,
			SubscriptionObjectType objectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, OBJECT_TYPE);
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
	public Set<Long> getAllProjectsUserHasForumSubs(String userId) {
		ValidateArgument.required(userId, "userId");
		Set<Long> results = new HashSet<Long>();
		results.addAll(jdbcTemplate.queryForList(SQL_GET_PROJECT_FORUM_SUB, Long.class, userId));
		return results;
	}

	@Override
	public Set<Long> getAllProjectsUserHasThreadSubs(String userId) {
		ValidateArgument.required(userId, "userId");
		Set<Long> results = new HashSet<Long>();
		results.addAll(jdbcTemplate.queryForList(SQL_GET_PROJECT_THREAD_SUB, Long.class, userId));
		return results;
	}

	@Override
	public List<String> getSubscribers(String objectId, SubscriptionObjectType objectType, long limit, long offset) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, OBJECT_TYPE);
		return jdbcTemplate.queryForList(SQL_GET_SUBSCRIBERS_LIMIT_AND_OFFSET,
				new Object[]{objectId, objectType.name(), limit, offset}, String.class);
	}

	@Override
	public long getSubscriberCount(String objectId, SubscriptionObjectType objectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, OBJECT_TYPE);
		return jdbcTemplate.queryForObject(SQL_GET_SUBSCRIBER_COUNT, Long.class, objectId, objectType.name());
	}

	@Override
	public List<Subscription> listSubscriptions(SubscriptionListRequest request) {
		if(willYieldEmptyResult(request)) {
			return new LinkedList<>();
		}
		MapSqlParameterSource parameters = createParameters(request);
		String query = createQuery(request);
		return namedTemplate.query(query, parameters, ROW_MAPPER);
	}
	
	@Override
	public Long listSubscriptionsCount(SubscriptionListRequest request) {
		if(willYieldEmptyResult(request)) {
			return 0L;
		}
		MapSqlParameterSource parameters = createParameters(request);
		String countQuery = createCountQuery(request);
		return namedTemplate.queryForObject(countQuery, parameters, Long.class);
	}
	
	/**
	 * Will the given request yield an empty result?
	 * 
	 * @param request
	 * @return
	 */
	public static boolean willYieldEmptyResult(SubscriptionListRequest request) {
		if(request.getProjectIds() != null) {
			return request.getProjectIds().isEmpty();
		}
		if(request.getObjectIds() != null) {
			return request.getObjectIds().isEmpty();
		}
		return false;
	}
	
	/**
	 * Create the parameters for the given request.
	 * 
	 * @param request
	 * @return
	 */
	static MapSqlParameterSource createParameters(SubscriptionListRequest request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getObjectType(), "ObjectType");
		ValidateArgument.required(request.getSubscriberId(), "subscriberId");
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue(SUBSCRIBER_ID, request.getSubscriberId());
		parameters.addValue(OBJECT_TYPE, request.getObjectType().name());
		if(request.getObjectIds() != null) {
			parameters.addValue(OBJECT_IDS, request.getObjectIds());
		}
		if(request.getProjectIds() != null) {
			parameters.addValue(PROJECT_IDS, request.getProjectIds());
		}
		if(request.getLimit() != null) {
			parameters.addValue(LIMIT, request.getLimit());
		}
		if(request.getOffset() != null) {
			parameters.addValue(OFFSET, request.getOffset());
		}
		return parameters;
	}
	
	/**
	 * Create the count query for the given request.
	 * 
	 * @param request
	 * @return
	 */
	static String createCountQuery(SubscriptionListRequest request) {
		StringBuilder builder = new StringBuilder("SELECT COUNT(*)");
		createQueryCore(builder, request);
		return builder.toString();
	}
	
	/**
	 * Create the query for the given request.
	 * @param request
	 * @return
	 */
	static String createQuery(SubscriptionListRequest request) {
		StringBuilder builder = new StringBuilder("SELECT *");
		createQueryCore(builder, request);
		if(request.getSortByType() != null) {
			builder.append(" ORDER BY S.").append(getColunNameForSortType(request.getSortByType()));
			if(request.getSortDirection() != null) {
				builder.append(" ").append(getSortDirection(request.getSortDirection()));
			}
		}
		if(request.getLimit() != null) {
			builder.append(" LIMIT :").append(LIMIT);
			if(request.getOffset() != null) {
				builder.append(" OFFSET :").append(OFFSET);
			}
		}
		return builder.toString();
	}
	
	/**
	 * Create the SQL query without the select, sorting, and pagination.
	 * 
	 * @param builder
	 * @param request
	 */
	static void createQueryCore(StringBuilder builder, SubscriptionListRequest request) {
		ValidateArgument.required("request", "SubscriptionListRequest");
		ValidateArgument.required(request.getObjectType(), "request.objectType");
		ValidateArgument.required(request.getSubscriberId(), "request.subscriberId");
		builder.append(" FROM ").append(TABLE_SUBSCRIPTION).append(" S");
		addTypeSpecificSql(builder, request.getObjectType(), request.getProjectIds());
		builder.append(" WHERE S.").append(COL_SUBSCRIPTION_OBJECT_TYPE).append(" = :").append(OBJECT_TYPE);
		builder.append(" AND S.").append(COL_SUBSCRIPTION_SUBSCRIBER_ID).append(" = :").append(SUBSCRIBER_ID);
		if(request.getObjectIds() != null) {
			builder.append(" AND S.").append(COL_SUBSCRIPTION_OBJECT_ID).append(" IN (:").append(OBJECT_IDS).append(")");
		}
	}

	/**
	 * Add SubscriptionObjectType specific SQL to the passed builder.
	 * @param builder
	 * @param objectType
	 * @param projectIds
	 */
	static void addTypeSpecificSql(StringBuilder builder, SubscriptionObjectType objectType, Set<Long> projectIds) {
		ValidateArgument.required(objectType, "SubscriptionObjectType");
		switch (objectType) {
		case FORUM:
			// unconditionally join with FORUM to filter out deleted forums.
			builder.append(" JOIN ").append(TABLE_FORUM).append(" F");
			builder.append(" ON (S.").append(COL_SUBSCRIPTION_OBJECT_ID).append(" = F.").append(COL_FORUM_ID);
			if (projectIds != null) {
				builder.append(" AND F.").append(COL_FORUM_PROJECT_ID).append(" IN (:").append(PROJECT_IDS).append(")");
			}
			builder.append(")");
			break;
		case THREAD:
			builder.append(" JOIN ").append(TABLE_DISCUSSION_THREAD).append(" T");
			builder.append(" ON (S.").append(COL_SUBSCRIPTION_OBJECT_ID).append(" = T.")
					.append(COL_DISCUSSION_THREAD_ID);
			builder.append(" AND T.").append(COL_DISCUSSION_THREAD_IS_DELETED).append(" = FALSE)");
			if(projectIds != null) {
				builder.append(" JOIN ").append(TABLE_FORUM).append(" F");
				builder.append(" ON (T.").append(COL_DISCUSSION_THREAD_FORUM_ID).append(" = F.").append(COL_FORUM_ID);
				builder.append(" AND F.").append(COL_FORUM_PROJECT_ID).append(" IN (:").append(PROJECT_IDS).append("))");
			}
			break;
		default:
			// nothing to add for the default case.
		}
	}
	
	/**
	 * Get the column name for a sort type.
	 * @param sortByType
	 * @return
	 */
	static String getColunNameForSortType(SortByType sortByType) {
		ValidateArgument.required(sortByType, "SortByType");
		switch (sortByType) {
		case SUBSCRIPTION_ID:
			return COL_SUBSCRIPTION_ID;
		case SUBSCRIBER_ID:
			return COL_SUBSCRIPTION_SUBSCRIBER_ID;
		case CREATED_ON:
			return COL_SUBSCRIPTION_CREATED_ON;
		case OBJECT_ID:
			return COL_SUBSCRIPTION_OBJECT_ID;
		case OBJECT_TYPE:
			return COL_SUBSCRIPTION_OBJECT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown sort type: " + sortByType);
		}
	}
	
	/**
	 * Convert the sort direction to SQL.
	 * @param direction
	 * @return
	 */
	static String getSortDirection(SortDirection direction) {
		ValidateArgument.required(direction, "SortDirection");
		switch (direction) {
		case ASC:
			return "ASC";
		case DESC:
			return "DESC";
		default:
			throw new IllegalArgumentException("Unknown sort direction: " + direction);
		}
	}
}
