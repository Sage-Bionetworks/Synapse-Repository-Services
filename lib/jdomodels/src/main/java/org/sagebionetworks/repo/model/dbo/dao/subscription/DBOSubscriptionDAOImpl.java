package org.sagebionetworks.repo.model.dbo.dao.subscription;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.subscription.DBOSubscription;
import org.sagebionetworks.repo.model.dbo.persistence.subscription.SubscriptionUtils;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class DBOSubscriptionDAOImpl implements SubscriptionDAO{

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private IdGenerator idGenerator;

	private static final String SQL_GET = "SELECT * "
			+ "FROM "+TABLE_SUBSCRIPTION+" "
			+ "WHERE "+COL_SUBSCRIPTION_ID+" = ?";

	private static final String SQL_GET_ALL = "SELECT * "
			+ "FROM "+TABLE_SUBSCRIPTION+" "
			+ "WHERE "+COL_SUBSCRIPTION_SUBSCRIBER_ID+" = ?";

	private static final String SQL_COUNT = "SELECT COUNT(*) "
			+ "FROM "+TABLE_SUBSCRIPTION+" "
			+ "WHERE "+COL_SUBSCRIPTION_SUBSCRIBER_ID+" = ?";

	public static final String OBJECT_TYPE_CONDITION = " AND "+COL_SUBSCRIPTION_OBJECT_TYPE+" = ";
	private static final String TOPIC_CONDITION = " AND ("+COL_SUBSCRIPTION_OBJECT_ID
			+", "+COL_SUBSCRIPTION_OBJECT_TYPE+") IN ";

	private static final String LIMIT = " limit ";
	private static final String OFFSET = " offset ";

	private static final String SQL_DELETE = "DELETE FROM "+TABLE_SUBSCRIPTION+" "
			+ "WHERE "+COL_SUBSCRIPTION_ID+" = ?";

	public static final char QUOTE = '"';
	private static final char LEFT_PAREN = '(';
	private static final char RIGHT_PAREN = ')';
	private static final String TOPIC_FORMAT = "(%1$s, \"%2$s\")";

	private static final RowMapper<Subscription> ROW_MAPPER = new RowMapper<Subscription>(){

		@Override
		public Subscription mapRow(ResultSet rs, int rowNum) throws SQLException {
			Subscription subscription = new Subscription();
			subscription.setSubscriptionId(""+rs.getLong(COL_SUBSCRIPTION_ID));
			subscription.setSubscriberId(""+rs.getLong(COL_SUBSCRIPTION_SUBSCRIBER_ID));
			subscription.setObjectId(""+rs.getLong(COL_SUBSCRIPTION_OBJECT_ID));
			subscription.setObjectType(SubscriptionObjectType.valueOf(rs.getString(COL_SUBSCRIPTION_OBJECT_TYPE)));
			subscription.setCreatedOn(new Date(rs.getLong(COL_SUBSCRIPTION_SUBSCRIBER_ID)));
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
		long subscriptionId = idGenerator.generateNewId(TYPE.SUBSCRIPTION_ID);
		DBOSubscription dbo = SubscriptionUtils.createDBO(subscriptionId, subscriberId, objectId, objectType, new Date());
		basicDao.createNew(dbo);
		return get(subscriptionId);
	}

	@Override
	public Subscription get(long subscriptionId) {
		List<Subscription> results = jdbcTemplate.query(SQL_GET, ROW_MAPPER, subscriptionId);
		if (results.size() != 1) {
			throw new NotFoundException();
		}
		return results.get(0);
	}

	@Override
	public SubscriptionPagedResults getAll(String subscriberId, Long limit,
			Long offset, SubscriptionObjectType objectType) {
		ValidateArgument.required(subscriberId, "subscriberId");
		ValidateArgument.required(limit, "limit");
		ValidateArgument.required(offset, "offset");
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		long count = getSubscriptionCount(subscriberId, objectType);
		results.setTotalNumberOfResults(count);
		if (count > 0) {
			String query = buildGetQuery(limit, offset, objectType);
			results.setResults(jdbcTemplate.query(query, ROW_MAPPER, subscriberId));
		} else {
			results.setResults(new ArrayList<Subscription>(0));
		}
		return results;
	}

	public static String buildGetQuery(Long limit, Long offset, SubscriptionObjectType objectType) {
		String query = SQL_GET_ALL;
		query = addCondition(query, objectType);
		query += LIMIT + limit + OFFSET + offset;
		return query;
	}

	public static String addCondition(String query, SubscriptionObjectType objectType) {
		if (objectType != null) {
			query += OBJECT_TYPE_CONDITION + QUOTE + objectType.name() + QUOTE;
		}
		return query;
	}

	@Override
	public long getSubscriptionCount(String subscriberId,
			SubscriptionObjectType objectType) {
		String query = addCondition(SQL_COUNT, objectType);
		return jdbcTemplate.queryForLong(query, subscriberId);
	}

	@Override
	public SubscriptionPagedResults getSubscriptionList(String subscriberId,
			List<Topic> listOfTopic) {
		ValidateArgument.required(subscriberId, "subscriberId");
		ValidateArgument.required(listOfTopic, "listOfTopic");
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		if (listOfTopic.isEmpty()) {
			results.setResults(new ArrayList<Subscription>(0));
			results.setTotalNumberOfResults(0L);
		} else {
			String query = SQL_GET_ALL + buildTopicCondition(listOfTopic);
			List<Subscription> subscriptions = jdbcTemplate.query(query, ROW_MAPPER, subscriberId);
			results.setResults(subscriptions);
			results.setTotalNumberOfResults((long) subscriptions.size());
		}
		return results;
	}

	public static String buildTopicCondition(List<Topic> listOfTopic) {
		String condition = TOPIC_CONDITION + LEFT_PAREN;
		condition += String.format(TOPIC_FORMAT, listOfTopic.get(0).getObjectId(), listOfTopic.get(0).getObjectType());
		for (int i = 1; i < listOfTopic.size(); i++) {
			condition += ", "+String.format(TOPIC_FORMAT, listOfTopic.get(i).getObjectId(), listOfTopic.get(i).getObjectType());
		}
		return condition + RIGHT_PAREN;
	}

	@WriteTransactionReadCommitted
	@Override
	public void delete(long subscriptionId) {
		jdbcTemplate.update(SQL_DELETE, subscriptionId);
	}

}
