package org.sagebionetworks.docusign;

import java.nio.charset.StandardCharsets;

import org.sagebionetworks.StackConfiguration;
import org.springframework.stereotype.Component;

@Component
public class StackDocuSignConfigProvider implements DocuSignClientConfig {

	private final StackConfiguration stackConfiguration;

	public StackDocuSignConfigProvider(StackConfiguration stackConfiguration) {
		this.stackConfiguration = stackConfiguration;
	}

	@Override
	public String getIntegrationKey() {
		return stackConfiguration.getDocuSignIntegrationKey();
	}

	@Override
	public String getUserId() {
		return stackConfiguration.getDocuSignUserId();
	}

	@Override
	public String getAccountId() {
		return stackConfiguration.getDocuSignAccountId();
	}

	@Override
	public byte[] getPrivateKeyBytes() {
		String pem = stackConfiguration.getDocuSignPrivateKey();
		if (pem == null) {
			return null;
		}
		return pem.replace("\\n", "\n").getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public String getBasePath() {
		return stackConfiguration.getDocuSignBasePath();
	}

	@Override
	public String getOAuthBasePath() {
		return stackConfiguration.getDocuSignOAuthBasePath();
	}
}
