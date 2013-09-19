package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOChange;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessageUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.tools.javac.util.Pair;

/**
 * Note: this "test" is used to gather timing profiles.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOChangeDAOImplExperiment {
	
	@Autowired
	private DBOChangeDAO changeDAO;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	private static final String SELECT_CHANGE_NUM_IN_RANGE_FROM_CHANGES = 
			"SELECT "+SqlConstants.COL_CHANGES_CHANGE_NUM+" FROM "+SqlConstants.TABLE_CHANGES+
			" WHERE "+SqlConstants.COL_CHANGES_CHANGE_NUM+" >= ?"+
			" AND "+SqlConstants.COL_CHANGES_CHANGE_NUM+" <= ?";
	
	private static final String SELECT_CHANGE_NUM_IN_RANGE_FROM_SENT = 
			"SELECT "+SqlConstants.COL_SENT_MESSAGES_CHANGE_NUM+" FROM "+SqlConstants.TABLE_SENT_MESSAGES+
			" WHERE "+SqlConstants.COL_SENT_MESSAGES_CHANGE_NUM+" >= ?"+
			" AND "+SqlConstants.COL_SENT_MESSAGES_CHANGE_NUM+" <= ?";
	
	private static final String SELECT_CHANGES_BY_CHANGE_NUM = 
			"SELECT * FROM "+SqlConstants.TABLE_SENT_MESSAGES+
			" WHERE "+SqlConstants.COL_CHANGES_CHANGE_NUM+" IN (?)";
	
	private TableMapping<DBOChange> rowMapper = new DBOChange().getTableMapping();
	
	private static final long UNSENT_MESSAGE_QUEUER_TEST_SEED = 5330L;
	private static final long APPROX_RANGE_SIZE = 10000L;
	private static Random random;

	@Before
	public void initialize() {
		random = new Random(UNSENT_MESSAGE_QUEUER_TEST_SEED);
	}
	
	@Ignore
	@Test
	public void testCleanup() {
		// Delete all the changes
		changeDAO.deleteAllChanges();
	}
	
	@Ignore
	@Test
	public void testSetupChanges() {
		final long start = System.currentTimeMillis();
		
		// Insert 1 million entries, 10000 at a time so that the transaction doesn't get too big
		for (int i = 0; i < 100; i++) {
			List<ChangeMessage> batch = createList(10000, ObjectType.ENTITY, 0, 2000000);
			changeDAO.replaceChange(batch);
			System.out.print(".");
		}
		System.out.println();
		System.out.println("Time taken to insert all messages:");
		System.out.println(System.currentTimeMillis() - start);
	}
	
	@Ignore
	@Test
	public void testSetupSent() {
		final long start = System.currentTimeMillis();
		
		// Blindly fetch all the changes
		List<ChangeMessage> messages = changeDAO.listUnsentMessages(1000000);
		
		final long doneFetch = System.currentTimeMillis();
		System.out.println("Time taken to fetch all messages:");
		System.out.println(doneFetch - start);
		
		// Insert about 90% of the items into the sent table
		Collections.shuffle(messages, random);
		for (int i = 0; i < messages.size() * 0.9; i++) {
			changeDAO.registerMessageSent(messages.get(i).getChangeNumber());
			if (i % 1000 == 0) {
				System.out.println(i);
			}
		}
		System.out.println("Time taken to insert 90% of sent messages:");
		System.out.println(System.currentTimeMillis() - doneFetch);
	}
	
	@Ignore
	@Test
	@Repeat(10)
	public void testTimeFetchAllAtOnce() {
		final long start = System.currentTimeMillis();
		
		// Blindly fetch all the changes
		changeDAO.listUnsentMessages(1000000);
		
		System.out.println("Time taken to fetch all unsent messages:");
		System.out.println(System.currentTimeMillis() - start);
	}

	@Ignore
	@Test
	@Repeat(10)
	public void testTimeFetchAllOverRange() {
		long totalTime = 0L;
		List<Pair<Long, Long>> ranges = getRanges();
		
		for (int i = 0; i < ranges.size(); i++) {
			final long start = System.currentTimeMillis();
			
			changeDAO.listUnsentMessages(ranges.get(i).fst, ranges.get(i).snd);
			
			final long fetchTime = System.currentTimeMillis() - start;
			totalTime += fetchTime;
			// System.out.println(fetchTime);
			
		}
		
		System.out.println("Time taken to fetch all unsent messages via ranges:");
		System.out.println(totalTime);
	}

	@Ignore
	@Test
	@Repeat(10)
	public void testTimeFetchAllOverRangeInMemory() {
		long totalTime = 0L;
		List<Pair<Long, Long>> ranges = getRanges();
		
		for (int i = 0; i < ranges.size(); i++) {
			final long start = System.currentTimeMillis();
			final Set<Long> changes = new HashSet<Long>((int) APPROX_RANGE_SIZE);
			
			// Fetch the changes
			simpleJdbcTemplate.query(SELECT_CHANGE_NUM_IN_RANGE_FROM_CHANGES, new RowMapper<Long>() {
				@Override
				public Long mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					Long value = rs.getLong(SqlConstants.COL_CHANGES_CHANGE_NUM);
					changes.add(value);
					return value;
				}
				
			}, ranges.get(i).fst, ranges.get(i).snd);
			
			// Remove the sent messages
			simpleJdbcTemplate.query(SELECT_CHANGE_NUM_IN_RANGE_FROM_SENT, new RowMapper<Long>() {
				@Override
				public Long mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					Long value = rs.getLong(SqlConstants.COL_SENT_MESSAGES_CHANGE_NUM);
					changes.remove(value);
					return value;
				}
				
			}, ranges.get(i).fst, ranges.get(i).snd);
			
			// Fetch the changes and convert them to DTOs
			List<DBOChange> dboList = simpleJdbcTemplate.query(SELECT_CHANGES_BY_CHANGE_NUM, rowMapper, changes);
			ChangeMessageUtils.createDTOList(dboList);
			
			final long fetchTime = System.currentTimeMillis() - start;
			totalTime += fetchTime;
			// System.out.println(fetchTime);
			
		}
		
		System.out.println("Time taken to fetch all unsent messages via ranges in memory:");
		System.out.println(totalTime);
	}

	private List<Pair<Long, Long>> getRanges() {
		final long min = changeDAO.getMinimumChangeNumber();
		final long max = changeDAO.getCurrentChangeNumber();
		final long count = changeDAO.getCount();
		final long rangeSize = APPROX_RANGE_SIZE;
		final long chunks = 1 + count / rangeSize;
		final long chunkSize = 1 + (max - min) / chunks;
		
		// Copied and modified from UnsentMessageQueuer
		List<Pair<Long, Long>> ranges = new ArrayList<Pair<Long, Long>>();
		for (int i = 0; i < chunks; i++) {
			ranges.add(new Pair<Long, Long>(min + i * chunkSize, min + (i + 1) * chunkSize));
		}
		if (min + chunks * chunkSize < max) {
			ranges.add(new Pair<Long, Long>(min + chunks * chunkSize, max));
		}
		return ranges;
	}

	private List<ChangeMessage> createList(int numChangesInBatch, ObjectType type, long lowerBound, long upperBound) {
		List<ChangeMessage> batch = new ArrayList<ChangeMessage>(numChangesInBatch);
		for(int i = 0; i < numChangesInBatch; i++){
			ChangeMessage change = new ChangeMessage();
			long changeNum = lowerBound + (random.nextLong() % (upperBound - lowerBound + 1));
			if(ObjectType.ENTITY == type){
				change.setObjectId("syn" + changeNum);
			}else{
				change.setObjectId("" + changeNum);
			}
			change.setObjectEtag("etag" + changeNum);
			change.setChangeType(ChangeType.UPDATE);
			change.setObjectType(type);
			batch.add(change);
		}
		return batch;
	}
}
