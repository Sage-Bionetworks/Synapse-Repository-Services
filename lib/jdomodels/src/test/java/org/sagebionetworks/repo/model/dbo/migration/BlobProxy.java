package org.sagebionetworks.repo.model.dbo.migration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Blob;

public class BlobProxy implements InvocationHandler {
	
	byte[] targetBytes;

	BlobProxy(byte[] targetBytes) {
		this.targetBytes = targetBytes;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if("length".equals(method.getName())) {
			return new Long(targetBytes.length);
		}else if("getBytes".equals(method.getName())) {
			return targetBytes;
		}
		throw new IllegalArgumentException("Unsupported method: "+method.getName());
	}
	
	/**
	 * Create a Blob proxy wrapping the target byte array.
	 * @param targetBytes
	 * @return
	 */
	public static Blob createProxy(final byte[] targetBytes) {
		return (Blob) Proxy.newProxyInstance(ResultSetProxy.class.getClassLoader(),
				new Class[] { Blob.class }, new BlobProxy(targetBytes));
	}

}
