package org.sagebionetworks.tool.migration.gui.view;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.TitledBorder;

import org.sagebionetworks.tool.migration.gui.PreviewUtil;
import org.sagebionetworks.tool.migration.gui.presenter.MigrationControlPresenter;


public class MigrationControlView extends JPanel implements MigrationControlPresenter.View {
	
	private static final long serialVersionUID = 1L;
	private JButton startButton = new JButton("Start");
	private JButton stopButton = new JButton("Stop");
	private JLabel phaseOneLabel = new JLabel("Phase One: Collects information from both the source and destination repository...");
	private JProgressBar collectSourceProgress = new JProgressBar();
	private JProgressBar collectDestProgress = new JProgressBar();
	private JLabel phaseTwoLabel = new JLabel("Phase Two: Executes Create, Update, and Deletes...");
	private JProgressBar phaseTwoProgress = new JProgressBar();
	private JLabel totalLabel = new JLabel("Total Creation Progress...");
	private JProgressBar totalCreateProgress = new JProgressBar();
	
	public MigrationControlView(){
		initComponents();
	}

	private void initComponents() {
		// The layout.
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		this.setBorder(new TitledBorder("Migration control"));
		// Use default gaps
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		// Setup the progress bars
		collectSourceProgress.setStringPainted(true);
		collectSourceProgress.setString("source");
		collectSourceProgress.setMinimum(0);
		collectSourceProgress.setMaximum(100);
		collectSourceProgress.setValue(5);
		
		collectDestProgress.setStringPainted(true);
		collectDestProgress.setString("destination");
		collectDestProgress.setMinimum(0);
		collectDestProgress.setMaximum(100);
		collectDestProgress.setValue(5);
		
		phaseTwoProgress.setStringPainted(true);
		phaseTwoProgress.setString("phase two");
		phaseTwoProgress.setMinimum(0);
		phaseTwoProgress.setMaximum(100);
		phaseTwoProgress.setValue(13);
		
		totalCreateProgress.setStringPainted(true);
		totalCreateProgress.setString("total create");
		totalCreateProgress.setMinimum(0);
		totalCreateProgress.setMaximum(100);
		totalCreateProgress.setValue(13);
		
		// Horizontal
		ParallelGroup h1 = layout.createParallelGroup(GroupLayout.Alignment.TRAILING);
		h1.addComponent(startButton);
		ParallelGroup h2 = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
		h2.addComponent(stopButton);
		SequentialGroup hSequence = layout.createSequentialGroup();
		hSequence.addGroup(h1);
		hSequence.addGroup(h2);
		
		ParallelGroup h3 = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
		h3.addComponent(phaseOneLabel);
		h3.addComponent(collectSourceProgress);
		h3.addComponent(collectDestProgress);
		h3.addComponent(phaseTwoLabel);
		h3.addComponent(phaseTwoProgress);
		h3.addComponent(totalLabel);
		h3.addComponent(totalCreateProgress);
		
		ParallelGroup hOuter = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
		hOuter.addGroup(hSequence);
		hOuter.addGroup(h3);
		
		// Vertical
		ParallelGroup v1 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v1.addComponent(startButton);
		v1.addComponent(stopButton);
		ParallelGroup v2 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v2.addComponent(phaseOneLabel);
		ParallelGroup v3 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v3.addComponent(collectSourceProgress);
		ParallelGroup v4 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v4.addComponent(collectDestProgress);
		ParallelGroup v5 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v5.addComponent(phaseTwoLabel);
		ParallelGroup v6 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v6.addComponent(phaseTwoProgress);
		ParallelGroup v7 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v7.addComponent(totalLabel);
		ParallelGroup v8 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v8.addComponent(totalCreateProgress);
	
		SequentialGroup vSequence = layout.createSequentialGroup();
		vSequence.addGroup(v1);
		vSequence.addGroup(v2);
		vSequence.addGroup(v3);
		vSequence.addGroup(v4);
		vSequence.addGroup(v5);
		vSequence.addGroup(v6);
		vSequence.addGroup(v7);
		vSequence.addGroup(v8);
		
		layout.setHorizontalGroup(hOuter);
		layout.setVerticalGroup(vSequence);
		
		setPreferredSize(new Dimension(500, 250));
		
	}

	/**
	 * Preview this component.
	 */
	public static void main(String[] args) {
		PreviewUtil.previewComponent(new MigrationControlView());
	}

	@Override
	public void showErrorMessage(Exception e) {
		JOptionPane.showMessageDialog(this.getParent(), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void addStartListener(ActionListener listener) {
		this.startButton.addActionListener(listener);
	}

	@Override
	public void setStartEnabled(boolean enabled) {
		this.startButton.setEnabled(enabled);
	}

	@Override
	public void addStopListener(ActionListener listener) {
		this.stopButton.addActionListener(listener);
	}

	@Override
	public void setStopEnabled(boolean enabled) {
		this.stopButton.setEnabled(enabled);
	}

	@Override
	public void setSourceCollectProgress(int value, int total, String message) {
		int percent = calulcatePercent(value, total);
		this.collectSourceProgress.setValue(percent);
		this.collectSourceProgress.setString(message);
	}

	@Override
	public void setDestinationCollectProgress(int value, int total, String message) {
		int percent = calulcatePercent(value, total);
		this.collectDestProgress.setValue(percent);
		this.collectDestProgress.setString(message);
	}
	
	@Override
	public void setConsumingProgress(int value, int total, String message) {
		int percent = calulcatePercent(value, total);
		this.phaseTwoProgress.setValue(percent);
		this.phaseTwoProgress.setString(message);
	}
	
	@Override
	public void setCreationTotalProgress(int value, int total, String message) {
		int percent = calulcatePercent(value, total);
		this.totalCreateProgress.setValue(percent);
		this.totalCreateProgress.setString(message);
	}
	
	/**
	 * Calculate a percent to be used for the progress bar.
	 * @param value
	 * @param total
	 * @return
	 */
	private int calulcatePercent(int value, int total){
		if(total < 1) return 0;
		if(value >= total){
			return 100;
		}else{
			return (int)((float)value/(float)total*100f);
		}
	}

	@Override
	public void resetAllProgress(String message) {
		// Reset all three
		resetProgress(collectSourceProgress, message);
		resetProgress(collectDestProgress, message);
		resetProgress(phaseTwoProgress, message);
	}
	
	private static void resetProgress(JProgressBar pb, String message){
		pb.setMaximum(0);
		pb.setValue(0);
		pb.setMaximum(100);
		pb.setString(message);
	}

}
