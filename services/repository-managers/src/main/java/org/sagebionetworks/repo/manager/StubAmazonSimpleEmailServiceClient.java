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
import com.amazonaws.services.simpleemail.model.CloneReceiptRuleSetRequest;
import com.amazonaws.services.simpleemail.model.CloneReceiptRuleSetResult;
import com.amazonaws.services.simpleemail.model.CreateReceiptFilterRequest;
import com.amazonaws.services.simpleemail.model.CreateReceiptFilterResult;
import com.amazonaws.services.simpleemail.model.CreateReceiptRuleRequest;
import com.amazonaws.services.simpleemail.model.CreateReceiptRuleResult;
import com.amazonaws.services.simpleemail.model.CreateReceiptRuleSetRequest;
import com.amazonaws.services.simpleemail.model.CreateReceiptRuleSetResult;
import com.amazonaws.services.simpleemail.model.DeleteIdentityPolicyRequest;
import com.amazonaws.services.simpleemail.model.DeleteIdentityPolicyResult;
import com.amazonaws.services.simpleemail.model.DeleteIdentityRequest;
import com.amazonaws.services.simpleemail.model.DeleteIdentityResult;
import com.amazonaws.services.simpleemail.model.DeleteReceiptFilterRequest;
import com.amazonaws.services.simpleemail.model.DeleteReceiptFilterResult;
import com.amazonaws.services.simpleemail.model.DeleteReceiptRuleRequest;
import com.amazonaws.services.simpleemail.model.DeleteReceiptRuleResult;
import com.amazonaws.services.simpleemail.model.DeleteReceiptRuleSetRequest;
import com.amazonaws.services.simpleemail.model.DeleteReceiptRuleSetResult;
import com.amazonaws.services.simpleemail.model.DeleteVerifiedEmailAddressRequest;
import com.amazonaws.services.simpleemail.model.DeleteVerifiedEmailAddressResult;
import com.amazonaws.services.simpleemail.model.DescribeActiveReceiptRuleSetRequest;
import com.amazonaws.services.simpleemail.model.DescribeActiveReceiptRuleSetResult;
import com.amazonaws.services.simpleemail.model.DescribeReceiptRuleRequest;
import com.amazonaws.services.simpleemail.model.DescribeReceiptRuleResult;
import com.amazonaws.services.simpleemail.model.DescribeReceiptRuleSetRequest;
import com.amazonaws.services.simpleemail.model.DescribeReceiptRuleSetResult;
import com.amazonaws.services.simpleemail.model.GetIdentityDkimAttributesRequest;
import com.amazonaws.services.simpleemail.model.GetIdentityDkimAttributesResult;
import com.amazonaws.services.simpleemail.model.GetIdentityMailFromDomainAttributesRequest;
import com.amazonaws.services.simpleemail.model.GetIdentityMailFromDomainAttributesResult;
import com.amazonaws.services.simpleemail.model.GetIdentityNotificationAttributesRequest;
import com.amazonaws.services.simpleemail.model.GetIdentityNotificationAttributesResult;
import com.amazonaws.services.simpleemail.model.GetIdentityPoliciesRequest;
import com.amazonaws.services.simpleemail.model.GetIdentityPoliciesResult;
import com.amazonaws.services.simpleemail.model.GetIdentityVerificationAttributesRequest;
import com.amazonaws.services.simpleemail.model.GetIdentityVerificationAttributesResult;
import com.amazonaws.services.simpleemail.model.GetSendQuotaRequest;
import com.amazonaws.services.simpleemail.model.GetSendQuotaResult;
import com.amazonaws.services.simpleemail.model.GetSendStatisticsRequest;
import com.amazonaws.services.simpleemail.model.GetSendStatisticsResult;
import com.amazonaws.services.simpleemail.model.ListIdentitiesRequest;
import com.amazonaws.services.simpleemail.model.ListIdentitiesResult;
import com.amazonaws.services.simpleemail.model.ListIdentityPoliciesRequest;
import com.amazonaws.services.simpleemail.model.ListIdentityPoliciesResult;
import com.amazonaws.services.simpleemail.model.ListReceiptFiltersRequest;
import com.amazonaws.services.simpleemail.model.ListReceiptFiltersResult;
import com.amazonaws.services.simpleemail.model.ListReceiptRuleSetsRequest;
import com.amazonaws.services.simpleemail.model.ListReceiptRuleSetsResult;
import com.amazonaws.services.simpleemail.model.ListVerifiedEmailAddressesRequest;
import com.amazonaws.services.simpleemail.model.ListVerifiedEmailAddressesResult;
import com.amazonaws.services.simpleemail.model.PutIdentityPolicyRequest;
import com.amazonaws.services.simpleemail.model.PutIdentityPolicyResult;
import com.amazonaws.services.simpleemail.model.ReorderReceiptRuleSetRequest;
import com.amazonaws.services.simpleemail.model.ReorderReceiptRuleSetResult;
import com.amazonaws.services.simpleemail.model.SendBounceRequest;
import com.amazonaws.services.simpleemail.model.SendBounceResult;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import com.amazonaws.services.simpleemail.model.SetActiveReceiptRuleSetRequest;
import com.amazonaws.services.simpleemail.model.SetActiveReceiptRuleSetResult;
import com.amazonaws.services.simpleemail.model.SetIdentityDkimEnabledRequest;
import com.amazonaws.services.simpleemail.model.SetIdentityDkimEnabledResult;
import com.amazonaws.services.simpleemail.model.SetIdentityFeedbackForwardingEnabledRequest;
import com.amazonaws.services.simpleemail.model.SetIdentityFeedbackForwardingEnabledResult;
import com.amazonaws.services.simpleemail.model.SetIdentityHeadersInNotificationsEnabledRequest;
import com.amazonaws.services.simpleemail.model.SetIdentityHeadersInNotificationsEnabledResult;
import com.amazonaws.services.simpleemail.model.SetIdentityMailFromDomainRequest;
import com.amazonaws.services.simpleemail.model.SetIdentityMailFromDomainResult;
import com.amazonaws.services.simpleemail.model.SetIdentityNotificationTopicRequest;
import com.amazonaws.services.simpleemail.model.SetIdentityNotificationTopicResult;
import com.amazonaws.services.simpleemail.model.SetReceiptRulePositionRequest;
import com.amazonaws.services.simpleemail.model.SetReceiptRulePositionResult;
import com.amazonaws.services.simpleemail.model.UpdateReceiptRuleRequest;
import com.amazonaws.services.simpleemail.model.UpdateReceiptRuleResult;
import com.amazonaws.services.simpleemail.model.VerifyDomainDkimRequest;
import com.amazonaws.services.simpleemail.model.VerifyDomainDkimResult;
import com.amazonaws.services.simpleemail.model.VerifyDomainIdentityRequest;
import com.amazonaws.services.simpleemail.model.VerifyDomainIdentityResult;
import com.amazonaws.services.simpleemail.model.VerifyEmailAddressRequest;
import com.amazonaws.services.simpleemail.model.VerifyEmailAddressResult;
import com.amazonaws.services.simpleemail.model.VerifyEmailIdentityRequest;
import com.amazonaws.services.simpleemail.model.VerifyEmailIdentityResult;
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
	
}
