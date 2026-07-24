package org.sagebionetworks.repo.manager.config;

import java.time.Duration;

import org.sagebionetworks.StackConfiguration;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterConfiguration;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.json.JsonReadFeature;

import jakarta.annotation.PostConstruct;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreAsyncClient;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;
import software.amazon.awssdk.services.bedrockagentcorecontrol.BedrockAgentCoreControlClient;
import software.amazon.awssdk.services.bedrockagentcorecontrol.model.CodeInterpreterSummary;
import software.amazon.awssdk.services.bedrockagentcorecontrol.model.ListCodeInterpretersRequest;
import software.amazon.awssdk.services.bedrockagentcorecontrol.model.ListCodeInterpretersResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Spring AI configuration for Bedrock Converse ChatModel and AgentCore services.
 */
@Configuration
public class SpringAiConfiguration {

	/**
	 * Spring AI parses LLM tool-call argument JSON through two separate shared, strict ObjectMappers,
	 * both of which reject literal control characters (e.g. a raw newline, code 10) inside JSON string
	 * values. Bedrock/Claude routinely emit multi-line text as a tool argument (for example the
	 * {@code script} passed to runPython, or a multi-line SQL query delegated to a specialist) with
	 * unescaped newlines, which would otherwise fail with "Illegal unquoted character (CTRL-CHAR, code 10)".
	 * <p>
	 * The two mappers are:
	 * <ul>
	 * <li>{@link JsonParser#getObjectMapper()} — used by {@code MethodToolCallback.extractToolArguments}
	 * when a tool is invoked (parsing the arguments into method parameters).</li>
	 * <li>{@link ModelOptionsUtils#OBJECT_MAPPER} — used by {@code BedrockProxyChatModel.createRequest}
	 * (via {@code ModelOptionsUtils.jsonToMap}) when a prior assistant tool-call's stored arguments are
	 * re-serialized into the next Bedrock request on a subsequent turn.</li>
	 * </ul>
	 * Enabling {@link JsonReadFeature#ALLOW_UNESCAPED_CONTROL_CHARS} on both mappers' factories makes
	 * tool-argument parsing tolerant of these characters everywhere. This is strictly more lenient: it
	 * never rejects valid JSON, it only additionally accepts input the strict parser refused. Applied
	 * once at startup so any code that resolves this @Configuration gets the behavior.
	 */
	@PostConstruct
	public void configureToolArgumentJsonParsing() {
		JsonParser.getObjectMapper().getFactory()
				.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
		ModelOptionsUtils.OBJECT_MAPPER.getFactory()
				.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
	}

	@Bean
	public ChatModel bedrockChatModel(AwsCredentialsProvider credentialProvider, StackConfiguration stackConfig) {
		return BedrockProxyChatModel.builder()
				.credentialsProvider(credentialProvider)
				.region(Region.of(stackConfig.getBedrockConverseRegion()))
				.timeout(Duration.ofMinutes(5))
				.defaultOptions(BedrockChatOptions.builder()
						.model(stackConfig.getModelIdClaudeHaiku())
						.build())
				.build();
	}

	@Bean
	public BedrockAgentCoreClient bedrockAgentCoreClient(AwsCredentialsProvider credentialProvider,
			StackConfiguration stackConfig) {
		return BedrockAgentCoreClient.builder()
				.credentialsProvider(credentialProvider)
				.region(Region.of(stackConfig.getBedrockConverseRegion()))
				.build();
	}

	@Bean
	public BedrockAgentCoreAsyncClient bedrockAgentCoreAsyncClient(AwsCredentialsProvider credentialProvider,
			StackConfiguration stackConfig) {
		return BedrockAgentCoreAsyncClient.builder()
				.credentialsProvider(credentialProvider)
				.region(Region.of(stackConfig.getBedrockConverseRegion()))
				.build();
	}

	@Bean
	public AgentCoreCodeInterpreterClient agentCoreCodeInterpreterClient(BedrockAgentCoreClient syncClient,
			BedrockAgentCoreAsyncClient asyncClient, AwsCredentialsProvider credentialProvider,
			StackConfiguration stackConfig) {
		String codeInterpreterIdentifier = lookupCodeInterpreterIdentifier(credentialProvider, stackConfig);
		return new AgentCoreCodeInterpreterClient(syncClient, asyncClient,
				new AgentCoreCodeInterpreterConfiguration(null, codeInterpreterIdentifier, null, null, null, null));
	}

	private String lookupCodeInterpreterIdentifier(AwsCredentialsProvider credentialProvider,
			StackConfiguration stackConfig) {
		String expectedName = stackConfig.getStack() + "_" + stackConfig.getStackInstance() + "_code_interpreter";
		try (BedrockAgentCoreControlClient controlClient = BedrockAgentCoreControlClient.builder()
				.credentialsProvider(credentialProvider)
				.region(Region.of(stackConfig.getBedrockConverseRegion()))
				.build()) {
			String nextToken = null;
			do {
				ListCodeInterpretersResponse response = controlClient.listCodeInterpreters(
						ListCodeInterpretersRequest.builder().nextToken(nextToken).build());
				for (CodeInterpreterSummary summary : response.codeInterpreterSummaries()) {
					if (expectedName.equals(summary.name())) {
						return summary.codeInterpreterId();
					}
				}
				nextToken = response.nextToken();
			} while (nextToken != null);
		}
		throw new IllegalStateException("Code interpreter not found with name: " + expectedName);
	}

	@Bean
	public S3Presigner s3Presigner(AwsCredentialsProvider credentialProvider) {
		return S3Presigner.builder()
				.credentialsProvider(credentialProvider)
				.region(Region.US_EAST_1)
				.build();
	}

	@Bean
	public ChatMemory agentCoreChatMemory() {
		return MessageWindowChatMemory.builder()
				.maxMessages(100)
				.build();
	}
}
