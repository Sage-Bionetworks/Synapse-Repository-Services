package org.sagebionetworks.repo.manager.file.preview;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PreviewGeneratorUtils {

	public static final float ONE_MEGA_BYTE = (float) Math.pow(2, 20.0);
	
	/**
	 * @param toRead
	 * @param generatorClass PreviewGenerator class
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws InterruptedException
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static void calculateMemoryRequirments(File toRead, Class<? extends PreviewGenerator> generatorClass) throws IOException,
			FileNotFoundException, InterruptedException, InstantiationException, IllegalAccessException {
		double startFreeMB = freeMegaBytes();
		File tempOut = File.createTempFile(generatorClass.getName(), "tmp");
		FileOutputStream fos = new FileOutputStream(tempOut);
		FileInputStream fis = new FileInputStream(toRead);
		try{
			PreviewGenerator gen = generatorClass.newInstance();
			gen.generatePreview(fis, fos);
			double peakFreeMB = freeMegaBytes();
			// see how much we can reclaim
			Runtime.getRuntime().gc();
			Thread.sleep(2000);
			double endFreeMB = freeMegaBytes();
			double peakUseMB = startFreeMB-peakFreeMB;
			double fileSizeMB = ((float)toRead.length())/ONE_MEGA_BYTE;
			double memoryMultiple = peakUseMB/fileSizeMB;
			System.out.println(toRead.getName()+" data:");
			System.out.println(String.format("\tFile size: %1$.2f MB, Peak memory usage: %2$.2f MB, Start free: %3$.2f MB, Peak free: %4$.2f MB, End free: %5$.2f MB, Memory used: %6$.2f x fileSize", fileSizeMB, peakUseMB,startFreeMB, peakFreeMB, endFreeMB, memoryMultiple));
		}finally{
			fis.close();
			fos.close();
			tempOut.delete();
		}
	}

	/**
	 * @return
	 */
	public static float freeMegaBytes() {
		return ((float)Runtime.getRuntime().freeMemory())/ONE_MEGA_BYTE;
	}

	public static String findExtension(String name) {
		if (name != null) {
			int lastDot = name.lastIndexOf(".");
			// ignore also if first character is .
			if (lastDot > 0 && lastDot < name.length() - 1) {
				return name.substring(lastDot + 1).toLowerCase();
			}
		}
		return "noextension";
	}
}
