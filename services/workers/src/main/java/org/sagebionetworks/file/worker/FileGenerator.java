package org.sagebionetworks.file.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * A little utility for generating large file to test upload.
 * @author John
 *
 */
public class FileGenerator {
	
	private static long BYTES_PER_MB = 1048576;
	
	public static void main(String[] args) throws IOException{
		Random rand = new Random();
		File file = new File(args[0]);
		long sizeMB = Long.parseLong(args[1]);
		if(file.exists()){
			file.delete();
		}
		file.createNewFile();
		byte[] buffer = new byte[1024];
		FileOutputStream fos = new FileOutputStream(file);
		try{
			long countBytes = 0;
			double mbWritten = 0;
			while(mbWritten < sizeMB){
				// fill the buffer with random bytes
				rand.nextBytes(buffer);
				fos.write(buffer);
				countBytes += buffer.length;
				mbWritten = countBytes/BYTES_PER_MB;
				System.out.println("mbs written: "+mbWritten);
			}
		}finally{
			fos.close();
		}
	}

}
