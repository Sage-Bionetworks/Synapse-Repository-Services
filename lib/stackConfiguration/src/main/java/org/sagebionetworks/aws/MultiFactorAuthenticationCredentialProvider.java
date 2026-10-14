package org.sagebionetworks.aws;

import java.util.Scanner;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.GetSessionTokenRequest;
import software.amazon.awssdk.services.sts.model.GetSessionTokenResponse;

/**
 * Credential provider that will prompt the user for a
 * Multi-factor-Authentication (MFA) code for the provided Device ARN.
 *
 */
public class MultiFactorAuthenticationCredentialProvider implements AWSCredentialsProvider {

	private String mfaDeviceArn;
	private StsClient tokenClient;
	private BasicSessionCredentials credentials;

	/**
	 * The ARN of the MFA device that will be used for authentication.
	 *
	 * @param mfaDeviceArn
	 */
	public MultiFactorAuthenticationCredentialProvider(String mfaDeviceArn) {
		this.mfaDeviceArn = mfaDeviceArn;
		this.tokenClient = StsClient.builder()
				.region(Region.US_EAST_1)
				.credentialsProvider(SystemPropertyCredentialsProvider.create())
				.build();
		refresh();
	}

	@Override
	public AWSCredentials getCredentials() {
		return credentials;
	}

	@Override
	public void refresh() {
		// fetch the code from console input.
		int mfaCode;
		try (Scanner in = new Scanner(System.in)) {
			System.out.println("Enter MFA code from the device:");
			mfaCode = in.nextInt();
		}
		GetSessionTokenResponse result = this.tokenClient.getSessionToken(
				GetSessionTokenRequest.builder()
						.serialNumber(this.mfaDeviceArn)
						.tokenCode("" + mfaCode)
						.build());
		Credentials tempCreds = result.credentials();
		this.credentials = new BasicSessionCredentials(
				tempCreds.accessKeyId(),
				tempCreds.secretAccessKey(),
				tempCreds.sessionToken());
	}

}
