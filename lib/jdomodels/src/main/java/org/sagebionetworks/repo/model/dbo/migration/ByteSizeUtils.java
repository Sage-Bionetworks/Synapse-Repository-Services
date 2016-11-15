package org.sagebionetworks.repo.model.dbo.migration;

import java.lang.reflect.Method;
import java.sql.Types;

import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;

/**
* Estimate the size of a BeanPropertySqlParameterSource object in bytes.
 * @author John
 *
 */
public class ByteSizeUtils {
	
	/**
	 * Estimate the size of a BeanPropertySqlParameterSource object in bytes.
	 * @param object
	 * @return
	 */
	public static int estimateSizeInBytes(BeanPropertySqlParameterSource object){
		if(object == null) return 0;
		int size = 0;
		String[] names = object.getReadablePropertyNames();
		for(String name: names){
			// Get the value
			Object value = object.getValue(name);
			if(value != null){
				int type = object.getSqlType(name);
				if(Types.VARCHAR == type){
					String string = (String) value;
					size += string.length()*Character.SIZE;
				}else if(Types.BIGINT == type){
					size += Long.SIZE;
				}else if(Types.TIMESTAMP == type){
					size += Long.SIZE;
				}else if(Types.DOUBLE == type){
					size += Double.SIZE;
				}else if(Types.SMALLINT == type){
					size += Integer.SIZE;
				}else if(Types.INTEGER == type){
					size += Integer.SIZE;
				}else if(Types.BOOLEAN == type){
						size += 1;
				}else if(Integer.MIN_VALUE == type){
					// Need to use reflection for this case
					if(value instanceof byte[]){
						byte[] array = (byte[]) value;
						size += array.length;
					}else if(value instanceof Class){
						// Skip the class
					}else if(value.getClass().isEnum()){
						try {
							Method nameMethod = value.getClass().getMethod("name");
							String enumName = (String) nameMethod.invoke(value, null);
							size += enumName.length()*Character.SIZE;
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}else if(value instanceof Boolean){
						// better to over estimate so assume one byte.
						size += 1;
					}else{
//						throw new IllegalArgumentException("Unknown class: "+value.getClass());
						//skip unknown types
					}
				}else{
					throw new IllegalArgumentException("Unknown SQL type: "+type+" for name: "+name+" value class: "+value.getClass());
				}
			}
		}
		return size;
	}
}
