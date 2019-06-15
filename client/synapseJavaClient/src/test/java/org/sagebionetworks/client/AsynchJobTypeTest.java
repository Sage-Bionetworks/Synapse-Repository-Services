package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;

public class AsynchJobTypeTest {
	private final String tableId = "0123456";
	private final String token = "abcdefgh";

	@Test
	public void testGetStartUrlwAppendableRowSetRequest() {
		AppendableRowSetRequest request = new AppendableRowSetRequest();
		request.setEntityId(tableId);
		AsynchJobType type = AsynchJobType.TableAppendRowSet;
		String actual = type.getStartUrl(request);
		String expected = "/entity/0123456/table/append/async/start";
		assertEquals(actual, expected);
	}

	@Test
	public void testGetStartUrlwUploadToTableRequest() {
		UploadToTableRequest request = new UploadToTableRequest();
		request.setTableId(tableId);
		AsynchJobType type = AsynchJobType.TableCSVUpload;
		String actual = type.getStartUrl(request);
		String expected = "/entity/0123456/table/upload/csv/async/start";
		assertEquals(actual, expected);
	}

	@Test
	public void testGetStartUrlwQueryBundleRequest() {
		QueryBundleRequest request = new QueryBundleRequest();
		request.setEntityId(tableId);
		AsynchJobType type = AsynchJobType.TableQuery;
		String actual = type.getStartUrl(request);
		String expected = "/entity/0123456/table/query/async/start";
		assertEquals(actual, expected);
	}

	@Test
	public void testGetStartUrlwQueryNextPageToken() {
		QueryNextPageToken request = new QueryNextPageToken();
		request.setEntityId(tableId);
		AsynchJobType type = AsynchJobType.TableQueryNextPage;
		String actual = type.getStartUrl(request);
		String expected = "/entity/0123456/table/query/nextPage/async/start";
		assertEquals(actual, expected);
	}

	@Test
	public void testGetStartUrlwDownloadFromTableRequest() {
		DownloadFromTableRequest request = new DownloadFromTableRequest();
		request.setEntityId(tableId);
		AsynchJobType type = AsynchJobType.TableCSVDownload;
		String actual = type.getStartUrl(request);
		String expected = "/entity/0123456/table/download/csv/async/start";
		assertEquals(actual, expected);
	}

	@Test
	public void testGetStartUrlwUploadToTablePreviewRequest() {
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		AsynchJobType type = AsynchJobType.TableCSVUploadPreview;
		String actual = type.getStartUrl(request);
		String expected = "/table/upload/csv/preview/async/start";
		assertEquals(actual, expected);
	}

	@Test
	public void testGetResultUrlwAppendableRowSetRequest() {
		AppendableRowSetRequest request = new AppendableRowSetRequest();
		request.setEntityId(tableId);
		AsynchJobType type = AsynchJobType.TableAppendRowSet;
		String actual = type.getResultUrl(token, request);
		String expected = "/entity/0123456/table/append/async/get/abcdefgh";
		assertEquals(actual, expected);
	}

	@Test
	public void testGetResultUrlwUploadToTableRequest() {
		UploadToTableRequest request = new UploadToTableRequest();
		request.setTableId(tableId);
		AsynchJobType type = AsynchJobType.TableCSVUpload;
		String actual = type.getResultUrl(token, request);
		String expected = "/entity/0123456/table/upload/csv/async/get/abcdefgh";
		assertEquals(actual, expected);
	}

	@Test
	public void testGetResultUrlwQueryBundleRequest() {
		QueryBundleRequest request = new QueryBundleRequest();
		request.setEntityId(tableId);
		AsynchJobType type = AsynchJobType.TableQuery;
		String actual = type.getResultUrl(token, request);
		String expected = "/entity/0123456/table/query/async/get/abcdefgh";
		assertEquals(actual, expected);
	}

	@Test
	public void testGetResultUrlwQueryNextPageToken() {
		QueryNextPageToken request = new QueryNextPageToken();
		request.setEntityId(tableId);
		AsynchJobType type = AsynchJobType.TableQueryNextPage;
		String actual = type.getResultUrl(token, request);
		String expected = "/entity/0123456/table/query/nextPage/async/get/abcdefgh";
		assertEquals(actual, expected);
	}

	@Test
	public void testGetResultUrlwDownloadFromTableRequest() {
		DownloadFromTableRequest request = new DownloadFromTableRequest();
		request.setEntityId(tableId);
		AsynchJobType type = AsynchJobType.TableCSVDownload;
		String actual = type.getResultUrl(token, request);
		String expected = "/entity/0123456/table/download/csv/async/get/abcdefgh";
		assertEquals(actual, expected);
	}

	@Test
	public void testGetResultUrlwUploadToTablePreviewRequest() {
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		AsynchJobType type = AsynchJobType.TableCSVUploadPreview;
		String actual = type.getResultUrl(token, request);
		String expected = "/table/upload/csv/preview/async/get/abcdefgh";
		assertEquals(actual, expected);
	}

	@Test
	public void testGetResultUrlwEntityId() {
		AsynchJobType type = AsynchJobType.TableAppendRowSet;
		String actual = type.getResultUrl(token, tableId);
		String expected = "/entity/0123456/table/append/async/get/abcdefgh";
		assertEquals(actual, expected);
	}

	@Test
	public void testGetResultUrlwNullEntityId() {
		AsynchJobType type = AsynchJobType.TableCSVUploadPreview;
		String actual = type.getResultUrl(token, (String) null);
		String expected = "/table/upload/csv/preview/async/get/abcdefgh";
		assertEquals(actual, expected);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetStartUrlwNullEntityId() {
		AppendableRowSetRequest request = new AppendableRowSetRequest();
		AsynchJobType type = AsynchJobType.TableAppendRowSet;
		type.getStartUrl(request);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetResultUrlwNullEntityId2() {
		AppendableRowSetRequest request = new AppendableRowSetRequest();
		AsynchJobType type = AsynchJobType.TableAppendRowSet;
		type.getResultUrl(token, request);
	}
	
	@Test
	public void testTableTransaction(){
		AsynchJobType type = AsynchJobType.TableTransaction;
		String actual = type.getResultUrl(token, (String) null);
		String expected = "/table/transaction/async/get/abcdefgh";
		assertEquals(actual, expected);
	}

	@Test
	public void testDoi(){
		AsynchJobType type = AsynchJobType.Doi;
		String actual = type.getResultUrl(token, (String) null);
		String expected = "/doi/async/get/abcdefgh";
		assertEquals(actual, expected);
	}

	@Test
	public void testStorageReport(){
		AsynchJobType type = AsynchJobType.DownloadStorageReport;
		String actual = type.getResultUrl(token, (String) null);
		String expected = "/storageReport/async/get/abcdefgh";
		assertEquals(actual, expected);
	}
}
