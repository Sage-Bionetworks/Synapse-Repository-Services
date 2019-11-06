package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;

@ExtendWith(MockitoExtension.class)
public class TableTransactionUtilsTest {

	String tableId;
	TableUpdateTransactionRequest request;
	UploadToTableRequest uploadRequest;

	TableUpdateTransactionResponse response;

	@BeforeEach
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);

		tableId = "syn213";
		request = new TableUpdateTransactionRequest();
		request.setEntityId(tableId);
		List<TableUpdateRequest> changes = new LinkedList<>();
		request.setChanges(changes);

		uploadRequest = new UploadToTableRequest();
		uploadRequest.setEntityId(tableId);
		changes.add(uploadRequest);
	}

	@Test
	public void testValidateRequestValid() {
		// call under test.
		TableTransactionUtils.validateRequest(request);
	}

	@Test
	public void testValidateRequestMissingId() {
		uploadRequest.setEntityId(null);
		// call under test.
		TableTransactionUtils.validateRequest(request);
		// the root table ID is passed to each change if null
		assertEquals(tableId, uploadRequest.getEntityId());
	}

	@Test
	public void testValidateRequestChangeIdDoesNotMatch() {
		uploadRequest.setEntityId("doesNotMatch");
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			TableTransactionUtils.validateRequest(request);
		});
	}

	@Test
	public void testValidateRequestNullRequest() {
		request = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			TableTransactionUtils.validateRequest(request);
		});
	}

	@Test
	public void testValidateRequestEntityIdNull() {
		request.setEntityId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			TableTransactionUtils.validateRequest(request);
		});
	}
	
	@Test
	public void testValidateRequestNullChagnesNulSnapshot() {
		request.setChanges(null);
		request.setCreateSnapshot(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			TableTransactionUtils.validateRequest(request);
		}).getMessage();
		assertEquals("Must include be at least one change or create a snapshot.", message);
	}

	@Test
	public void testValidateRequestChagnesEmptySnapshotFalse() {
		request.setChanges(new LinkedList<TableUpdateRequest>());
		request.setCreateSnapshot(false);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			TableTransactionUtils.validateRequest(request);
		}).getMessage();
		assertEquals("Must include be at least one change or create a snapshot.", message);
	}
	
	@Test
	public void testValidateRequestChagnesEmptySnapshotNull() {
		request.setChanges(new LinkedList<TableUpdateRequest>());
		request.setCreateSnapshot(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			TableTransactionUtils.validateRequest(request);
		}).getMessage();
		assertEquals("Must include be at least one change or create a snapshot.", message);
	}
	
	/**
	 * Test added for PLFM-5912.
	 */
	@Test
	public void testValidateRequestChagnesEmptyWithSnapshot() {
		request.setChanges(new LinkedList<TableUpdateRequest>());
		request.setCreateSnapshot(true);
		// call under test.
		TableTransactionUtils.validateRequest(request);
	}
	
	/**
	 * Test added for PLFM-5912.
	 */
	@Test
	public void testValidateRequestNullChagnesEmptyWithSnapshot() {
		request.setChanges(null);
		request.setCreateSnapshot(true);
		// call under test.
		TableTransactionUtils.validateRequest(request);
	}
}
