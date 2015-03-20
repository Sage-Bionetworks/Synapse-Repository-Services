package org.sagebionetworks.repo.manager;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.regions.Region;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.DeleteIdentityRequest;
import com.amazonaws.services.simpleemail.model.DeleteIdentityResult;
import com.amazonaws.services.simpleemail.model.DeleteVerifiedEmailAddressRequest;
import com.amazonaws.services.simpleemail.model.GetIdentityDkimAttributesRequest;
import com.amazonaws.services.simpleemail.model.GetIdentityDkimAttributesResult;
import com.amazonaws.services.simpleemail.model.GetIdentityNotificationAttributesRequest;
import com.amazonaws.services.simpleemail.model.GetIdentityNotificationAttributesResult;
import com.amazonaws.services.simpleemail.model.GetIdentityVerificationAttributesRequest;
import com.amazonaws.services.simpleemail.model.GetIdentityVerificationAttributesResult;
import com.amazonaws.services.simpleemail.model.GetSendQuotaRequest;
import com.amazonaws.services.simpleemail.model.GetSendQuotaResult;
import com.amazonaws.services.simpleemail.model.GetSendStatisticsRequest;
import com.amazonaws.services.simpleemail.model.GetSendStatisticsResult;
import com.amazonaws.services.simpleemail.model.ListIdentitiesRequest;
import com.amazonaws.services.simpleemail.model.ListIdentitiesResult;
import com.amazonaws.services.simpleemail.model.ListVerifiedEmailAddressesRequest;
import com.amazonaws.services.simpleemail.model.ListVerifiedEmailAddressesResult;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import com.amazonaws.services.simpleemail.model.SetIdentityDkimEnabledRequest;
import com.amazonaws.services.simpleemail.model.SetIdentityDkimEnabledResult;
import com.amazonaws.services.simpleemail.model.SetIdentityFeedbackForwardingEnabledRequest;
import com.amazonaws.services.simpleemail.model.SetIdentityFeedbackForwardingEnabledResult;
import com.amazonaws.services.simpleemail.model.SetIdentityNotificationTopicRequest;
import com.amazonaws.services.simpleemail.model.SetIdentityNotificationTopicResult;
import com.amazonaws.services.simpleemail.model.VerifyDomainDkimRequest;
import com.amazonaws.services.simpleemail.model.VerifyDomainDkimResult;
import com.amazonaws.services.simpleemail.model.VerifyDomainIdentityRequest;
import com.amazonaws.services.simpleemail.model.VerifyDomainIdentityResult;
import com.amazonaws.services.simpleemail.model.VerifyEmailAddressRequest;
import com.amazonaws.services.simpleemail.model.VerifyEmailIdentityRequest;
import com.amazonaws.services.simpleemail.model.VerifyEmailIdentityResult;

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
	public void verifyEmailAddress(
			VerifyEmailAddressRequest verifyEmailAddressRequest)
			throws AmazonServiceException, AmazonClientException {
		
	}

	@Override
	public SendRawEmailResult sendRawEmail(
			SendRawEmailRequest sendRawEmailRequest)
			throws AmazonServiceException, AmazonClientException {
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
	public void deleteVerifiedEmailAddress(
			DeleteVerifiedEmailAddressRequest deleteVerifiedEmailAddressRequest)
			throws AmazonServiceException, AmazonClientException {
		
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
	
}
