package org.sagebionetworks.repo.manager.file;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.KeyPairUtil;

import java.security.PrivateKey;
import java.util.concurrent.ExecutionException;

@RunWith(MockitoJUnitRunner.class)
public class CloudFrontCacheTest {
	@Mock
	StackConfiguration mockStackConfig;

	@InjectMocks
	@Spy
	private CloudFrontCache cloudFrontCache;

	private static final String FAKE_PRIVATE_KEY =
			"MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDY+638Ns56F57p" +
					"JwXhp9y4BJ5pn3mlcV6G33XwxQYTYOgAWD284C4ZME8+/boLGjcbtZEcoZXEwelx" +
					"Ouwu8Zbc7LMRI8lafYpXtVNkICjArEQKyfA/17Ikdl4w5xu9kyfItthfyzsZxnke" +
					"eiBQr7yl+0qbaCCLUcWJHew5BkNstH+NtQSFQpEmLnXDDkn07IIxtHJ2r497qBb+" +
					"w+wCxv6GGpZZ/xPrLigHjixE1rG/yzpWf+6u91LgznJmv928VNW0QZmYinZd83jq" +
					"eNpuYoERRtJleqjmXyfWTxtP/b2zRZqTmPWCh9IgJT+sq3Rev8LI0wRC6KeTuKgs" +
					"rSAxzT2nAgMBAAECggEAcTKry96TzWIxRxVSnizCm0XdluDZx5Pjap19nARNbSKr" +
					"JjLi0nxp0D5BuW0I9+3PPid08ujhh2pabPX+bWcf+1WI/bIbw5em6qbwQFX+rLWy" +
					"Maa0LbpLd3ZBIWYQNNBmevHY4/DUflfqrBmubimgUz9L5tNl1wjr8uKncABygGyc" +
					"CVTry9GUeaOXKugn7ODLvbkQeVFzwzibmzkMCTk5RH6ZOA92ZfHEmBlgQSNL1x0S" +
					"Kfhm9ZV5qhnZFnrCRYlG2BCJgjL1hXYoubDchT3uiL9KgsMZw+4ydDTAivKoxYue" +
					"wdDz5VbjQzU+zKlXzgAE+A585nFrYTxFD/WpGIcHYQKBgQDuOUbT+o7DgLLi/T+w" +
					"dlkw1I5dwvlbV/ee4XkKHcmMVMGUfi4TcHmHFs/sx+KKiUmGfkxR2oq/Vhv0uIS0" +
					"i0WdTkmYbzv+bmhsYuLLiKeYtSh3nYSBP4Ki3AZ8VUnQ7rfBlfYCwZXyUSVUawRI" +
					"QUX2uYKOKo9qxboxNeo0nkkPmQKBgQDpLKY32Bk6TAF5mIMmCxjFiX/D99ljHINy" +
					"nqw9x6jF4D5LM7NNgDrr862ypKBQpWQG9rd6CupX2qNfQ31M64xZhzuyAyGikRjs" +
					"COJ1s3CxSooAYDOjjGK5SGZiz7rrnPjMPHWbDp1wnhJ1SHNerRazCSng0OKEFNgQ" +
					"85ijMzH/PwKBgQDkZgHkZ0vNYW0heFFB7JYi3QgKGU9eJn8A04hrDJgadYCL0FZ4" +
					"yNObk2GS0SoATRQzYI/nwrJYNETlYqvJNeZupYqmHa/VhyGTGVP8dG7LWJUN6fYK" +
					"vUuQvYdyWYtGSDnh3tdZWSVciDRUNa6LYBmmLcJgb6nFYwHbAKgl/sRpsQKBgQDW" +
					"wjnhi1ZI/EILhW2dd3D8V1Tm4HtHLrbetcf8Ks2GWq/lQZvuUKF0On6L39aMEJid" +
					"VVTNwgnumsAH+LgKRZSBzO0tWnb7LNqgYtp4/6lWkUmjaPeGtcEj18v9TEhjw7Lf" +
					"IPxMsNxPIjfr76va0l7qzRDWMG3AqxYKHuJBxeBRrwKBgQC8wo3O4fXHeD5F23os" +
					"ElJr6aAq5D2Jn/bB7uNIgLFo3DHS0Rit/Gx9fkC0etYHAB+lTuLkced4DT9I4VTS" +
					"8PkstKI0as7Y9y34pXdcpLVHl37a2wIWbF3q1uF6jtXjXcX/mql6XuLyX6waboiX" +
					"KVLO30STVTmxgLTbhXQ157Kg4Q==";

	@Test
	public void testGetPrivateKey() {
		when(mockStackConfig.getCloudFrontPrivateKey()).thenReturn(FAKE_PRIVATE_KEY);

		// Call under test
		PrivateKey result = cloudFrontCache.getPrivateKey();
		result = cloudFrontCache.getPrivateKey();

		PrivateKey expected = KeyPairUtil.getPrivateKeyFromPEM(FAKE_PRIVATE_KEY, "RSA");

		verify(mockStackConfig, times(1)).getCloudFrontPrivateKey();
		assertEquals(expected, result);
	}

	@Test
	public void testGetDomainName() {
		String expectedDomain = "data.dev.sagebase.org";

		when(mockStackConfig.getCloudFrontDomainName()).thenReturn(expectedDomain);

		// Call under test
		String result = cloudFrontCache.getDomainName();
		result = cloudFrontCache.getDomainName();

		verify(mockStackConfig, times(1)).getCloudFrontDomainName();
		assertEquals(expectedDomain, result);
	}

	@Test
	public void testGetKeyPairId() {
		String expectedId = "12345";

		when(mockStackConfig.getCloudFrontKeyPairId()).thenReturn(expectedId);

		// Call under test
		String result = cloudFrontCache.getKeyPairId();
		result = cloudFrontCache.getKeyPairId();

		verify(mockStackConfig, times(1)).getCloudFrontKeyPairId();
		assertEquals(expectedId, result);
	}

}
