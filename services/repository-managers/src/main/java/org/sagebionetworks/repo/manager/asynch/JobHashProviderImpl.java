package org.sagebionetworks.repo.manager.asynch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.asynch.CacheableRequestBody;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.HasEntityId;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.util.Md5Utils;

public class JobHashProviderImpl implements JobHashProvider {
	
	private static final String NULL = "NULL";
	@Autowired
	TableRowManager tableRowManager;

	@Override
	public String getJobHash(CacheableRequestBody body) {
		if(body == null){
			throw new IllegalArgumentException("Body cannot be null");
		}
		try {
			StringBuilder builder = new StringBuilder();
			builder.append(EntityFactory.createJSONStringForEntity(body));
			String objectEtag = getRequestObjectEtag(body);
			if(objectEtag != null){
				builder.append(objectEtag);
			}else{
				builder.append(NULL);
			}
			return Md5Utils.md5AsBase64(builder.toString().getBytes("UTF-8"));
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private String getRequestObjectEtag(CacheableRequestBody body) {
		if(body == null){
			throw new IllegalArgumentException("Body cannot be null");
		}
		// Etag lookup is dependent on the type of request body provided.
		if(body instanceof DownloadFromTableRequest){
			return getTableEtag((DownloadFromTableRequest)body);
		}else if(body instanceof QueryBundleRequest){
			return getTableEtag((QueryBundleRequest)body);
		}else if(body instanceof QueryNextPageToken){
			return getTableEtag((QueryNextPageToken)body);
		}else{
			throw new IllegalArgumentException("Unknown request body type: "+body.getClass());
		}
	}

	/**
	 * Get an Etag for a table. The etag used here is the concatenation of:
	 * TableStatus.lastTableChangeEtag + TableStatus.resetToken
	 * This ensure any change to the table or its status will produce a different etag.
	 * 
	 * @param body
	 * @return
	 */
	private String getTableEtag(HasEntityId body){
		// Base the etag on the table status
		try {
			TableStatus status = tableRowManager.getTableStatusOrCreateIfNotExists(body.getEntityId());
			return status.getLastTableChangeEtag() + status.getResetToken();
		}  catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
