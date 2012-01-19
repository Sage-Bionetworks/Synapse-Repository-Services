package org.sagebionetworks.tool.migration.gui;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.tool.migration.Configuration;
import org.sagebionetworks.tool.migration.MigrationDriver;
import org.sagebionetworks.tool.migration.SynapseConnectionInfo;
import org.sagebionetworks.tool.migration.gui.presenter.MigrationControlPresenter;
import org.sagebionetworks.tool.migration.gui.presenter.StackStatusPresenter;
import org.sagebionetworks.tool.migration.gui.view.MigrationControlView;
import org.sagebionetworks.tool.migration.gui.view.StackStatusView;

/**
 * The Main UI for Migration.
 * 
 * @author John
 *
 */
public class MigrationConsoleUI extends JFrame {

	private static final String SYNAPSE_MIGRATION_UTILTITY_TITLE = "Synapse Migration Utility";
	private static final String ICONS_FORWARD_PNG = "icons/forward.png";
	private static final long serialVersionUID = 1L;
	private static final String PREFERRED_LOOK_AND_FEEL = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
	
	private StackStatusView sourcePanel = new StackStatusView();
	private StackStatusView destinationPanel = new StackStatusView();
	private JLabel arrowLabel = null;
	private MigrationControlView controlPanel = new MigrationControlView();
	
	
	public MigrationControlView getControlPanel() {
		return controlPanel;
	}

	public StackStatusView getSourcePanel() {
		return sourcePanel;
	}

	public StackStatusView getDestinationPanel() {
		return destinationPanel;
	}

	public MigrationConsoleUI() {
		initComponents();
	}

	private void initComponents() {
		setTitle(SYNAPSE_MIGRATION_UTILTITY_TITLE);
		BorderLayout layout = new BorderLayout();
		this.setLayout(layout);
		arrowLabel = new JLabel(createImageIcon(ICONS_FORWARD_PNG, "->"));
		this.add(sourcePanel, BorderLayout.WEST);
		this.add(arrowLabel, BorderLayout.CENTER);
		this.add(destinationPanel, BorderLayout.EAST);
		this.add(controlPanel, BorderLayout.SOUTH);
		
		setSize(650, 600);
	}
    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = MigrationConsoleUI.class.getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

	private static void installLnF() {
		try {
			String lnfClassname = PREFERRED_LOOK_AND_FEEL;
			if (lnfClassname == null)
				lnfClassname = UIManager.getCrossPlatformLookAndFeelClassName();
			UIManager.setLookAndFeel(lnfClassname);
		} catch (Exception e) {
			System.err.println("Cannot install " + PREFERRED_LOOK_AND_FEEL
					+ " on this platform:" + e.getMessage());
		}
	}

	/**
	 * Main entry of the class.
	 * Note: This class is only created so that you can easily preview the result at runtime.
	 * It is not expected to be managed by the designer.
	 * You can modify it as you like.
	 * @throws IOException 
	 * @throws SynapseException 
	 */
	public static void main(String[] args) throws IOException, SynapseException {
		// First load the configuration
		MigrationDriver.loadConfigUsingArgs(args);
		// Get the source and destination info
		// Create the two connections.
		final SynapseConnectionInfo sourceInfo = Configuration.getSourceConnectionInfo();
		final SynapseConnectionInfo destInfo = Configuration.getDestinationConnectionInfo();
		// Create the thread pool
		int maxThreads = Configuration.getMaximumNumberThreads();
		// We must have at least 2 threads
		if(maxThreads < 2){
			maxThreads = 2;
		}
		final int finalThreadCount = maxThreads;
		
		installLnF();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				
				// Create the presenters
				MigrationConsoleUI frame = new MigrationConsoleUI();
				frame.setDefaultCloseOperation(MigrationConsoleUI.EXIT_ON_CLOSE);
				frame.getContentPane().setPreferredSize(frame.getSize());
				frame.pack();
				frame.setLocationRelativeTo(null);
				
				// Before we show the main frame setup the presenters.
				StackStatusPresenter sourcePresenter = new StackStatusPresenter(RepositoryType.SOURCE, sourceInfo, frame.getSourcePanel());
				StackStatusPresenter destPresenter = new StackStatusPresenter(RepositoryType.DESTINATION, destInfo, frame.getDestinationPanel());
				// Create the control presenter
				MigrationControlPresenter controlPresenter = new MigrationControlPresenter(finalThreadCount, sourceInfo, destInfo, frame.getControlPanel());
				// Now show the frame.
				frame.setVisible(true);
				// Now start the presenter threads
				Thread one = new Thread(sourcePresenter);
				Thread two = new Thread(destPresenter);
				one.start();
				two.start();
			}
		});
	}

}
