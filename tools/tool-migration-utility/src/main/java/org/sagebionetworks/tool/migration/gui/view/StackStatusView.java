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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.tool.migration.gui.PreviewUtil;
import org.sagebionetworks.tool.migration.gui.model.EntityCountModel;
import org.sagebionetworks.tool.migration.gui.presenter.StackStatusPresenter;

public class StackStatusView extends JPanel implements StackStatusPresenter.View {
	private static final long serialVersionUID = 1L;
	private static final String LONG_WITH_COMMA_FORMAT = "%1$,d";
	private TitledBorder border = new TitledBorder("Title");
	private JLabel authStaticLabel = new JLabel("Authentication URL:");
	private JTextField authUrlText = new JTextField("http://auth-test-url");
	private JLabel repoStaticLabel = new JLabel("Repository URL:");
	private JTextField repoUrlText =  new JTextField("http://repo-test-url");
	private JLabel stackStatusLabel = new JLabel("Stack Status:");
	private JTextField stackStatusText = new JTextField("READ_ONLY");
	private JButton changeStatus = new JButton("...");
	private JLabel progressLabel = new JLabel("Starting...");
	private JProgressBar progressBar = new JProgressBar();
	private EntityCountModel countModel = new EntityCountModel();
	private JTable statusTable = new JTable(countModel);
	private JScrollPane tableScrollPane = new JScrollPane(statusTable);
	private JLabel totalLabel = new JLabel("Total Entity Count:");
	private JTextField totalText = new JTextField("Unknown");

	public StackStatusView(){
		initComponents();
	}

	private void initComponents() {
		// Start with indeterminate
		progressBar.setIndeterminate(false);
		
		// the text fields are not editable
		authUrlText.setEditable(false);
		repoUrlText.setEditable(false);
		stackStatusText.setEditable(false);
		statusTable.setEnabled(false);
		totalText.setEditable(false);
		// seupt the layout
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		this.setBorder(border);
		// Use default gaps
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		// Horizontal
		ParallelGroup h1 = layout.createParallelGroup(GroupLayout.Alignment.TRAILING);
		h1.addComponent(authStaticLabel);
		h1.addComponent(repoStaticLabel);
		h1.addComponent(stackStatusLabel);
		h1.addComponent(totalLabel);
		ParallelGroup h2 = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
		h2.addComponent(authUrlText);
		h2.addComponent(repoUrlText);
		h2.addComponent(stackStatusText);
		h2.addComponent(totalText);
		ParallelGroup h3 = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
		h3.addComponent(changeStatus);
		SequentialGroup hSequence = layout.createSequentialGroup();
		hSequence.addGroup(h1);
		hSequence.addGroup(h2);
		hSequence.addGroup(h3);
		
		ParallelGroup h4 = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
		h4.addComponent(progressLabel);
		h4.addComponent(progressBar);
		h4.addComponent(tableScrollPane);
		
		ParallelGroup hOuter = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
		hOuter.addGroup(hSequence);
		hOuter.addGroup(h4);
		
		// Vertical
		ParallelGroup v1 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v1.addComponent(authStaticLabel);
		v1.addComponent(authUrlText);
		ParallelGroup v2 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v2.addComponent(repoStaticLabel);
		v2.addComponent(repoUrlText);
		ParallelGroup v3 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v3.addComponent(stackStatusLabel);
		v3.addComponent(stackStatusText);
		v3.addComponent(changeStatus);
		ParallelGroup v4 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v4.addComponent(progressLabel);
		ParallelGroup v5 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v5.addComponent(progressBar);
		ParallelGroup v6 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v6.addComponent(tableScrollPane);
		ParallelGroup v7 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
		v7.addComponent(totalLabel);
		v7.addComponent(totalText);
		
		SequentialGroup vSequence = layout.createSequentialGroup();
		vSequence.addGroup(v1);
		vSequence.addGroup(v2);
		vSequence.addGroup(v3);
		vSequence.addGroup(v4);
		vSequence.addGroup(v5);
		vSequence.addGroup(v6);
		vSequence.addGroup(v7);
		
		layout.setHorizontalGroup(hOuter);
		layout.setVerticalGroup(vSequence);
		
		setPreferredSize(new Dimension(300, 400));
	}	
	/**
	 * Preview this component.
	 */
	public static void main(String[] args) {
		PreviewUtil.previewComponent(new StackStatusView());
	}

	@Override
	public void setAuthUrl(String url) {
		this.authUrlText.setText(url);
	}

	@Override
	public void setRepoUrl(String url) {
		this.repoUrlText.setText(url);
	}

	@Override
	public void setStackStatus(String status) {
		this.stackStatusText.setText(status);
	}

	@Override
	public void setProgressMessage(String message) {
		this.progressLabel.setText(message);
	}

	@Override
	public void resetProgress(int min, int max, String message) {
		progressBar.setMinimum(min);
		progressBar.setMaximum(max);
		progressBar.setValue(min);
		this.progressLabel.setText(message);
	}

	@Override
	public void setProgressValue(int value, String message) {
		progressBar.setValue(value);
		this.progressLabel.setText(message);
	}

	@Override
	public void setTitle(String title) {
		this.border.setTitle(title);
	}

	@Override
	public void showErrorMessage(Exception e) {
		JOptionPane.showMessageDialog(this.getParent(), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void setTotalEntityCount(long count) {
		this.totalText.setText(String.format(LONG_WITH_COMMA_FORMAT, count));
	}

	@Override
	public void setEntityTypeCount(EntityType type, long count) {
		countModel.setValue(type, count);
	}

	@Override
	public void addStatusChangeListner(ActionListener listener) {
		changeStatus.addActionListener(listener);
	}
}
