package org.sagebionetworks.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.feature.FeatureStatus;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.IdGeneratorExport;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.MigrationTypeNames;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientConfig;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Java Client API for Synapse Administrative REST APIs
 */
public class SynapseAdminClientImpl extends SynapseClientImpl implements SynapseAdminClient {

	protected static final String ADMIN = "/admin";
	protected static final String ADMIN_STACK_STATUS = ADMIN + "/synapse/status";
	private static final String ADMIN_CHANGE_MESSAGES = ADMIN + "/messages";
	private static final String ADMIN_FIRE_MESSAGES = ADMIN + "/messages/refire";
	private static final String ADMIN_GET_CURRENT_CHANGE_NUM = ADMIN + "/messages/currentnumber";
	private static final String ADMIN_PUBLISH_MESSAGES = ADMIN_CHANGE_MESSAGES + "/rebroadcast";
	private static final String ADMIN_DOI_CLEAR = ADMIN + "/doi/clear";

	private static final String MIGRATION = "/migration";
	private static final String MIGRATION_COUNTS = MIGRATION + "/counts";
	private static final String MIGRATION_COUNT = MIGRATION + "/count";
	private static final String MIGRATION_DELETE = MIGRATION + "/delete";
	private static final String MIGRATION_STATUS = MIGRATION + "/status";
	private static final String MIGRATION_PRIMARY = MIGRATION + "/primarytypes";
	private static final String MIGRATION_PRIMARY_NAMES = MIGRATION_PRIMARY + "/names";
	private static final String MIGRATION_TYPES = MIGRATION + "/types";
	private static final String MIGRATION_TYPE_NAMES = MIGRATION_TYPES + "/names";
	private static final String MIGRATION_RANGE_CHECKSUM = MIGRATION + "/rangechecksum";
	private static final String MIGRATION_TYPE_CHECKSUM = MIGRATION + "/typechecksum";

	private static final String ADMIN_DYNAMO_CLEAR = ADMIN + "/dynamo/clear";
	
	private static final String ADMIN_USER = ADMIN + "/user";
	private static final String ADMIN_CLEAR_LOCKS = ADMIN+"/locks";
	private static final String ADMIN_CREATE_OR_UPDATE_CHANGE_MESSAGES = ADMIN+"/messages/createOrUpdate";
	
	private static final String DAYS_IN_TRASH_PARAM = "daysInTrash";
	
	private static final String ADMIN_ASYNCHRONOUS_JOB = "/admin/asynchronous/job";
	public static final String ADMIN_ID_GEN_EXPORT = ADMIN + "/id/generator/export";

	private static final String EVALUATION_URI_PATH = "/evaluation";
	private static final String SUBMISSION = "submission";

	private static final String CERTIFIED_USER_TEST_RESPONSE = "/certifiedUserTestResponse";
	private static final String PRINCIPAL_ID_REQUEST_PARAM = "principalId";
	private static final String CERTIFIED_USER_STATUS = "/certificationStatus";
	private static final String CERTIFIED_USER_PASSING_RECORDS = "/certifiedUserPassingRecords";
	private static final String OAUTH_CLIENT = "/oauth2/client";

	private static final String VERIFIED = "/verified";
	private static final String MESSAGE = "/message";

	private static final String REDACT_USER = "/redact/user";

	public SynapseAdminClientImpl() {
		super();
	}
	
	public SynapseAdminClientImpl(SimpleHttpClientConfig config) {
		super(config);
	}
	
	/**
	 * @param updated
	 * @return status
	 * @throws SynapseException
	 */
	public StackStatus updateCurrentStackStatus(StackStatus updated) throws SynapseException {
		return putJSONEntity(getRepoEndpoint(), ADMIN_STACK_STATUS, updated, StackStatus.class);
	}
	
	@Override
	public ChangeMessages listMessages(Long startChangeNumber, ObjectType type, Long limit) throws SynapseException{
		// Build up the URL
		String url = buildListMessagesURL(startChangeNumber, type, limit);
		return getJSONEntity(getRepoEndpoint(), url, ChangeMessages.class);
	}
	
	// New migration client methods
	
	public MigrationTypeList getPrimaryTypes() throws SynapseException {
		return getJSONEntity(getRepoEndpoint(), MIGRATION_PRIMARY, MigrationTypeList.class);
	}

	public MigrationTypeNames getPrimaryTypeNames() throws SynapseException {
		return getJSONEntity(getRepoEndpoint(), MIGRATION_PRIMARY_NAMES, MigrationTypeNames.class);
	}

	public MigrationTypeList getMigrationTypes() throws SynapseException {
		return getJSONEntity(getRepoEndpoint(), MIGRATION_TYPES, MigrationTypeList.class);
	}

	public MigrationTypeNames getMigrationTypeNames() throws SynapseException {
		return getJSONEntity(getRepoEndpoint(), MIGRATION_TYPE_NAMES, MigrationTypeNames.class);
	}

	public MigrationTypeCounts getTypeCounts() throws SynapseException {
		return getJSONEntity(getRepoEndpoint(), MIGRATION_COUNTS, MigrationTypeCounts.class);
	}
	
	public MigrationTypeCount getTypeCount(MigrationType type) throws SynapseException {
		String uri = MIGRATION_COUNT + "?type=" + type.name() ;
		return getJSONEntity(getRepoEndpoint(), uri, MigrationTypeCount.class);
	}
	
	public MigrationTypeCount deleteMigratableObject(MigrationType migrationType, IdList ids) throws SynapseException {
		String uri = MIGRATION_DELETE + "?type=" + migrationType.name();
		return putJSONEntity(getRepoEndpoint(), uri, ids, MigrationTypeCount.class);
	}

	@Override
	public MigrationRangeChecksum getChecksumForIdRange(MigrationType migrationType, String salt, Long minId, Long maxId) throws SynapseException {
		if (migrationType == null || minId == null || maxId == null) {
			throw new IllegalArgumentException("Arguments type, minId and maxId cannot be null");
		}
		String uri = MIGRATION_RANGE_CHECKSUM + "?migrationType=" + migrationType.name() + "&salt=" + salt + "&minId=" + minId + "&maxId=" + maxId;
		return getJSONEntity(getRepoEndpoint(), uri,  MigrationRangeChecksum.class);
	}
	
	@Override
	public MigrationTypeChecksum getChecksumForType(MigrationType migrationType) throws SynapseException {
		if (migrationType == null) {
			throw new IllegalArgumentException("Arguments type cannot be null");
		}
		String uri = MIGRATION_TYPE_CHECKSUM + "?migrationType=" + migrationType.name();
		return getJSONEntity(getRepoEndpoint(), uri,  MigrationTypeChecksum.class);
	}
	
	@Override
	public FireMessagesResult fireChangeMessages(Long startChangeNumber, Long limit) throws SynapseException {
		if(startChangeNumber == null) throw new IllegalArgumentException("startChangeNumber cannot be null");
		String uri = ADMIN_FIRE_MESSAGES + "?startChangeNumber=" + startChangeNumber;
		if (limit != null){
			uri = uri + "&limit=" + limit;
		}
		return getJSONEntity(getRepoEndpoint(), uri, FireMessagesResult.class);
	}
	
	/**
	 * Return current change message number
	 */
	@Override
	public FireMessagesResult getCurrentChangeNumber() throws SynapseException {
		String uri = ADMIN_GET_CURRENT_CHANGE_NUM;
		return getJSONEntity(getRepoEndpoint(), uri, FireMessagesResult.class);
	}

	/**
	 * Build up the URL for the list change message call.
	 * @param startChangeNumber
	 * @param type
	 * @param limit
	 * @return
	 */
	static String buildListMessagesURL(Long startChangeNumber, ObjectType type, Long limit){
		if(startChangeNumber == null) throw new IllegalArgumentException("startChangeNumber cannot be null");
		StringBuilder builder = new StringBuilder();
		builder.append(ADMIN_CHANGE_MESSAGES);
		builder.append("?");
		builder.append("startChangeNumber=").append(startChangeNumber);
		if(type != null){
			builder.append("&type=").append(type.name());
		}
		if(limit != null){
			builder.append("&limit=").append(limit);
		}
		return builder.toString();
	}
	
	@Override
	public PublishResults publishChangeMessages(String queueName, Long startChangeNumber, ObjectType type, Long limit) throws SynapseException{
		// Build up the URL
		String url = buildPublishMessagesURL(queueName, startChangeNumber, type, limit);
		return postJSONEntity(getRepoEndpoint(), url, null, PublishResults.class);
	}
	
	/**
	 * Build up the URL for publishing messages.
	 * @param startChangeNumber
	 * @param type
	 * @param limit
	 * @return
	 */
	static String buildPublishMessagesURL(String queueName, Long startChangeNumber, ObjectType type, Long limit){
		if(queueName == null) throw new IllegalArgumentException("queueName cannot be null");
		if(startChangeNumber == null) throw new IllegalArgumentException("startChangeNumber cannot be null");
		StringBuilder builder = new StringBuilder();
		builder.append(ADMIN_PUBLISH_MESSAGES);
		builder.append("?");
		builder.append("queueName=").append(queueName);
		builder.append("&startChangeNumber=").append(startChangeNumber);
		if(type != null){
			builder.append("&type=").append(type.name());
		}
		if(limit != null){
			builder.append("&limit=").append(limit);
		}
		return builder.toString();
	}

	@Override
	public void clearDoi() throws SynapseException {
		deleteUri(getRepoEndpoint(), ADMIN_DOI_CLEAR);
	}

	@Override
	public void clearDynamoTable(String tableName, String hashKeyName, String rangeKeyName)
			throws SynapseException {

		if (tableName == null || tableName.isEmpty()) {
			throw new IllegalArgumentException("Table name cannot be null or empty.");
		}
		if (hashKeyName == null || hashKeyName.isEmpty()) {
			throw new IllegalArgumentException("Hash key name cannot be null or empty.");
		}
		if (rangeKeyName == null || rangeKeyName.isEmpty()) {
			throw new IllegalArgumentException("Range key name cannot be null or empty.");
		}

		try {
			String uri = ADMIN_DYNAMO_CLEAR + "/" + URLEncoder.encode(tableName, "UTF-8");
			uri += "?hashKeyName=" + URLEncoder.encode(hashKeyName, "UTF-8");
			uri += "&rangeKeyName=" + URLEncoder.encode(rangeKeyName, "UTF-8");
			super.deleteUri(getRepoEndpoint(), uri);
		} catch (UnsupportedEncodingException e) {
			throw new SynapseClientException(e);
		}
	}

	@Override
	public long createUser(NewIntegrationTestUser user) throws SynapseException {
		if(user.getEmail() == null) throw new IllegalArgumentException("New users must have an email");
		if(user.getUsername() == null) throw new IllegalArgumentException("New users must have a username");
		EntityId id = postJSONEntity(getRepoEndpoint(), ADMIN_USER, user, EntityId.class);
		return Long.parseLong(id.getId());
	}


	@Override
	public void deleteUser(Long id) throws SynapseException {
		String url = ADMIN_USER + "/" + id; 
		deleteUri(getRepoEndpoint(), url);
	}

	@Override
	public void rebuildTableCacheAndIndex(String tableId) throws SynapseException {
		String url = ADMIN + "/entity/" + tableId + "/table/rebuild";
		getStringDirect(getRepoEndpoint(), url);
	}

	@Override
	public void clearAllLocks() throws SynapseException{
		deleteUri(getRepoEndpoint(), ADMIN_CLEAR_LOCKS);
	}

	@Override
	public ChangeMessages createOrUpdateChangeMessages(ChangeMessages batch) throws SynapseException {
		return postJSONEntity(getRepoEndpoint(), ADMIN_CREATE_OR_UPDATE_CHANGE_MESSAGES, batch, ChangeMessages.class);
	}

	@Override
	public AsynchronousJobStatus startAdminAsynchronousJob(
			AsyncMigrationRequest migReq) throws SynapseException {
		if (migReq == null)
			throw new IllegalArgumentException("JobBody cannot be null");
		String url = ADMIN_ASYNCHRONOUS_JOB;
		return postJSONEntity(getRepoEndpoint(), url, migReq,
				AsynchronousJobStatus.class);
	}

	@Override
	public AsynchronousJobStatus getAdminAsynchronousJobStatus(String jobId)
			throws SynapseException {
		ValidateArgument.required(jobId, "jobId");
		String url = ADMIN_ASYNCHRONOUS_JOB + "/" + jobId;
		return getJSONEntity(getRepoEndpoint(), url, AsynchronousJobStatus.class);
	}

	@Override
	public IdGeneratorExport createIdGeneratorExport() throws SynapseException {
		return getJSONEntity(getRepoEndpoint(), ADMIN_ID_GEN_EXPORT, IdGeneratorExport.class);
	}

	/**
	 * Add a contributor to an existing submission.  This is available to Synapse administrators only.
	 * @param submissionId
	 * @param contributor
	 * @return
	 */
	@Override
	public SubmissionContributor addSubmissionContributor(String submissionId, SubmissionContributor contributor)
			throws SynapseException {
		validateStringAsLong(submissionId);
		String uri = ADMIN + EVALUATION_URI_PATH + "/" + SUBMISSION + "/" + submissionId + "/contributor";
		return postJSONEntity(getRepoEndpoint(), uri, contributor, SubmissionContributor.class);
	}

	@Override
	public PaginatedResults<QuizResponse> getCertifiedUserTestResponses(
			long offset, long limit, String principalId)
			throws SynapseException {

		String uri = null;
		if (principalId == null) {
			uri = ADMIN + CERTIFIED_USER_TEST_RESPONSE + "?" + OFFSET + "=" + offset
					+ "&" + LIMIT + "=" + limit;
		} else {
			uri = ADMIN + CERTIFIED_USER_TEST_RESPONSE + "?"
					+ PRINCIPAL_ID_REQUEST_PARAM + "=" + principalId + "&"
					+ OFFSET + "=" + offset + "&" + LIMIT + "=" + limit;
		}
		return getPaginatedResults(getRepoEndpoint(), uri, QuizResponse.class);
	}

	@Override
	public void setCertifiedUserStatus(String principalId, boolean status)
			throws SynapseException {
		String url = ADMIN + USER + "/" + principalId + CERTIFIED_USER_STATUS
				+ "?isCertified=" + status;
		voidPut(getRepoEndpoint(), url, null);
	}

	/**
	 * Deletes a message.  Used for test cleanup only.  Admin only.
	 */
	@Override
	public void deleteMessage(String messageId) throws SynapseException {
		String uri = ADMIN + MESSAGE + "/" + messageId;
		deleteUri(getRepoEndpoint(), uri);
	}


	private static final void validateStringAsLong(String s) throws SynapseClientException {
		if (s==null) throw new NullPointerException();
		try {
			Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new SynapseClientException("Expected integer but found "+s, e);
		}
	}

	@Override
	public PaginatedResults<PassingRecord> getCertifiedUserPassingRecords(
			long offset, long limit, String principalId)
			throws SynapseException {
		ValidateArgument.required(principalId, "principalId");
		String uri = ADMIN + USER + "/" + principalId + CERTIFIED_USER_PASSING_RECORDS
				+ "?" + OFFSET + "=" + offset + "&" + LIMIT + "=" + limit;
		return getPaginatedResults(getRepoEndpoint(), uri, PassingRecord.class);
	}
	
	@Override
	public void deleteCertifiedUserTestResponse(String id)
			throws SynapseException {
		deleteUri(getRepoEndpoint(), ADMIN + CERTIFIED_USER_TEST_RESPONSE + "/" + id);
	}
	
	@Override
	public OAuthClient updateOAuthClientVerifiedStatus(String clientId, String etag, boolean status) throws SynapseException {
		validateStringAsLong(clientId);
		String uri = ADMIN + OAUTH_CLIENT + "/" + clientId + VERIFIED + "?status=" + status + "&etag=" + etag;
		return putJSONEntity(getRepoEndpoint(), uri, null, OAuthClient.class);
	}

	@Override
	public void redactUserInformation(String principalId) throws SynapseException {
		validateStringAsLong(principalId);
		String uri = ADMIN + REDACT_USER + "/" + principalId;
		voidPost(getRepoEndpoint(), uri, null, null);
	}
	
	@Override
	public FeatureStatus getFeatureStatus(Feature feature) throws SynapseException {
		String uri = ADMIN + "/feature/" + feature.name() + "/status";
		return getJSONEntity(getRepoEndpoint(), uri, FeatureStatus.class);
	}
	
	@Override
	public FeatureStatus setFeatureStatus(Feature feature, FeatureStatus status) throws SynapseException {
		String uri = ADMIN + "/feature/" + feature.name() + "/status";
		return postJSONEntity(getRepoEndpoint(), uri, status, FeatureStatus.class);
	}
}
