package org.sagebionetworks.tool.migration.v4;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Iterator;

/**
 * Simple iterator for DataInputStream.readLong()
 * 
 * @author John
 *
 */
public class DataInputStreamIterator implements Iterator<Long>{
	
	private DataInputStream dis;
	private boolean done = false;

	public DataInputStreamIterator(DataInputStream dis) {
		super();
		this.dis = dis;
	}
	
	@Override
	public boolean hasNext() {
		return !done;
	}

	@Override
	public Long next() {
		try{
			return new Long(dis.readLong());
		}catch(EOFException e){
			this.done = true;
			return null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Not supported");
	}

}
