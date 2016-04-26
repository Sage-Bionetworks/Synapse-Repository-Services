package org.sagebionetworks.repo.manager.asynch;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Hex;
import org.sagebionetworks.repo.manager.table.TableEntityManagerImpl;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.asynch.CacheableRequestBody;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.util.Md5Utils;

public class JobHashProviderImpl implements JobHashProvider {
	
	private static final String NULL = "NULL";
	@Autowired
	TableManagerSupport tableManagerSupport;

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
			byte[] md5Bytes = Md5Utils.computeMD5Hash(builder.toString().getBytes("UTF-8"));
			return new String(Hex.encodeHex(md5Bytes));
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
	 * See: {@link #getTableEtag(String)}
	 * @param body
	 * @return
	 */
	private String getTableEtag(QueryNextPageToken body){
		Query query = TableEntityManagerImpl.createQueryFromNextPageToken(body);
		String tableId = extractTableIdFromSql(query.getSql());
		return getTableEtag(tableId);
	}
	
	/**
	 * See: {@link #getTableEtag(String)}
	 * @param body
	 * @return
	 */
	private String getTableEtag(QueryBundleRequest body){
		if(body.getQuery() == null){
			throw new IllegalArgumentException("QueryBundleRequest.query cannot be null");
		}
		String tableId = extractTableIdFromSql(body.getQuery().getSql());
		return getTableEtag(tableId);
	}
	
	/**
	 * See: {@link #getTableEtag(String)}
	 * @param body
	 * @return
	 */
	private String getTableEtag(DownloadFromTableRequest body){
		String tableId = extractTableIdFromSql(body.getSql());
		return getTableEtag(tableId);
	}
	
	/**
	 * Helper to determine the tableId from the SQL.
	 * @param sql
	 * @return
	 */
	private String extractTableIdFromSql(String sql){
		try {
			return new TableQueryParser(sql).querySpecification().getTableExpression().getFromClause().getTableReference().getTableName();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
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
	private String getTableEtag(String tableId){
		// Base the etag on the table status
		TableStatus status = tableManagerSupport.getTableStatusOrCreateIfNotExists(tableId);
		return status.getLastTableChangeEtag() + status.getResetToken();
	}
}
