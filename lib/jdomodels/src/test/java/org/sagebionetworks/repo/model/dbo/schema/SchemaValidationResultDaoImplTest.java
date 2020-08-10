package org.sagebionetworks.repo.model.dbo.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.repo.model.schema.ValidationException;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SchemaValidationResultDaoImplTest {

	@Autowired
	SchemaValidationResultDao dao;

	ValidationResults results;

	@BeforeEach
	public void before() {
		dao.clearAll();

		results = new ValidationResults();
		results.setObjectId("syn123");
		results.setObjectType(ObjectType.entity);
		results.setObjectEtag("etag");
		results.setIsValid(true);
		results.setSchema$id("my.org-foo.bar-1.0.1");
		results.setValidatedOn(new Date());
		results.setValidationErrorMessage("the main message");
		results.setAllValidationMessages(Lists.newArrayList("message one", "message two"));

		ValidationException cause = new ValidationException();
		cause.setKeyword("causeKeyWord");
		cause.setMessage("the main message");
		cause.setPointerToViolation("cause pointer");
		cause.setSchemaLocation("cause location");

		ValidationException exception = new ValidationException();
		exception.setKeyword("keyWord");
		exception.setMessage("the main message");
		exception.setPointerToViolation("pointer");
		exception.setSchemaLocation("location");
		exception.setCausingExceptions(Lists.newArrayList(cause));
		results.setValidationException(exception);
	}

	@AfterEach
	public void after() {
		dao.clearAll();
	}

	@Test
	public void testCreateOrUpdateResults() {
		// call under test
		dao.createOrUpdateResults(results);
		ValidationResults fetched = dao.getValidationResults(results.getObjectId(), results.getObjectType());
		assertEquals(results, fetched);
	}

	@Test
	public void testCreateOrUpdateResultsWithUpdate() {
		dao.createOrUpdateResults(results);
		ValidationResults fetched = dao.getValidationResults(results.getObjectId(), results.getObjectType());
		assertEquals(results, fetched);
		// change the etag
		results.setObjectEtag("a new etag");
		results.setIsValid(false);
		// call under test
		dao.createOrUpdateResults(results);
		fetched = dao.getValidationResults(results.getObjectId(), results.getObjectType());
		assertEquals(results, fetched);
	}

	@Test
	public void testCreateOrUpdateResultsWithAllNulls() {
		results.setValidationErrorMessage(null);
		results.setAllValidationMessages(null);
		results.setValidationException(null);
		// call under test
		dao.createOrUpdateResults(results);
		ValidationResults fetched = dao.getValidationResults(results.getObjectId(), results.getObjectType());
		assertEquals(results, fetched);
	}

	@Test
	public void testCreateOrUpdateResultsWithNullResult() {
		results = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.createOrUpdateResults(results);
		});
	}

	@Test
	public void testCreateOrUpdateResultsWithNullObjectId() {
		results.setObjectId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.createOrUpdateResults(results);
		});
	}

	@Test
	public void testCreateOrUpdateResultsWithNullObjectType() {
		results.setObjectType(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.createOrUpdateResults(results);
		});
	}

	@Test
	public void testCreateOrUpdateResultsWithNullObjectEtag() {
		results.setObjectEtag(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.createOrUpdateResults(results);
		});
	}

	@Test
	public void testCreateOrUpdateResultsWithNullIsValid() {
		results.setIsValid(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.createOrUpdateResults(results);
		});
	}

	@Test
	public void testCreateOrUpdateResultsWithNullValidatedOn() {
		results.setValidatedOn(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.createOrUpdateResults(results);
		});
	}

	@Test
	public void testCreateOrUpdateResultsWithNullSchemaId() {
		results.setSchema$id(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.createOrUpdateResults(results);
		});
	}

	@Test
	public void testGetValidationResults() {
		dao.createOrUpdateResults(results);
		// call under test
		ValidationResults fetched = dao.getValidationResults(results.getObjectId(), results.getObjectType());
		assertEquals(results, fetched);
	}

	@Test
	public void testGetValidationResultsNotFound() {
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			dao.getValidationResults(results.getObjectId(), results.getObjectType());
		}).getMessage();
		assertEquals("ValidationResults do not exist for objectId: 'syn123' and objectType: 'entity'", message);
	}

	@Test
	public void testGetValidationResultsNullObjectId() {
		results.setObjectId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.getValidationResults(results.getObjectId(), results.getObjectType());
		});
	}

	@Test
	public void testGetValidationResultsNullObjectType() {
		results.setObjectType(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.getValidationResults(results.getObjectId(), results.getObjectType());
		});
	}

	@Test
	public void testClearResults() {
		dao.createOrUpdateResults(results);
		ValidationResults fetched = dao.getValidationResults(results.getObjectId(), results.getObjectType());
		assertEquals(results, fetched);
		// call under test
		dao.clearResults(results.getObjectId(), results.getObjectType());
		assertThrows(NotFoundException.class, () -> {
			dao.getValidationResults(results.getObjectId(), results.getObjectType());
		});
	}

	@Test
	public void testClearResultsWithNullObjectId() {
		results.setObjectId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.clearResults(results.getObjectId(), results.getObjectType());
		});
	}

	@Test
	public void testClearResultsWithNullObjectType() {
		results.setObjectType(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			dao.clearResults(results.getObjectId(), results.getObjectType());
		});
	}
}
