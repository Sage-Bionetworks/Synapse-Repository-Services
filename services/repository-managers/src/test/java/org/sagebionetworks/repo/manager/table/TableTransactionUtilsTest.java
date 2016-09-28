package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;

public class TableTransactionUtilsTest {

	String tableId;
	TableUpdateTransactionRequest request;
	UploadToTableRequest uploadRequest;

	TableUpdateTransactionResponse response;

	@SuppressWarnings("unchecked")
	@Before
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

	@Test(expected = IllegalArgumentException.class)
	public void testValidateRequestChangeIdDoesNotMatch() {
		uploadRequest.setEntityId("doesNotMatch");
		// call under test.
		TableTransactionUtils.validateRequest(request);
		// the root table ID is passed to each change if null
		assertEquals(tableId, uploadRequest.getEntityId());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateRequestNullRequest() {
		request = null;
		// call under test.
		TableTransactionUtils.validateRequest(request);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateRequestEntityIdNull() {
		request.setEntityId(null);
		// call under test.
		TableTransactionUtils.validateRequest(request);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateRequestChagnesNull() {
		request.setChanges(null);
		// call under test.
		TableTransactionUtils.validateRequest(request);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateRequestChagnesEmpty() {
		request.setChanges(new LinkedList<TableUpdateRequest>());
		// call under test.
		TableTransactionUtils.validateRequest(request);
	}
}
