package org.sagebionetworks.bridge.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.bridge.model.ParticipantDataDescriptorDAO;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnType;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataRepeatType;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOParticipantDataDescriptor;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOParticipantDataDescriptorDAOImplTest {

	@SuppressWarnings("rawtypes")
	public class Deletable {
		Class clazz;
		Object id;

		public Deletable(Class clazz, Object id) {
			this.clazz = clazz;
			this.id = id;
		}
	}

	@Autowired
	private ParticipantDataDescriptorDAO participantDataDescriptorDAO;

	@Autowired
	private DBOBasicDao dboBasicDao;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private AmazonS3Client s3Client;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	StackConfiguration stackConfiguration;

	List<Deletable> toDelete = Lists.newArrayList();

	@SuppressWarnings({ "unchecked" })
	@After
	public void after() throws Exception {
		if (dboBasicDao != null) {
			for (Deletable item : Lists.reverse(toDelete)) {
				try {
					MapSqlParameterSource params = new MapSqlParameterSource("id", item.id);
					dboBasicDao.getObjectByPrimaryKey(item.clazz, params);
					dboBasicDao.deleteObjectByPrimaryKey(item.clazz, params);
				} catch (NotFoundException e) {
					// ignore, expected
				} catch (InvalidDataAccessApiUsageException e) {
					// ignore, expected
				}
			}
		}
	}

	private ParticipantDataDescriptor createParticipantDataDescriptor() {
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setDescription("some description");
		participantDataDescriptor.setName("some name " + idGenerator.generateNewId());
		participantDataDescriptor.setRepeatType(ParticipantDataRepeatType.ALWAYS);
		participantDataDescriptor = participantDataDescriptorDAO.createParticipantDataDescriptor(participantDataDescriptor);

		toDelete.add(new Deletable(DBOParticipantDataDescriptor.class, participantDataDescriptor.getId()));
		return participantDataDescriptor;
	}
	
	private ParticipantDataColumnDescriptor createColumnDescriptor(ParticipantDataDescriptor participantDataDescriptor, String columnName) {
		ParticipantDataColumnDescriptor participantDataColumnDescriptor1 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor1.setName(columnName);
		participantDataColumnDescriptor1.setColumnType(ParticipantDataColumnType.STRING);
		participantDataColumnDescriptor1.setDescription("something");
		participantDataColumnDescriptor1.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor1 = participantDataDescriptorDAO.createParticipantDataColumnDescriptor(participantDataColumnDescriptor1);

		toDelete.add(new Deletable(DBOParticipantDataColumnDescriptor.class, participantDataColumnDescriptor1.getId()));
		return participantDataColumnDescriptor1;
	}

	@Test
	public void testRoundTrip() throws Exception {

		ParticipantDataDescriptor participantDataDescriptor = createParticipantDataDescriptor();

		ParticipantDataColumnDescriptor participantDataColumnDescriptor1 = createColumnDescriptor(participantDataDescriptor, "data1");
		ParticipantDataColumnDescriptor participantDataColumnDescriptor2 = createColumnDescriptor(participantDataDescriptor, "data2");

		List<ParticipantDataDescriptor> participantDataDescriptors = participantDataDescriptorDAO.getParticipantDatas();
		ParticipantDataDescriptor found = null;
		for (ParticipantDataDescriptor dd : participantDataDescriptors) {
			if (dd.getId().equals(participantDataDescriptor.getId())) {
				found = dd;
				break;
			}
		}
		assertNotNull(found);
		assertEquals(participantDataDescriptor, found);

		List<ParticipantDataColumnDescriptor> participantDataColumns = participantDataDescriptorDAO.getParticipantDataColumns(participantDataDescriptor.getId());
		assertEquals(2, participantDataColumns.size());
		if (participantDataColumns.get(0).getId().equals(participantDataColumnDescriptor1.getId())) {
			assertEquals(participantDataColumnDescriptor1, participantDataColumns.get(0));
			assertEquals(participantDataColumnDescriptor2, participantDataColumns.get(1));
		} else {
			assertEquals(participantDataColumnDescriptor1, participantDataColumns.get(1));
			assertEquals(participantDataColumnDescriptor2, participantDataColumns.get(0));
		}
	}
	
	@Test
	public void testUpdateParticipantDataDescriptor() throws Exception {
		ParticipantDataDescriptor participantDataDescriptor = createParticipantDataDescriptor();
		
		participantDataDescriptor.setName("Altered name");
		participantDataDescriptor.setDescription(null);
		participantDataDescriptor.setRepeatFrequency("* 17 * * 1-5");
		participantDataDescriptor.setRepeatType(ParticipantDataRepeatType.IF_NEW);
		participantDataDescriptorDAO.updateParticipantDataDescriptor(participantDataDescriptor);

		participantDataDescriptor = participantDataDescriptorDAO.getParticipantDataDescriptor(participantDataDescriptor.getId());
		assertEquals("Altered name", participantDataDescriptor.getName());
		assertNull(participantDataDescriptor.getDescription());
		assertEquals("* 17 * * 1-5", participantDataDescriptor.getRepeatFrequency());
		assertEquals(ParticipantDataRepeatType.IF_NEW, participantDataDescriptor.getRepeatType());
	}

	@Test
	public void testModifyColumn() throws Exception {

		ParticipantDataDescriptor participantDataDescriptor = createParticipantDataDescriptor();

		ParticipantDataColumnDescriptor participantDataColumnDescriptor = createColumnDescriptor(participantDataDescriptor, "data1");

		participantDataColumnDescriptor.setDescription("Some other description");
		participantDataDescriptorDAO.updateParticipantDataColumnDescriptor(participantDataColumnDescriptor);

		List<ParticipantDataColumnDescriptor> participantDataColumns = participantDataDescriptorDAO.getParticipantDataColumns(participantDataDescriptor.getId());
		assertEquals(1, participantDataColumns.size());
		assertEquals(participantDataColumnDescriptor, participantDataColumns.get(0));
	}

	@Test
	public void testDeleteColumn() throws Exception {

		ParticipantDataDescriptor participantDataDescriptor = createParticipantDataDescriptor();

		ParticipantDataColumnDescriptor participantDataColumnDescriptor = createColumnDescriptor(participantDataDescriptor, "data1");
		ParticipantDataColumnDescriptor participantDataColumnDescriptor2 = createColumnDescriptor(participantDataDescriptor, "data2");

		List<ParticipantDataColumnDescriptor> participantDataColumns = participantDataDescriptorDAO.getParticipantDataColumns(participantDataDescriptor.getId());
		assertEquals(2, participantDataColumns.size());

		participantDataDescriptorDAO.deleteParticipantDataColumnDescriptor(participantDataDescriptor.getId(), participantDataColumnDescriptor2.getId());

		participantDataColumns = participantDataDescriptorDAO.getParticipantDataColumns(participantDataDescriptor.getId());
		assertEquals(1, participantDataColumns.size());
		assertEquals(participantDataColumnDescriptor, participantDataColumns.get(0));
	}
	
	@Test(expected=DuplicateKeyException.class)
	public void testRejectDuplicateColumnName() throws Throwable {
		ParticipantDataDescriptor participantDataDescriptor = createParticipantDataDescriptor();
		try {
			createColumnDescriptor(participantDataDescriptor, "data1");
			createColumnDescriptor(participantDataDescriptor, "data1");
		} catch(IllegalArgumentException iae) {
			throw iae.getCause();
		}
	}
	
	@Test
	public void testDuplicateColumnNameUnderDifferentDescriptorsNotRejected() {
		ParticipantDataDescriptor participantDataDescriptor = createParticipantDataDescriptor();
		createColumnDescriptor(participantDataDescriptor, "data1");
		
		participantDataDescriptor = createParticipantDataDescriptor();
		createColumnDescriptor(participantDataDescriptor, "data1");
	}	
	
}
