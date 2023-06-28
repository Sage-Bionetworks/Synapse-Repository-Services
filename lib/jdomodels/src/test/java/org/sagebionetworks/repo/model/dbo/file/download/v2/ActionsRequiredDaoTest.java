package org.sagebionetworks.repo.model.dbo.file.download.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.download.ActionRequiredCount;
import org.sagebionetworks.repo.model.download.EnableTwoFa;
import org.sagebionetworks.repo.model.download.MeetAccessRequirement;
import org.sagebionetworks.repo.model.download.RequestDownload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class ActionsRequiredDaoTest {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	private ActionsRequiredDao dao;
	
	private FilesBatchProvider filesProvider;
	private EntityActionRequiredCallback actionsFilter;
	
	private long userId = 123;
	private long batchSize = 10;
	
	@BeforeEach
	public void before() {
		dao = new ActionsRequiredDao(jdbcTemplate);		
	}
	
	@AfterEach
	public void after() {
		dao.dropActionsRequiredTable(userId);
	}
	
	@Test
	public void testCreateActionsRequiredTable() {
		
		List<Long> files = List.of(1L, 2L, 3L); 
		
		filesProvider = getFilesProvider(files);
		
		actionsFilter = (fileIds) -> {
			return List.of(
				new FileActionRequired().withFileId(1L).withAction(new RequestDownload().setBenefactorId(123L)),
				new FileActionRequired().withFileId(2L).withAction(new RequestDownload().setBenefactorId(123L)),
				new FileActionRequired().withFileId(3L).withAction(new MeetAccessRequirement().setAccessRequirementId(789L))
			);
		};
		
		// Call under test
		String tableName = dao.createActionsRequiredTable(userId, batchSize, filesProvider, actionsFilter);
		
		assertEquals("U123A", tableName);
		
		List<ActionRequiredCount> expectedActions = List.of(
			new ActionRequiredCount().setCount(2L).setAction(new RequestDownload().setBenefactorId(123L)),
			new ActionRequiredCount().setCount(1L).setAction(new MeetAccessRequirement().setAccessRequirementId(789L))
		);
		List<ActionRequiredCount> result = dao.getActionsRequiredCount(userId, batchSize);
		
		assertEquals(expectedActions, result);
		
	}
	
	@Test
	public void testCreateActionsRequiredTableWithMultipleBatches() {
		
		List<Long> files = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L); 
		
		filesProvider = getFilesProvider(files);
		
		actionsFilter = (fileIds) -> {
			List<FileActionRequired> actionsRequired = new ArrayList<>();
			
			for (Long id : fileIds) {
				if (id % 2 == 0) {
					// even ids gets a download request
					actionsRequired.add(new FileActionRequired().withFileId(id).withAction(new RequestDownload().setBenefactorId(123L)));
				} else if (id > 5) {
					// The last two odd get 2FA
					actionsRequired.add(new FileActionRequired().withFileId(id).withAction(new EnableTwoFa().setAccessRequirementId(456L)));
				}
			}
			return actionsRequired;
		};
		
		// Call under test
		String tableName = dao.createActionsRequiredTable(userId, 3, filesProvider, actionsFilter);
		
		assertEquals("U123A", tableName);
		
		List<ActionRequiredCount> expectedActions = List.of(
			new ActionRequiredCount().setCount(5L).setAction(new RequestDownload().setBenefactorId(123L)),
			new ActionRequiredCount().setCount(2L).setAction(new EnableTwoFa().setAccessRequirementId(456L))
		);
		
		List<ActionRequiredCount> result = dao.getActionsRequiredCount(userId, batchSize);
		
		assertEquals(expectedActions, result);
		
	}
	
	@Test
	public void testCreateActionsRequiredTableWithNoFiles() {
		
		List<Long> files = Collections.emptyList();
		
		filesProvider = getFilesProvider(files);
		
		actionsFilter = (fileIds) -> {
			return List.of(
				new FileActionRequired().withFileId(1L).withAction(new RequestDownload().setBenefactorId(123L)),
				new FileActionRequired().withFileId(2L).withAction(new RequestDownload().setBenefactorId(123L)),
				new FileActionRequired().withFileId(3L).withAction(new MeetAccessRequirement().setAccessRequirementId(789L))
			);
		};
		
		// Call under test
		String tableName = dao.createActionsRequiredTable(userId, batchSize, filesProvider, actionsFilter);
		
		assertEquals("U123A", tableName);
		
		List<ActionRequiredCount> expectedActions = Collections.emptyList();
		List<ActionRequiredCount> result = dao.getActionsRequiredCount(userId, batchSize);
		
		assertEquals(expectedActions, result);
		
	}
	
	@Test
	public void testCreateActionsRequiredTableWithNoActions() {
		List<Long> files = List.of(1L, 2L, 3L); 
		
		filesProvider = getFilesProvider(files);
		
		actionsFilter = (fileIds) -> {
			return Collections.emptyList();
		};
		
		// Call under test
		String tableName = dao.createActionsRequiredTable(userId, batchSize, filesProvider, actionsFilter);
		
		assertEquals("U123A", tableName);
		
		List<ActionRequiredCount> expectedActions = Collections.emptyList();
		List<ActionRequiredCount> result = dao.getActionsRequiredCount(userId, batchSize);
		
		assertEquals(expectedActions, result);
		
	}
	
	private FilesBatchProvider getFilesProvider(List<Long> files) {
		return (limit, offset) -> files.subList((int)offset, Math.min(files.size(), (int)(offset + limit)));
	}

}
