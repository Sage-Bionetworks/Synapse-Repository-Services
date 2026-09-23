package org.sagebionetworks.markdown;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.regions.Region;

/**
 * AWS Signature Version 4 implementation of RequestSigner.
 */
public class AwsV4RequestSigner implements RequestSigner {

	private final AwsCredentialsProvider awsCredentialsProvider;
	private final AwsV4HttpSigner signer;

	public AwsV4RequestSigner(AwsCredentialsProvider awsCredentialsProvider, AwsV4HttpSigner signer) {
		this.awsCredentialsProvider = awsCredentialsProvider;
		this.signer = signer;
	}

	@Override
	public Map<String, String> sign(URI uri, byte[] payload) {
		SdkHttpRequest httpRequest = SdkHttpRequest.builder()
				.uri(uri)
				.method(SdkHttpMethod.POST)
				.build();

		SignedRequest signedRequest = signer.sign(SignRequest.builder(awsCredentialsProvider.resolveCredentials())
				.request(httpRequest)
				.payload(ContentStreamProvider.fromByteArray(payload))
				.putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "execute-api")
				.putProperty(AwsV4HttpSigner.REGION_NAME, Region.US_EAST_1.toString())
				.build());

		Map<String, String> headers = new HashMap<>();
		signedRequest.request().forEachHeader((name, values) -> {
			// Skip Host — Apache HttpClient regenerates it from the URI; a duplicate would break signature verification
			if (!"Host".equalsIgnoreCase(name)) {
				headers.put(name, String.join(",", values));
			}
		});
		return Collections.unmodifiableMap(headers);
	}
}
