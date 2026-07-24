package org.sagebionetworks.docusign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.docusign.esign.client.ApiException;

@ExtendWith(MockitoExtension.class)
public class DocuSignApiRetryHelperTest {

	@Mock
	private DocuSignAccessTokenProvider mockAccessTokenProvider;

	private DocuSignApiRetryHelper retryHelper;

	@BeforeEach
	public void before() {
		retryHelper = new DocuSignApiRetryHelper(mockAccessTokenProvider);
	}

	@Test
	public void testExecuteWithRetrySuccess() throws Exception {
		when(mockAccessTokenProvider.getAccessToken()).thenReturn("token-1");

		// call under test
		String result = retryHelper.executeWithRetry(token -> "result-" + token);

		assertEquals("result-token-1", result);
	}

	@Test
	public void testExecuteWithRetryOn401() throws Exception {
		when(mockAccessTokenProvider.getAccessToken())
				.thenReturn("stale-token")
				.thenReturn("fresh-token");

		// call under test
		String result = retryHelper.executeWithRetry(token -> {
			if ("stale-token".equals(token)) {
				throw new ApiException(401, "Unauthorized");
			}
			return "result-" + token;
		});

		assertEquals("result-fresh-token", result);
		verify(mockAccessTokenProvider).invalidateAccessToken();
		verify(mockAccessTokenProvider, times(2)).getAccessToken();
	}

	@Test
	public void testExecuteWithRetryPropagatesPersistent401() throws Exception {
		when(mockAccessTokenProvider.getAccessToken())
				.thenReturn("token-1")
				.thenReturn("token-2");

		// call under test
		assertThrows(DocuSignUnauthorizedException.class, () ->
				retryHelper.executeWithRetry(token -> {
					throw new ApiException(401, "Unauthorized");
				}));

		verify(mockAccessTokenProvider).invalidateAccessToken();
		verify(mockAccessTokenProvider, times(2)).getAccessToken();
	}

	@Test
	public void testExecuteWithRetryPropagatesNon401Error() throws Exception {
		when(mockAccessTokenProvider.getAccessToken()).thenReturn("token-1");

		// call under test
		IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
				retryHelper.executeWithRetry(token -> {
					throw new ApiException(500, "Server error");
				}));

		assertEquals("DocuSign API error 500.", ex.getMessage());
	}
}
