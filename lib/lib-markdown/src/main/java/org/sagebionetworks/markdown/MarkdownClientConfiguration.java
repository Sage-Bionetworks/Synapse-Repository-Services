package org.sagebionetworks.markdown;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.v2.AwsCredentialsProviderV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;

import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;

@Configuration
@ImportResource("classpath:stack-configuration.spb.xml")
public class MarkdownClientConfiguration {

	private final StackConfiguration stackConfiguration;

	public MarkdownClientConfiguration(StackConfiguration stackConfiguration) {
		this.stackConfiguration = stackConfiguration;
	}

	@Bean
	@Scope("singleton")
	public RequestSigner requestSigner() {
		return new AwsV4RequestSigner(
			AwsCredentialsProviderV2.PROVIDER_CHAIN,
			AwsV4HttpSigner.create()
		);
	}

	@Bean
	@Scope("singleton")
	public MarkdownClient markdownClient() {
		return new MarkdownClient(
			stackConfiguration.getMarkdownServiceEndpoint(),
			requestSigner(),
			null
		);
	}

	@Bean
	@Scope("singleton")
	public MarkdownDao markdownDao() {
		return new MarkdownDaoImpl(
			markdownClient(),
			stackConfiguration.getSynapseBaseUrl()
		);
	}
}
