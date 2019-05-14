package org.sagebionetworks.repo.manager.asynch;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Hex;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.asynch.CacheableRequestBody;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.util.Md5Utils;

public class JobHashProviderImpl implements JobHashProvider {

	private static final String NULL = "NULL";
	@Autowired
	TableManagerSupport tableManagerSupport;

	@Override
	public String getJobHash(CacheableRequestBody body) {
		if (body == null) {
			throw new IllegalArgumentException("Body cannot be null");
		}
		try {
			// Extract the tableId from the request body.
			String tableId = TableQueryUtils.getTableIdFromRequestBody(body);
			IdAndVersion idAndVersion = IdAndVersion.parse(tableId);

			/*
			 * Since view query results can vary with permission changes on
			 * entities within a view's scope, the view results cannot be
			 * cached. Returning a null job hash will prevent caching of view
			 * results. See PLFM-4231.
			 */
			ObjectType type = tableManagerSupport.getTableType(idAndVersion);
			if (ObjectType.ENTITY_VIEW.equals(type)) {
				return null;
			}
			
			StringBuilder builder = new StringBuilder();
			builder.append(EntityFactory.createJSONStringForEntity(body));
			String objectEtag = getTableEtag(tableId);
			if (objectEtag != null) {
				builder.append(objectEtag);
			} else {
				builder.append(NULL);
			}
			byte[] md5Bytes = Md5Utils.computeMD5Hash(builder.toString()
					.getBytes("UTF-8"));
			return new String(Hex.encodeHex(md5Bytes));
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Get an Etag for a table. The etag used here is the concatenation of:
	 * TableStatus.lastTableChangeEtag + TableStatus.resetToken This ensure any
	 * change to the table or its status will produce a different etag.
	 * 
	 * @param body
	 * @return
	 */
	private String getTableEtag(String tableId) {
		// Base the etag on the table status
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		TableStatus status = tableManagerSupport
				.getTableStatusOrCreateIfNotExists(idAndVersion);
		return status.getLastTableChangeEtag() + status.getResetToken();
	}
}
