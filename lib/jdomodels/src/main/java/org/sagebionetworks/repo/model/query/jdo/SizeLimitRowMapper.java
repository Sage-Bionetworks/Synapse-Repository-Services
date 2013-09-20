package org.sagebionetworks.repo.model.query.jdo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import org.springframework.jdbc.core.ColumnMapRowMapper;

/**
 * This RowMapper will throw an exception if the maximum number of bytes is exceeded while processing the a
 * result set. By throwing while reading the result set we minimize the amount of data read from the database
 * when the limit is exceeded.
 * 
 * @author jmhill
 *
 */
public class SizeLimitRowMapper extends ColumnMapRowMapper{
	
	public static final int BYTES_PER_CHAR = Character.SIZE/8;
	public static final int BYTES_PER_LONG = Long.SIZE/8;
	public static final int BYTES_PER_DOUBLE = Double.SIZE/8;
	public static final int BYTES_PER_INTEGER = Integer.SIZE/8;
	
	private long maxBytes;
	private long bytesUsed;


	/**
	 * Provide the maximum number of bytes allowed the query results.
	 * @param maxSize
	 */
	public SizeLimitRowMapper(long maxSize){
		this.maxBytes = maxSize;
	}

	@Override
	public Map<String, Object> mapRow(ResultSet rs, int rowNum)	throws SQLException {
		Map<String, Object> rowMap = super.mapRow(rs, rowNum);
		// Determine the size of this row.
		return checkSize(rowMap);
	}

	/**
	 * This is where we actually check the size
	 * @param rowMap
	 * @return
	 */
	Map<String, Object> checkSize(Map<String, Object> rowMap) {
		bytesUsed += getRowSizeBytes(rowMap);
		if(bytesUsed > maxBytes){
			throw new IllegalArgumentException("The results of this query exceeded the maximum number of allowable bytes: "+maxBytes+".  Please try the query again with a smaller page size or limit the columns returned in the select clause.");
		}
		return rowMap;
	}
	
	/**
	 * How many bytes were used for this query?
	 * @return
	 */
	public long getBytesUsed(){
		return bytesUsed;
	}
	
	/**
	 * Count the bytes used by this map.
	 * @param rowMap
	 * @return
	 */
	public static long getRowSizeBytes(Map<String, Object> rowMap){
		long bytesUsed = 0;
		Iterator it = rowMap.values().iterator();
		while(it.hasNext()){
			Object value = it.next();
			if(value == null) continue;
			if(value instanceof String){
				String sValue = (String) value;
				bytesUsed += sValue.length()*BYTES_PER_CHAR;
			}else if(value instanceof Long){
				bytesUsed += BYTES_PER_LONG;
			}else if(value instanceof Integer){
				bytesUsed += BYTES_PER_INTEGER;
			}else if(value instanceof Double){
				bytesUsed += BYTES_PER_DOUBLE;
			}else if(value instanceof byte[]){
				byte[] bytes = (byte[]) value;
				bytesUsed += bytes.length;
			}else{
				throw new IllegalArgumentException("Unknown value type: "+value.getClass().getName());
			}
		}
		return bytesUsed;
	}

}
