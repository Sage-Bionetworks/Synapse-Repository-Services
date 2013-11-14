package org.sagebionetworks.client;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;
import org.mockito.*;
import org.sagebionetworks.bridge.model.versionInfo.BridgeVersionInfo;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import com.amazonaws.util.StringInputStream;

import static org.mockito.Mockito.*;

import static org.junit.Assert.fail;

public class BridgeClientTest {

	private static final String URIBASE = "https://bridge-prod.prod.sagebase.org/bridge/v1";

	BridgeClient bridgeClient;
	@Mock
	HttpClientProvider mockHttpClientProvider;
	@Mock
	HttpResponse mockHttpResponse;
	@Mock
	StatusLine mockStatusLine;
	@Mock
	HttpEntity mockEntity;

	@Before
	public void doBefore() {
		MockitoAnnotations.initMocks(this);
		bridgeClient = new BridgeClientImpl(mockHttpClientProvider);
		verify(mockHttpClientProvider).setGlobalConnectionTimeout(ServiceConstants.DEFAULT_CONNECT_TIMEOUT_MSEC);
		verify(mockHttpClientProvider).setGlobalSocketTimeout(ServiceConstants.DEFAULT_SOCKET_TIMEOUT_MSEC);
	}

	@After
	public void doAfter() {
		verifyNoMoreInteractions(mockHttpClientProvider, mockHttpResponse, mockStatusLine, mockEntity);
	}

	@Test
	public void testGetVersionInfo() throws Exception {
		BridgeVersionInfo version = new BridgeVersionInfo();
		version.setVersion("none");

		// You cannot set originClient to bridge here, because getVersionInfo() has no version
		// that takes OriginatingClient (there's no difference in the call between clients). 
		// The underlying libraries always set this to synapse (and it's ignored).
		
		expectRequest(URIBASE + "/version?originClient=synapse", "GET", null, EntityFactory.createJSONStringForEntity(version));

		bridgeClient.getVersionInfo();

		verifyRequest(URIBASE + "/version?originClient=synapse", "GET", null, EntityFactory.createJSONStringForEntity(version));
	}

	@Test
	public void testCreateCommunity() throws Exception {
	}

	@Test
	public void testGetCommunities() throws Exception {
	}

	@Test
	public void testGetCommunity() throws Exception {
	}

	@Test
	public void testUpdateCommunity() throws Exception {
	}

	@SuppressWarnings("unchecked")
	private void expectRequest(String uri, String method, String content, String answer) throws Exception {
		when(mockHttpResponse.getEntity()).thenReturn(mockEntity);
		when(mockEntity.getContentLength()).thenReturn((long) answer.length());
		when(mockEntity.getContent()).thenReturn(new StringInputStream(answer));
		when(mockEntity.getContentType()).thenReturn(null);
		when(mockHttpClientProvider.performRequest(eq(uri), eq(method), eq(content), Mockito.anyMap())).thenReturn(mockHttpResponse);
	}

	@SuppressWarnings("unchecked")
	private void verifyRequest(String uri, String method, String content, String answer) throws Exception {
		verify(mockHttpResponse, times(2)).getEntity();
		verify(mockEntity, times(2)).getContentLength();
		verify(mockEntity).getContent();
		verify(mockEntity).getContentType();
		verify(mockHttpClientProvider).performRequest(eq(uri), eq(method), eq(content), Mockito.anyMap());
	}

	private void expectExecute(HttpUriRequest httpUriRequest, boolean success) throws ClientProtocolException, IOException {
		when(mockStatusLine.getStatusCode()).thenReturn(success ? 200 : 500);
		when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
		if (success) {

		}

		when(mockHttpClientProvider.execute(httpUriRequest)).thenReturn(mockHttpResponse);
	}

	private void verifyExecute(HttpUriRequest httpUriRequest, boolean success) throws ClientProtocolException, IOException {
		verify(mockHttpClientProvider.execute(httpUriRequest));
		verify(mockHttpResponse.getStatusLine());
		verify(mockStatusLine).getStatusCode();
		if (success) {

		}
	}
}
