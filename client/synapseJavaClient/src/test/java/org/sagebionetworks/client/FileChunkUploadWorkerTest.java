package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;

/**
 * Test for the FileChunkUploadWorker
 * @author John
 *
 */
public class FileChunkUploadWorkerTest {

	Synapse mockClient;
	ChunkedFileToken token;
	ChunkRequest request;
	File mockChunk;
	FileChunkUploadWorker worker;
	
	@Before
	public void before(){
		mockClient = Mockito.mock(Synapse.class);
		mockChunk = Mockito.mock(File.class);
		// token
		token = new ChunkedFileToken();
		token.setContentType("text/plain");
		token.setFileName("foo.bar");
		token.setKey("key");
		token.setUploadId("uploadId");
		// request
		request = new ChunkRequest();
		request.setChunkedFileToken(token);
		request.setChunkNumber(123l);
		// Worker
		worker = new FileChunkUploadWorker(mockClient, request, mockChunk);
	}
	
	@Test
	public void testHappyCase() throws Exception{
		ChunkResult expected = new ChunkResult();
		expected.setChunkNumber(request.getChunkNumber());
		expected.setEtag("etag");
		when(mockClient.addChunkToFile(request)).thenReturn(expected);
		URL url = new URL("http://google.com");
		when(mockClient.createChunkedPresignedUrl(request)).thenReturn(url);
		when(mockClient.putFileToURL(url, mockChunk, token.getContentType())).thenReturn("Result");
		ChunkResult result = worker.call();
		assertEquals(expected, result);
	}
	
	@Test
	public void testOneError() throws Exception{
		final ChunkResult expected = new ChunkResult();
		expected.setChunkNumber(request.getChunkNumber());
		expected.setEtag("etag");
		final int count = 0;
		when(mockClient.addChunkToFile(request)).thenAnswer(new Answer<ChunkResult>() {
			int count = 0;
			@Override
			public ChunkResult answer(InvocationOnMock invocation)	throws Throwable {
				count++;
				// Fail once
				if(count < 2) throw new RuntimeException("Some failure");
				// then pass
				return expected;
			}
		});
		URL url = new URL("http://google.com");
		when(mockClient.createChunkedPresignedUrl(request)).thenReturn(url);
		when(mockClient.putFileToURL(url, mockChunk, token.getContentType())).thenReturn("Result");
		ChunkResult result = worker.call();
		assertEquals(expected, result);
	}
	
	@Test
	public void testTwoError() throws Exception{
		final ChunkResult expected = new ChunkResult();
		expected.setChunkNumber(request.getChunkNumber());
		expected.setEtag("etag");
		final int count = 0;
		when(mockClient.addChunkToFile(request)).thenAnswer(new Answer<ChunkResult>() {
			int count = 0;
			@Override
			public ChunkResult answer(InvocationOnMock invocation)	throws Throwable {
				count++;
				// Fail the first two times
				if(count < 3) throw new RuntimeException("Some failure");
				// then pass
				return expected;
			}
		});
		URL url = new URL("http://google.com");
		when(mockClient.createChunkedPresignedUrl(request)).thenReturn(url);
		when(mockClient.putFileToURL(url, mockChunk, token.getContentType())).thenReturn("Result");
		ChunkResult result = worker.call();
		assertEquals(expected, result);
	}
	
	@Test (expected=RuntimeException.class)
	public void testThreeError() throws Exception{
		// This time it will fail three times so the error should be thrown.
		final ChunkResult expected = new ChunkResult();
		expected.setChunkNumber(request.getChunkNumber());
		expected.setEtag("etag");
		final int count = 0;
		when(mockClient.addChunkToFile(request)).thenAnswer(new Answer<ChunkResult>() {
			int count = 0;
			@Override
			public ChunkResult answer(InvocationOnMock invocation)	throws Throwable {
				count++;
				// Fail the every time
				if(count < 4) throw new RuntimeException("Some failure");
				// then pass
				return expected;
			}
		});
		URL url = new URL("http://google.com");
		when(mockClient.createChunkedPresignedUrl(request)).thenReturn(url);
		when(mockClient.putFileToURL(url, mockChunk, token.getContentType())).thenReturn("Result");
		ChunkResult result = worker.call();
		assertEquals(expected, result);
	}
	
}
