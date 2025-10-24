package org.sagebionetworks.markdown;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.AwsClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Configuration
@ImportResource("classpath:stack-configuration.spb.xml")
public class MarkdownConfig {

    @Autowired
    private StackConfiguration stackConfiguration;

    @Bean
    @Scope("singleton")
    public LambdaClient lambdaClient() {
        LambdaClient lambdaClient = AwsClientFactory.createLambdaClient();
        return lambdaClient;
    }

    @Bean
    @Scope("singleton")
    public MarkdownDao markdownDao() {
        MarkdownDaoImpl markdownDao = new MarkdownDaoImpl(lambdaClient(), stackConfiguration.getSynapseBaseUrl(), stackConfiguration.getStack());
        return markdownDao;
    }

}
