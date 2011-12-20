package org.sagebionetworks.client;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.transfer.Upload;

/**
 * A Swing-based progress listener implementation
 * 
 * @author deflaux
 * 
 */
public class SwingProgressListener implements org.sagebionetworks.client.ProgressListener {
	private static final String TITLE = "Synapse File Upload to Amazon S3";
	private static final int MIN_HEIGHT_PIXELS = 100;
	private static final int MIN_WIDTH_PIXELS = 300;

	private JProgressBar pb;
	private JFrame frame;
	private Upload upload;

	/**
	 * Default Constructor
	 */
	public SwingProgressListener() {
		frame = new JFrame(TITLE);
		pb = new JProgressBar(0, 100);
		pb.setStringPainted(true);

		frame.setContentPane(createContentPane());
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame
				.setMinimumSize(new Dimension(MIN_WIDTH_PIXELS,
						MIN_HEIGHT_PIXELS));
	}

	private JPanel createContentPane() {
		JPanel panel = new JPanel();
		panel.add(pb);

		JPanel borderPanel = new JPanel();
		borderPanel.setLayout(new BorderLayout());
		borderPanel.add(panel, BorderLayout.NORTH);
		borderPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		return borderPanel;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.client.ProgressListener#setUpload(com.amazonaws.services.s3.transfer.Upload)
	 */
	@Override
	public void setUpload(Upload upload) {
		this.upload = upload;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.client.ProgressListener#progressChanged(com.amazonaws.services.s3.model.ProgressEvent)
	 */
	@Override
	public void progressChanged(ProgressEvent progressEvent) {
		if (upload == null)
			return;

		pb.setValue((int) upload.getProgress().getPercentTransfered());

		switch (progressEvent.getEventCode()) {
		case ProgressEvent.COMPLETED_EVENT_CODE:
			pb.setValue(100);
			break;
		case ProgressEvent.FAILED_EVENT_CODE:
			try {
				AmazonClientException e = upload.waitForException();
				JOptionPane
						.showMessageDialog(frame,
								"Unable to upload file to Amazon S3: "
										+ e.getMessage(),
								"Error Uploading File",
								JOptionPane.ERROR_MESSAGE);
			} catch (InterruptedException e) {
			}
			break;
		}
	}
}
