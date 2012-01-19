package org.sagebionetworks.tool.migration.gui.presenter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.tool.migration.AllEntityDataWorker;
import org.sagebionetworks.tool.migration.ClientFactoryImpl;
import org.sagebionetworks.tool.migration.Configuration;
import org.sagebionetworks.tool.migration.MigrationDriver;
import org.sagebionetworks.tool.migration.ResponseBundle;
import org.sagebionetworks.tool.migration.SynapseConnectionInfo;
import org.sagebionetworks.tool.migration.Progress.AggregateProgress;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.dao.QueryRunner;
import org.sagebionetworks.tool.migration.dao.QueryRunnerImpl;
import org.sagebionetworks.tool.migration.job.AggregateResult;
import org.sagebionetworks.tool.migration.job.BuilderResponse;
import org.sagebionetworks.tool.migration.job.Job;

public class MigrationControlPresenter {
	
	private static final String WAITING_TO_START = "Waiting to start...";
	static private Log log = LogFactory.getLog(MigrationControlPresenter.class);

	public static interface View {

		/**
		 * Show an error message.
		 * 
		 * @param e
		 */
		public void showErrorMessage(Exception e);
		
		/**
		 * Listener for the start button.
		 * @param listener
		 */
		public void addStartListener(ActionListener listener);
		
		/**
		 * Set the start enabled.
		 * @param enabled
		 */
		public void setStartEnabled(boolean enabled);
		
		/**
		 * Add a stop listener
		 * @param listener
		 */
		public void addStopListener(ActionListener listener);
		
		/**
		 * Set the stop enabled.
		 * @param enabled
		 */
		public void setStopEnabled(boolean enabled);

		/**
		 * Reset the source 
		 * @param i
		 * @param j
		 * @param string
		 */
		public void setSourceCollectProgress(int value, int total, String message);

		/**
		 * Reset the destination.
		 * @param min
		 * @param max
		 * @param string
		 */
		public void setDestinationCollectProgress(int value, int total, String message);

		/**
		 * Set the consuming progress
		 * @param current
		 * @param total
		 * @param string
		 */
		public void setConsumingProgress(int value, int total, String message);
		
		/**
		 * Reset all progress bars.
		 * @param message
		 */
		public void resetAllProgress(String message);

		/**
		 * Set the creation total.
		 * @param value
		 * @param total
		 * @param message
		 */
		public void setCreationTotalProgress(int value, int total, String message);
		

	}

	private View view;
	/**
	 * The thread that actually runs migration.
	 */
	private Thread migrationThread; 
	private int poolSize;
	private SynapseConnectionInfo sourceInfo;
	private SynapseConnectionInfo destInfo;

	public MigrationControlPresenter(int poolSize, SynapseConnectionInfo sourceInfo, SynapseConnectionInfo destInfo, final View view) {
		if(sourceInfo == null) throw new IllegalArgumentException("Source information cannot be null");
		if(destInfo == null) throw new IllegalArgumentException("Destination information cannot be null");
		if(view == null) throw new IllegalArgumentException("View cannot be null");
		this.view = view;
		this.poolSize = poolSize;
		this.sourceInfo = sourceInfo;
		this.destInfo = destInfo;
		// This must be called from the EDT.
		if(!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("The constructor can only be called from the Event-Dispatch-Thread");
		
		// The start button should be enabled and the stop disabled
		view.setStartEnabled(true);
		view.setStopEnabled(false);
		// Clear the progress bars.
		view.resetAllProgress(WAITING_TO_START);
		
		// Register the start and stop listeners
		view.addStartListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// First disable the start
				view.setStartEnabled(false);
				startMigration();
				view.setStopEnabled(true);
			}
		});
		// Add the stop listener
		view.addStopListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// First disable the start
				view.setStopEnabled(false);
				stopMigration();
				view.setStartEnabled(true);
			}
		});
		
		// Start the creation Monitor
		view.setCreationTotalProgress(0, 100, "Starting...");
		startCreationMonitor();
		
	}
	private void startCreationMonitor(){
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				BasicProgress progress = new BasicProgress();
				progress.setCurrent(0);
				progress.setTotal(0);
				Synapse sourceClient = null;
				Synapse destClient = null;
				while(true){
					try{
						ClientFactoryImpl factory = new ClientFactoryImpl();
						if(sourceClient == null){
							sourceClient = factory.createNewConnection(sourceInfo);
						}
						// Give interrupt a chance.
						Thread.sleep(100);
						if(destClient == null){
							destClient = factory.createNewConnection(destInfo);
						}
						// Get the source and destination counts
						QueryRunner queryRunner = new QueryRunnerImpl();
						long sourceTotal = queryRunner.getTotalEntityCount(sourceClient);
						long destTotal = queryRunner.getTotalEntityCount(destClient);
						// Update the progress
						progress.setCurrent(destTotal);
						progress.setTotal(sourceTotal);
						if(sourceTotal == destTotal){
							progress.setDone();
						}
						String message = String.format("Source: %1$,d , Destination: %2$,d  %3$s", sourceTotal, destTotal, progress.getCurrentStatus().toStringHours());
						updateCreationProgress(progress, message);
						if(sourceTotal == destTotal){
							// Done with create so kill this.
							return;
						}

						Thread.sleep(1000);
					} catch (InterruptedException e){
						// kill the migration.
						log.debug("Stopping migration: "+e.getMessage());
						return;
					} catch (Exception e) {
						// Log the error
						log.error(e);
						// Show it to the user.
						showError(e);
						try {
							Thread.sleep(1000*60);
						} catch (InterruptedException e1) {
							return;
						}
						// Clear the connections
						sourceClient = null;
						destClient = null;
					}
				}
				
			}
		});
		thread.start();
	}
	
	private void startMigration(){
		// first make sure the 
		this.migrationThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				// the thread pool.
				ExecutorService threadPool = Executors.newFixedThreadPool(poolSize);;
				// Do the work
				while(true){
					try {
						// Reset all progress
						resetAllProgress(WAITING_TO_START);
						// The JOB queue
						Queue<Job> jobQueue = new ConcurrentLinkedQueue<Job>();
						// Give interrupt a chance.
						Thread.sleep(100);
						// connect to both repositories
						ClientFactoryImpl factory = new ClientFactoryImpl();
						Synapse sourceClient = factory.createNewConnection(sourceInfo);
						// Give interrupt a chance.
						Thread.sleep(100);
						Synapse destClient = factory.createNewConnection(destInfo);
						// Give interrupt a chance.
						Thread.sleep(100);
						// Phase one collect information from both threads.
						// 1. Get all entity data from both the source and destination.
						BasicProgress sourceProgress = new BasicProgress();
						BasicProgress destProgress = new BasicProgress();
						QueryRunner queryRunner = new QueryRunnerImpl();
						AllEntityDataWorker sourceQueryWorker = new AllEntityDataWorker(sourceClient, queryRunner, sourceProgress);

						// Stop work when interrupted.
						if(Thread.interrupted()) return;
						AllEntityDataWorker destQueryWorker = new AllEntityDataWorker(destClient, queryRunner, destProgress);
						// Start both at the same time
						Future<List<EntityData>> sourceFuture = threadPool.submit(sourceQueryWorker);
						Future<List<EntityData>> destFuture = threadPool.submit(destQueryWorker);
						// Give interrupt a chance.
						Thread.sleep(100);
						// Wait for both to finish
						log.info("Starting phase one: Gathering all data from the source and destination repository...");
						while(!sourceFuture.isDone() || !destFuture.isDone()){
							// Report on the progress.
							// update the view
							updateCollectProgress(sourceProgress, destProgress);
							Thread.sleep(200);
						}
						// Final update
						updateCollectProgress(sourceProgress, destProgress);
						
						// Get the results
						List<EntityData> sourceData = sourceFuture.get();
						List<EntityData> destData = destFuture.get();
						// Give interrupt a chance.
						Thread.sleep(100);
						log.debug("Finished phase one.  Source entity count: "+sourceData.size()+". Destination entity Count: "+destData.size());
						// Start phase 2
						log.debug("Starting phase two: Calculating creates, updates, and deletes...");
						ResponseBundle response = MigrationDriver.populateQueue(threadPool, jobQueue, sourceData, destData, Configuration.getMaximumBatchSize());
						// Build the prefix
						BuilderResponse create = response.getCreateResponse();
						BuilderResponse update = response.getUpdateResponse();
						BuilderResponse delete = response.getDeleteResponse();
						String prefix = "Creating: "+create.getSubmitedToQueue()+", Updating: "+update.getSubmitedToQueue()+", Deleting: "+delete.getSubmitedToQueue()+", Pending: "+create.getPendingDependancies()+".  ";
						// 3. Process all jobs on the queue.
						log.debug("Starting phase three: Processing the job queue...");
						AggregateProgress consumingProgress = new AggregateProgress();
						
						Future<AggregateResult> consumFuture = MigrationDriver.consumeAllJobs(factory, threadPool, jobQueue, consumingProgress);
						while(!consumFuture.isDone()){
							//log.info("Processing entities: "+consumingProgress.getCurrentStatus());
							// Update the progress
							updateConsumingProgress(consumingProgress, prefix);
							Thread.sleep(200);
						}
						// Update the progress
						updateConsumingProgress(consumingProgress, prefix);
						// Start the loop over again.
						Thread.sleep(2000);
					} catch (InterruptedException e){
						// Stop the thread pool
						threadPool.shutdownNow();
						// kill the migration.
						log.debug("Stopping migration: "+e.getMessage());
						return;
					} catch (Exception e) {
						// Log the error
						log.error(e);
						// Show it to the user.
						showError(e);
						try {
							Thread.sleep(1000*60);
						} catch (InterruptedException e1) {
							return;
						}
					}
				}
			}
		});
		this.migrationThread.start();
		
	}
	
	
	private void stopMigration(){
		// Terminate migration
		this.migrationThread.interrupt();
	}


	/**
	 * This method will block.
	 * 
	 * @param e
	 */
	public void showError(final Exception e) {
		// Show the error to the user but do not block.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.showErrorMessage(e);
			}
		});
	}
	
	private void updateCollectProgress(final BasicProgress sourceProgress, final BasicProgress destProgress){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				String sourceMessage = "Source: "+sourceProgress.getCurrentStatus().toString();
				String desetMessage = "Destination: "+ destProgress.getCurrentStatus().toString();
				view.setSourceCollectProgress((int)sourceProgress.getCurrent(), (int)sourceProgress.getTotal(), sourceMessage);
				view.setDestinationCollectProgress((int)destProgress.getCurrent(), (int)destProgress.getTotal(),desetMessage);
			}
		});
	}
	
	private void updateConsumingProgress(final AggregateProgress progress, final String prefix){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.setConsumingProgress((int)progress.getCurrent(), (int)progress.getTotal(), prefix+progress.getCurrentStatus().toStringHours());
			}
		});
	}
	
	private void updateCreationProgress(final BasicProgress progress, final String message){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.setCreationTotalProgress((int)progress.getCurrent(), (int)progress.getTotal(), message);
			}
		});
	}
	
	private void resetAllProgress(final String message){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.resetAllProgress(message);
			}
		});
	}
}
