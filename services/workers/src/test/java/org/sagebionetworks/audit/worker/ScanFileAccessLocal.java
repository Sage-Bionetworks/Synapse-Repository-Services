package org.sagebionetworks.audit.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.aws.utils.s3.ObjectCSVReader;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.tool.progress.BasicProgress;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * This is a utility that will scan through local access record CSV files and generate a report of users
 * that have accessed those files.
 * 
 * In the past we just used 'grep' for this but it stopped working once there were too many files to scan through.
 * 
 * @author John
 *
 */
public class ScanFileAccessLocal {

	/**
	 * This is the schema. If it changes we will not be able to read old data.
	 */
	private final static String[] HEADERS = new String[]{"returnObjectId", "elapseMS","timestamp","via","host","threadId","userAgent","queryString","sessionId","xForwardedFor","requestURL","userId","origin", "date","method","vmId","instance","stack","success", "responseStatus"};

	static private Log log = LogFactory.getLog(ScanFileAccessLocal.class);
	
	public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
		if(args == null || args.length < 2){
			throw new IllegalArgumentException("The first argument must be the full path to the directory containing the audit record CSV or gz files.");
		}
		File path = new File(args[0]);
		if(!path.exists()){
			throw new IllegalArgumentException("Path does not exist: "+path.getAbsolutePath());
		}
		File out = new File(args[1]);
		if(out.exists()){
			out.delete();
		}
		out.createNewFile();
		// the ID of the files to gather the information for.
		Set<Long> fileIds = new HashSet<Long>(args.length-1);
		for(int i=2; i<args.length; i++){
			fileIds.add(KeyFactory.stringToKey(args[i]));
		}
		log.info("Collecting file metadata...");
		Collection<File> list = FileUtils.listFiles(path, new String[] {"csv", "CSV"}, true);
		log.info("Number of Files found: "+list.size());
		ExecutorService executor = Executors.newFixedThreadPool(1);
		BasicProgress progress = new BasicProgress();
		Future<Map<String, Set<Long>>> future = executor.submit(new Worker(list,fileIds,progress ));
		// Wait for it to do its thing.
		while(!future.isDone()){
			log.info("Scanning files: "+progress.getCurrentStatus().toString());
			Thread.sleep(1000);
		}
		log.info("All files scanned: "+progress.getCurrentStatus().toString());
		Map<String, Set<Long>> results = future.get();
		// Write out the results
		log.info("Users that accessed each file: ");
		CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(out)));
		try{
			for(String key: results.keySet()){
				Set<Long> users = results.get(key);
				for(Long userId: users){
					log.info(key+","+userId);
					writer.writeNext(new String[]{key, userId.toString()});
				}
			}
		}finally{
			writer.close();
		}

	}

	private static class Worker implements Callable<Map<String, Set<Long>>>{
		Collection<File> toScan;
		Set<FileEntityMatcher> fileUrlFragments;
		BasicProgress progress;
		/**
		 * Create a new worker.
		 * @param toScan
		 * @param progress
		 */
		public Worker(Collection<File> toScan, Set<Long> fileIds, BasicProgress progress) {
			super();
			this.toScan = toScan;
			this.fileUrlFragments = new HashSet<FileEntityMatcher>(fileIds.size());
			this.progress = progress;
			// the total size is the sum of the file sizes
			long totalSize = 0l;
			for(File file: toScan){
				totalSize += file.length();
			}
			this.progress.setTotal(totalSize);
			this.progress.setCurrent(0);
			for(Long id: fileIds){
				fileUrlFragments.add(new FileEntityMatcher(id));
			}
		}

		@Override
		public Map<String, Set<Long>> call() throws Exception {
			Map<String, Set<Long>> map = new HashMap<String, Set<Long>>();
			long readBytes = 0;
			for(File file: toScan){
				// Read the file
				InputStreamReader readerStream = new InputStreamReader(new FileInputStream(file), "UTF-8");
				try{
					ObjectCSVReader<AccessRecord> reader = new ObjectCSVReader<AccessRecord>(
							readerStream, AccessRecord.class, HEADERS);
					AccessRecord ar = reader.next();
					do {
						if(ar != null){
							// Check for a match to each file
							for(FileEntityMatcher matcher: fileUrlFragments){
								if(ar.getUserId() != null){
									if(matcher.matches(ar.getRequestURL())){
										Set<Long> users = map.get(ar.getRequestURL());
										if(users == null){
											users = new HashSet<Long>(5);
											map.put(ar.getRequestURL(), users);
										}
										if(users.add(ar.getUserId())){
											log.info("New user found: "+ar.getUserId()+" "+ar.toString());
										};
									}
								}
							}
						}
						try {
							ar = reader.next();
						} catch (Exception e) {
							log.error("Failed to read file: "+file.getAbsolutePath(), e);
						}
					} while (ar!= null);
				}finally{
					readerStream.close();
				}
				readBytes += file.length();
				progress.setCurrent(readBytes);
				if(Thread.currentThread().isInterrupted()){
					throw new InterruptedException();
				}
				Thread.yield();
			}
			return map;
		}
	}

}
