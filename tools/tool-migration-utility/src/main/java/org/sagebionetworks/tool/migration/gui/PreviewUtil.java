package org.sagebionetworks.tool.migration.gui;

import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Preview a component.
 * @author John
 *
 */
public class PreviewUtil {
	
	private static final String PREFERRED_LOOK_AND_FEEL = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
	
	/**
	 * Preview a component in a dialog.
	 * @param contentPane
	 */
	public static void previewComponent(final Container contentPane){
		installLnF();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JFrame dialog = new JFrame();
				dialog.setDefaultCloseOperation(MigrationConsoleUI.EXIT_ON_CLOSE);
				dialog.setTitle("Showing: "+contentPane.getClass().getName());
				dialog.setContentPane(contentPane);
				dialog.pack();
				dialog.setLocationRelativeTo(null);
				dialog.setVisible(true);
			}
		});
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

}
