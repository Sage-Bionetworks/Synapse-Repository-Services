package org.sagebionetworks.tool.migration.gui.presenter;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.ClientFactoryImpl;
import org.sagebionetworks.tool.migration.SynapseConnectionInfo;
import org.sagebionetworks.tool.migration.dao.QueryRunner;
import org.sagebionetworks.tool.migration.dao.QueryRunnerImpl;
import org.sagebionetworks.tool.migration.gui.RepositoryType;
import org.sagebionetworks.tool.migration.gui.view.StackStatusEditor;

/**
 * The presenter drives the view and fetches all required data.
 * @author John
 *
 */
public class StackStatusPresenter implements Runnable{
	
	static private Log log = LogFactory.getLog(StackStatusPresenter.class);
	
	/**
	 * The interface that defines the view.
	 *
	 */
	public static interface View {
		
		/**
		 * Set the title
		 * @param title
		 */
		public void setTitle(String title);
		
		/**
		 * Set the authentication services URL.
		 * @param url
		 */
		public void setAuthUrl(String url);
		
		/**
		 * Set the repository URL.
		 * @param url
		 */
		public void setRepoUrl(String url);
		
		/**
		 * Set the stack status
		 * @param status
		 */
		public void setStackStatus(String status);
		
		/**
		 * Set the progress message.
		 * @param message
		 */
		public void setProgressMessage(String message);
		
		/**
		 * Reset the progress bar.
		 * @param min
		 * @param max
		 * @param message
		 */
		public void resetProgress(int min, int max, String message);
		
		/**
		 * Make some progress
		 * @param value
		 * @param message
		 */
		public void setProgressValue(int value, String message);
		
		/**
		 * Show the given exception in an error dialog.
		 * @param e
		 */
		public void showErrorMessage(Exception e);
		
		/**
		 * Set the total entity count
		 * @param count
		 */
		public void setTotalEntityCount(long count);
		
		/**
		 * Set the count for a given entity type.
		 * @param type
		 * @param count
		 */
		public void setEntityTypeCount(EntityType type, long count);
		
		/**
		 * Listener for the start button.
		 * @param listener
		 */
		public void addStatusChangeListner(ActionListener listener);
		
	}
	
	
	private View view = null;
	private SynapseConnectionInfo connectionInfo = null;
	private RepositoryType type;
	
	
	/**
	 * Create a new presenter.
	 * @param sourceInfo
	 * @param view
	 */
	public StackStatusPresenter(RepositoryType type, SynapseConnectionInfo info, final View view){
		if(type == null) throw new IllegalArgumentException("RepositoryType cannot be null");
		if(info == null) throw new IllegalArgumentException("Source info cannot be null");
		if(view == null) throw new IllegalArgumentException("The view cannot be null");
		this.type = type;
		this.connectionInfo = info;
		this.view = view;
		if(!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("The constructor can only be called from the Event-Dispatch-Thread");
		// Initialized the view
		view.setTitle(type.getTitle());
		view.setAuthUrl(info.getAuthenticationEndPoint());
		view.setRepoUrl(info.getRepositoryEndPoint());
		view.setStackStatus("UNKNOWN");
		view.resetProgress(0, 100, "Initializing...");
		final StackStatusPresenter presenter = this;
		// Listen for button changes
		view.addStatusChangeListner(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent event) {
				// Show the edit dialog.
				// First get the current status.
				try {
					ClientFactoryImpl factory = new ClientFactoryImpl();
					SynapseAdministration client = factory.createNewConnection(connectionInfo);
					StackStatus status = client.getCurrentStackStatus();
					if(status != null){
						StackStatusEditor editor = new StackStatusEditor();
						StackStatus updated = editor.showStatusDialog((Component)view, status);
						if(updated != null){
							client.updateCurrentStackStatus(updated);
							view.setStackStatus(updated.getStatus().name());
						}
					}
				} catch (Exception e1) {
					showError(e1);
				} 				
			}
		});
	}


	@Override
	public void run() {
		// Any work we do here is off of the event dispatch thread, so we will need SwingUtilties to talk to the view.
		while(true){
			try{
				// Start the progress
				int totalProgress = EntityType.values().length+3;
				int current = 0;
				resetProgress(current, totalProgress, "Authenticating...");
				// Connect
				ClientFactoryImpl factory = new ClientFactoryImpl();
				SynapseAdministration client = factory.createNewConnection(this.connectionInfo);
				// Get the status
				setProgresssValue(current++, "Getting stack status...");
				StackStatus status = client.getCurrentStackStatus();
				setStackStatus(status.getStatus().name());
				
				QueryRunner queryRunner = new QueryRunnerImpl(client);
				// Get each entity type
				for(EntityType type: EntityType.values()){
					// Make some progress
					setProgresssValue(current++, "Querying for "+type.name()+" count...");
					long count = queryRunner.getCountForType(type);
					setCountForEntity(type, count);
				}
				
				// Make some progress
				setProgresssValue(current++, "Querying for total entity count...");
				// Count the total number of entities
				long total = queryRunner.getTotalEntityCount();
				setTotalCount(total);
				
				// Finished
				setProgresssValue(totalProgress, "Waiting for next run...");
				
				Thread.sleep(20000);
			}catch(final Exception e){
				// Log the error and show it to the user
				log.error(e);
				// Show the error to the user
				showError(e);
				try {
					Thread.sleep(1000*60);
				} catch (InterruptedException e1) {
					// terminate
					return;
				}
			}
		}
	}


	/**
	 * This method will block.
	 * @param e
	 */
	public void showError(final Exception e) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					view.showErrorMessage(e);
				}
			});
		} catch (InterruptedException e1) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e1) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Set the stack status.
	 * @param name
	 */
	private void setStackStatus(final String name) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.setStackStatus(name);
			}
		});
	}


	/**
	 * Set the progress value.
	 * @param value
	 * @param message
	 */
	private void setProgresssValue(final int value, final String message) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.setProgressValue(value, message);
			}
		});
	}
	
	/**
	 * Set the progress value.
	 * @param value
	 * @param message
	 */
	private void setTotalCount(final long total) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.setTotalEntityCount(total);
			}
		});
	}
	
	private void setCountForEntity(final EntityType type, final long count) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.setEntityTypeCount(type, count);
			}
		});
	}


	/**
	 * Reset the progress.
	 * @param min
	 * @param max
	 * @param message
	 */
	public void resetProgress(final int min, final int max, final String message) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.resetProgress(min, max, message);
			}
		});
	}

}
