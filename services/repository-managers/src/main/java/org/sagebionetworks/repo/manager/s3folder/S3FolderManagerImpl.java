package org.sagebionetworks.repo.manager.s3folder;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetFederationTokenRequest;
import com.amazonaws.services.securitytoken.model.GetFederationTokenResult;
import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.Folder;
import org.springframework.beans.factory.annotation.Autowired;

// todo doc
public class S3FolderManagerImpl implements S3FolderManager {
	private static final int DURATION_SECONDS = 15 * 60; // todo change this to 12 hours
	private static final String READ_ONLY_ACTIONS = "\"s3:Get*\",\"s3:List*\"";
	private static final String READ_WRITE_ACTIONS = "\"s3:*\"";
	private static final String SESSION_POLICY_TEMPLATE = "{\n" +
			"	\"Version\": \"2012-10-17\",\n" +
			"	\"Statement\": [\n" +
			"		{\n" +
			"			\"Sid\": \"ListParentBuckets\",\n" +
			"			\"Action\": [\"s3:ListBucket*\"],\n" +
			"			\"Effect\": \"Allow\",\n" +
			"			\"Resource\": [\"arn:aws:s3:::$bucket\"],\n" +
			"			\"Condition\":{\"StringEquals\":{\"s3:prefix\":[\"$folder\"]}}\n" +
			"		},\n" +
			"		{\n" +
			"			\"Sid\": \"ListBucketAccess\",\n" +
			"			\"Action\": [\"s3:ListBucket*\"],\n" +
			"			\"Effect\": \"Allow\",\n" +
			"			\"Resource\": [\"arn:aws:s3:::$bucket\"],\n" +
			"			\"Condition\":{\"StringLike\":{\"s3:prefix\":[\"$folder/*\"]}}\n" +
			"		},\n" +
			"		{\n" +
			"			\"Sid\": \"FolderAccess\",\n" +
			"			\"Effect\": \"Allow\",\n" +
			"			\"Action\": [\n" +
			"				$actions\n" +
			"			],\n" +
			"			\"Resource\": [\n" +
			"				\"arn:aws:s3:::$bucket/$folder\",\n" +
			"				\"arn:aws:s3:::$bucket/$folder/*\"\n" +
			"			]\n" +
			"		}\n" +
			"	]\n" +
			"}";

	private AWSSecurityTokenService stsClient;

	@Autowired
	public final void setStsClient(AWSSecurityTokenService stsClient) {
		this.stsClient = stsClient;
	}

	/** {@inheritDoc} */
	@Override
	public Credentials getTemporaryCredentials(Folder folder, Permissions permissions) {
		// Determine permissions level.
		String actions = null;
		switch (permissions) {
			case READ_ONLY:
				actions = READ_ONLY_ACTIONS;
				break;
			case READ_WRITE:
				actions = READ_WRITE_ACTIONS;
				break;
		}

		// Optional folder. Convert null and blank to empty strings, so we can substitute correctly.
		String s3Prefix = "";
		// todo
//		if (StringUtils.isNotBlank(s3Folder.getFolder())) {
//			s3Prefix = s3Folder.getFolder();
//		}

		String bucket = ""; //todo

		// Call STS.
		String policy = SESSION_POLICY_TEMPLATE.replace("$bucket", bucket)
				.replace("$folder", s3Prefix)
				.replace("$actions", actions);
		GetFederationTokenRequest request = new GetFederationTokenRequest();
		request.setDurationSeconds(DURATION_SECONDS);
		request.setPolicy(policy);
		GetFederationTokenResult result = stsClient.getFederationToken(request);
		return result.getCredentials();
	}
}
