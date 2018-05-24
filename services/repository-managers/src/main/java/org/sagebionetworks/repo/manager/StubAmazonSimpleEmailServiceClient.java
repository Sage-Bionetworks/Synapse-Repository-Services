package org.sagebionetworks.repo.manager;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.regions.Region;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import com.amazonaws.services.simpleemail.waiters.AmazonSimpleEmailServiceWaiters;

/**
 * Stub implementation of Amazon's SES client
 * Used to prevent messages from being sent during testing
 */
public class StubAmazonSimpleEmailServiceClient implements AmazonSimpleEmailService {
	
	@Override
	public void setEndpoint(String endpoint) throws IllegalArgumentException { }

	@Override
	public DeleteIdentityResult deleteIdentity(
			DeleteIdentityRequest deleteIdentityRequest)
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public ListVerifiedEmailAddressesResult listVerifiedEmailAddresses(
			ListVerifiedEmailAddressesRequest listVerifiedEmailAddressesRequest)
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public GetSendStatisticsResult getSendStatistics(
			GetSendStatisticsRequest getSendStatisticsRequest)
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public VerifyEmailIdentityResult verifyEmailIdentity(
			VerifyEmailIdentityRequest verifyEmailIdentityRequest)
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public GetIdentityNotificationAttributesResult getIdentityNotificationAttributes(
			GetIdentityNotificationAttributesRequest getIdentityNotificationAttributesRequest)
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public VerifyDomainDkimResult verifyDomainDkim(
			VerifyDomainDkimRequest verifyDomainDkimRequest)
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public GetIdentityDkimAttributesResult getIdentityDkimAttributes(
			GetIdentityDkimAttributesRequest getIdentityDkimAttributesRequest)
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public SendRawEmailResult sendRawEmail(
			SendRawEmailRequest sendRawEmailRequest)
			throws AmazonServiceException, AmazonClientException {
		try {
			MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
					new ByteArrayInputStream(sendRawEmailRequest.getRawMessage().getData().array()));
			if (mimeMessage.getSubject().indexOf(MESSAGE_SUBJECT_FOR_FAILURE)>=0) {
				throw new RuntimeException(TRANSMISSION_FAILURE);
			}
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	@Override
	public ListIdentitiesResult listIdentities(
			ListIdentitiesRequest listIdentitiesRequest)
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public GetIdentityVerificationAttributesResult getIdentityVerificationAttributes(
			GetIdentityVerificationAttributesRequest getIdentityVerificationAttributesRequest)
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public SetIdentityDkimEnabledResult setIdentityDkimEnabled(
			SetIdentityDkimEnabledRequest setIdentityDkimEnabledRequest)
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public GetSendQuotaResult getSendQuota(
			GetSendQuotaRequest getSendQuotaRequest)
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public SetIdentityFeedbackForwardingEnabledResult setIdentityFeedbackForwardingEnabled(
			SetIdentityFeedbackForwardingEnabledRequest setIdentityFeedbackForwardingEnabledRequest)
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public VerifyDomainIdentityResult verifyDomainIdentity(
			VerifyDomainIdentityRequest verifyDomainIdentityRequest)
			throws AmazonServiceException, AmazonClientException {
		return null;
	}
	
	public static final String MESSAGE_SUBJECT_FOR_FAILURE = "generatefailure";
	public static final String TRANSMISSION_FAILURE = "transmission failure";

	@Override
	public SendEmailResult sendEmail(SendEmailRequest sendEmailRequest)
			throws AmazonServiceException, AmazonClientException {
		if (sendEmailRequest.getMessage().getSubject().getData().toLowerCase().indexOf(MESSAGE_SUBJECT_FOR_FAILURE)>=0) {
			throw new RuntimeException(TRANSMISSION_FAILURE);
		}
		return null;
	}

	@Override
	public SetIdentityNotificationTopicResult setIdentityNotificationTopic(
			SetIdentityNotificationTopicRequest setIdentityNotificationTopicRequest)
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public ListVerifiedEmailAddressesResult listVerifiedEmailAddresses()
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public GetSendStatisticsResult getSendStatistics()
			throws AmazonServiceException, AmazonClientException {
		return null;
	}

	@Override
	public ListIdentitiesResult listIdentities() throws AmazonServiceException,
			AmazonClientException {
		return null;
	}

	@Override
	public GetSendQuotaResult getSendQuota() throws AmazonServiceException,
			AmazonClientException {
		return null;
	}

	@Override
	public void shutdown() { }

	@Override
	public ResponseMetadata getCachedResponseMetadata(
			AmazonWebServiceRequest request) {
		return null;
	}

	@Override
	public void setRegion(Region region) throws IllegalArgumentException {
		// TODO Auto-generated method stub

	}

	@Override
	public CloneReceiptRuleSetResult cloneReceiptRuleSet(CloneReceiptRuleSetRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateReceiptFilterResult createReceiptFilter(CreateReceiptFilterRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateReceiptRuleResult createReceiptRule(CreateReceiptRuleRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateReceiptRuleSetResult createReceiptRuleSet(CreateReceiptRuleSetRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteIdentityPolicyResult deleteIdentityPolicy(DeleteIdentityPolicyRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteReceiptFilterResult deleteReceiptFilter(DeleteReceiptFilterRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteReceiptRuleResult deleteReceiptRule(DeleteReceiptRuleRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteReceiptRuleSetResult deleteReceiptRuleSet(DeleteReceiptRuleSetRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteVerifiedEmailAddressResult deleteVerifiedEmailAddress(DeleteVerifiedEmailAddressRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DescribeActiveReceiptRuleSetResult describeActiveReceiptRuleSet(DescribeActiveReceiptRuleSetRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DescribeReceiptRuleResult describeReceiptRule(DescribeReceiptRuleRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DescribeReceiptRuleSetResult describeReceiptRuleSet(DescribeReceiptRuleSetRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GetIdentityMailFromDomainAttributesResult getIdentityMailFromDomainAttributes(
			GetIdentityMailFromDomainAttributesRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GetIdentityPoliciesResult getIdentityPolicies(GetIdentityPoliciesRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIdentityPoliciesResult listIdentityPolicies(ListIdentityPoliciesRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListReceiptFiltersResult listReceiptFilters(ListReceiptFiltersRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListReceiptRuleSetsResult listReceiptRuleSets(ListReceiptRuleSetsRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PutIdentityPolicyResult putIdentityPolicy(PutIdentityPolicyRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReorderReceiptRuleSetResult reorderReceiptRuleSet(ReorderReceiptRuleSetRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SendBounceResult sendBounce(SendBounceRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SetActiveReceiptRuleSetResult setActiveReceiptRuleSet(SetActiveReceiptRuleSetRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SetIdentityHeadersInNotificationsEnabledResult setIdentityHeadersInNotificationsEnabled(
			SetIdentityHeadersInNotificationsEnabledRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SetIdentityMailFromDomainResult setIdentityMailFromDomain(SetIdentityMailFromDomainRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SetReceiptRulePositionResult setReceiptRulePosition(SetReceiptRulePositionRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateReceiptRuleResult updateReceiptRule(UpdateReceiptRuleRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VerifyEmailAddressResult verifyEmailAddress(VerifyEmailAddressRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AmazonSimpleEmailServiceWaiters waiters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateConfigurationSetResult createConfigurationSet(
			CreateConfigurationSetRequest createConfigurationSetRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateConfigurationSetEventDestinationResult createConfigurationSetEventDestination(
			CreateConfigurationSetEventDestinationRequest createConfigurationSetEventDestinationRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateConfigurationSetTrackingOptionsResult createConfigurationSetTrackingOptions(
			CreateConfigurationSetTrackingOptionsRequest createConfigurationSetTrackingOptionsRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateCustomVerificationEmailTemplateResult createCustomVerificationEmailTemplate(
			CreateCustomVerificationEmailTemplateRequest createCustomVerificationEmailTemplateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateTemplateResult createTemplate(CreateTemplateRequest createTemplateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteConfigurationSetResult deleteConfigurationSet(
			DeleteConfigurationSetRequest deleteConfigurationSetRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteConfigurationSetEventDestinationResult deleteConfigurationSetEventDestination(
			DeleteConfigurationSetEventDestinationRequest deleteConfigurationSetEventDestinationRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteConfigurationSetTrackingOptionsResult deleteConfigurationSetTrackingOptions(
			DeleteConfigurationSetTrackingOptionsRequest deleteConfigurationSetTrackingOptionsRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteCustomVerificationEmailTemplateResult deleteCustomVerificationEmailTemplate(
			DeleteCustomVerificationEmailTemplateRequest deleteCustomVerificationEmailTemplateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteTemplateResult deleteTemplate(DeleteTemplateRequest deleteTemplateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DescribeConfigurationSetResult describeConfigurationSet(
			DescribeConfigurationSetRequest describeConfigurationSetRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GetAccountSendingEnabledResult getAccountSendingEnabled(
			GetAccountSendingEnabledRequest getAccountSendingEnabledRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GetCustomVerificationEmailTemplateResult getCustomVerificationEmailTemplate(
			GetCustomVerificationEmailTemplateRequest getCustomVerificationEmailTemplateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GetTemplateResult getTemplate(GetTemplateRequest getTemplateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListConfigurationSetsResult listConfigurationSets(
			ListConfigurationSetsRequest listConfigurationSetsRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListCustomVerificationEmailTemplatesResult listCustomVerificationEmailTemplates(
			ListCustomVerificationEmailTemplatesRequest listCustomVerificationEmailTemplatesRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListTemplatesResult listTemplates(ListTemplatesRequest listTemplatesRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SendBulkTemplatedEmailResult sendBulkTemplatedEmail(
			SendBulkTemplatedEmailRequest sendBulkTemplatedEmailRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SendCustomVerificationEmailResult sendCustomVerificationEmail(
			SendCustomVerificationEmailRequest sendCustomVerificationEmailRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SendTemplatedEmailResult sendTemplatedEmail(SendTemplatedEmailRequest sendTemplatedEmailRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TestRenderTemplateResult testRenderTemplate(TestRenderTemplateRequest testRenderTemplateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateAccountSendingEnabledResult updateAccountSendingEnabled(
			UpdateAccountSendingEnabledRequest updateAccountSendingEnabledRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateConfigurationSetEventDestinationResult updateConfigurationSetEventDestination(
			UpdateConfigurationSetEventDestinationRequest updateConfigurationSetEventDestinationRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateConfigurationSetReputationMetricsEnabledResult updateConfigurationSetReputationMetricsEnabled(
			UpdateConfigurationSetReputationMetricsEnabledRequest updateConfigurationSetReputationMetricsEnabledRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateConfigurationSetSendingEnabledResult updateConfigurationSetSendingEnabled(
			UpdateConfigurationSetSendingEnabledRequest updateConfigurationSetSendingEnabledRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateConfigurationSetTrackingOptionsResult updateConfigurationSetTrackingOptions(
			UpdateConfigurationSetTrackingOptionsRequest updateConfigurationSetTrackingOptionsRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateCustomVerificationEmailTemplateResult updateCustomVerificationEmailTemplate(
			UpdateCustomVerificationEmailTemplateRequest updateCustomVerificationEmailTemplateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateTemplateResult updateTemplate(UpdateTemplateRequest updateTemplateRequest) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
