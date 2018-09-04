package org.sagebionetworks.aws;

import java.util.Scanner;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenRequest;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;

/**
 * Credential provider that will prompt the user for a
 * Multi-factor-Authentication (MFA) code for the provided Device ARN.
 *
 */
public class MultiFactorAuthenticationCredentialProvider implements AWSCredentialsProvider {

	private String mfaDeviceArn;
	private AWSSecurityTokenService tokenClient;
	private BasicSessionCredentials credentials;

	/**
	 * The ARN of the MFA device that will be used for authentication.
	 * 
	 * @param mfaDeviceArn
	 */
	public MultiFactorAuthenticationCredentialProvider(String mfaDeviceArn) {
		this.mfaDeviceArn = mfaDeviceArn;
		this.tokenClient = AWSSecurityTokenServiceClientBuilder.standard().withRegion(Regions.US_EAST_1)
				.withCredentials(new SystemPropertiesCredentialsProvider()).build();
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
		GetSessionTokenResult result = this.tokenClient.getSessionToken(
				new GetSessionTokenRequest().withSerialNumber(this.mfaDeviceArn).withTokenCode("" + mfaCode));
		Credentials tempCreds = result.getCredentials();
		this.credentials = new BasicSessionCredentials(tempCreds.getAccessKeyId(), tempCreds.getSecretAccessKey(),
				tempCreds.getSessionToken());
	}

}
