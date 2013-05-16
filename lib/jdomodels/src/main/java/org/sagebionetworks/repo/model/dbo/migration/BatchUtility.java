package org.sagebionetworks.repo.model.dbo.migration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * Utility for doing batch updates.
 * 
 * @author John
 * 
 */
public class BatchUtility {

	/**
	 * The max_allowed_packet database property limits the size of a single
	 * database call. This utility will attempt to stay under the limit by
	 * breaking a single batch into multiple batches as need.
	 * 
	 * @param batch
	 *            - Batch of parameters
	 * @param maxAllowedPacketBytes
	 *            - Should be less than or equal to max_allowed_packet
	 * @return
	 */
	public static <D> List<SqlParameterSource[]> prepareBatches(List<D> batch,	int maxAllowedPacketBytes) {
		if (batch == null) throw new IllegalArgumentException("Batch cannot be null");
		// nothing to do with an empty batch
		List<SqlParameterSource[]> results = new LinkedList<SqlParameterSource[]>();
		List<BeanPropertySqlParameterSource> batchParam = new ArrayList<BeanPropertySqlParameterSource>();
		int currentSizeInBytes = 0;
		for (int i = 0; i < batch.size(); i++) {
			BeanPropertySqlParameterSource param = new BeanPropertySqlParameterSource(batch.get(i));
			int paramSizeBytes = ByteSizeUtils.estimateSizeInBytes(param);
			if (currentSizeInBytes + paramSizeBytes > maxAllowedPacketBytes) {
				// Add this to the batch
				if (!batchParam.isEmpty()) {
					results.add(batchParam.toArray(new BeanPropertySqlParameterSource[batchParam.size()]));
					// Clear and reset
					batchParam.clear();
					currentSizeInBytes = paramSizeBytes;
					batchParam.add(param);
				}else{
					// If this object is over and the batch is empty just send this object
					results.add(new SqlParameterSource[]{param});
					currentSizeInBytes = 0;
				}
			} else {
				// Add it to the batch
				batchParam.add(param);
				currentSizeInBytes += paramSizeBytes;
			}
		}
		// If there are any parameters left send them
		if (!batchParam.isEmpty()) {
			results.add(batchParam.toArray(new BeanPropertySqlParameterSource[batchParam.size()]));
		}
		return results;
	}
}
