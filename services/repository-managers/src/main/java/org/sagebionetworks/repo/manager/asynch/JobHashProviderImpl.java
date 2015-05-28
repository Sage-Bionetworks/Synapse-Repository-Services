package org.sagebionetworks.repo.manager.asynch;

import java.io.UnsupportedEncodingException;

import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.CacheableRequestBody;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.util.Md5Utils;

public class JobHashProviderImpl implements JobHashProvider {
	
	private static final String NULL = "NULL";
	@Autowired
	TableRowTruthDAO tableRowTruthDao;

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
		// For now we only provide etags for tables. We can extend this if needed.
		String tableId = null;
		if(body instanceof DownloadFromTableRequest){
			tableId = ((DownloadFromTableRequest)body).getEntityId();
		}else if(body instanceof QueryBundleRequest){
			tableId = ((QueryBundleRequest)body).getEntityId();
		}else if(body instanceof QueryNextPageToken){
			tableId = ((QueryNextPageToken)body).getEntityId();
		}else{
			throw new IllegalArgumentException("Unknown request body type: "+body.getClass());
		}
		if(tableId != null){
			TableRowChange change = tableRowTruthDao.getLastTableRowChange(tableId);
			if(change != null){
				return change.getEtag();
			}
		}
		return null;
	}

}
