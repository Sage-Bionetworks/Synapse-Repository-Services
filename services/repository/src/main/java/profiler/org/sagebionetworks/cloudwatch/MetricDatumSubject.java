package profiler.org.sagebionetworks.cloudwatch;

//class used to implement the Subject portion of the Observer pattern
//class collects and holds strings representing information
/**
 * 
 */
public interface MetricDatumSubject {
	public void registerWatcher(Watcher w);
	public void removeWatcher(Watcher w);
	public void notifyWatchers(String message);
}
