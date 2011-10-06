package profiler.org.sagebionetworks.cloudwatch; 

//interface for Observer portion of the the Observable pattern
public interface Watcher {
	
	public void update(String message);
}
