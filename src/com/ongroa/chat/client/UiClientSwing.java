package com.ongroa.chat.client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.ongroa.chat.Message;
import com.ongroa.chat.Util;

public class UiClientSwing 
extends JFrame 
implements ActionListener, UserInterface {
	private static final long serialVersionUID = 1L;

	private static final String BUTTON_CONNECT_TEXT = "Connect";
	private static final String BUTTON_CONNECTING_TEXT = "Connecting...";
	private static final String BUTTON_DISCONNECT_TEXT = "Disconnect";
	private static final String BUTTON_DISCONNECTING_TEXT = "Disconnecting...";

	private static final int WIDTH = 60;
	private static final int HEIGHT = 20;

	private ChatClient mChatClient;

	private JTextField textFieldServerIp;
	private JTextField textFieldServerPort;
	private JTextField textFieldNick;
	private JButton buttonConnect;
	private JTextArea textAreaResponse;
	private JTextField textFieldMessage;
	private JTextArea textAreaNicks;

	public UiClientSwing(ChatClient chatClient) {
		mChatClient = chatClient;
		createGui();
	}

	public ChatClient getChatClient() {
		return mChatClient;
	}

	public void setChatClient(ChatClient mChatClient) {
		this.mChatClient = mChatClient;
	}

	public void createGui() {
		String defaultNick = "Zoli";
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		String localIP = mChatClient.getClient().getIpAddress();
		JLabel labelServerIp = new JLabel("Server IP address: ");
		textFieldServerIp = new JTextField(localIP, 15);
		JLabel labelServerPort = new JLabel("Server port: ");
		textFieldServerPort = new JTextField("" + Util.port, 4);
		JLabel labelNick = new JLabel("Nick name: ");
		textFieldNick = new JTextField(defaultNick, 15);
		buttonConnect = new JButton(BUTTON_CONNECT_TEXT);
		buttonConnect.addActionListener(this);
		FlowLayout layoutNorth = new FlowLayout(FlowLayout.LEFT);
		JPanel panelNorth = new JPanel();
		panelNorth.setLayout(layoutNorth);
		panelNorth.add(labelServerIp);
		panelNorth.add(textFieldServerIp);
		panelNorth.add(labelServerPort);
		panelNorth.add(textFieldServerPort);
		panelNorth.add(labelNick);
		panelNorth.add(textFieldNick);
		panelNorth.add(buttonConnect);
		add(panelNorth, BorderLayout.NORTH);
		JPanel panelEast = new JPanel();
		panelEast.setLayout(new BoxLayout(panelEast, BoxLayout.Y_AXIS)); 
		JLabel labelNicks = new JLabel("Users:");
		textAreaNicks = new JTextArea(HEIGHT, 20);
		textAreaNicks.setEditable(false);
		panelEast.add(labelNicks);
		panelEast.add(textAreaNicks);
		add(panelEast, BorderLayout.EAST);
		textAreaResponse = new JTextArea(HEIGHT, WIDTH);
		textAreaResponse.setEditable(false);
		JScrollPane paneResponse = new JScrollPane(textAreaResponse);
		add(paneResponse, BorderLayout.CENTER);
		textFieldMessage = new JTextField(WIDTH);
		textFieldMessage.addActionListener(this);
		JScrollPane paneMessage = new JScrollPane(textFieldMessage);
		add(paneMessage, BorderLayout.SOUTH);
		pack();
	}

	public String getServerIpAddress() {
		return textFieldServerIp.getText();
	}
	
	public String getServerPort() {
		return textFieldServerPort.getText();
	}
	
	public String getNick() {
		return textFieldNick.getText();
	}
	
	public void setNick(String nick) {
		textFieldNick.setText(nick);
	}
	
	private void enableWidgets(boolean enable) {
		textFieldServerIp.setEnabled(enable);
		textFieldServerPort.setEnabled(enable);
		textFieldNick.setEnabled(enable);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == buttonConnect) {
			if (mChatClient.getState() == ChatClient.IDLE) {
				textFieldNick.setText(textFieldNick.getText().replaceAll(" ", ""));
				boolean succ = mChatClient.connect(
						true,
						textFieldServerIp.getText(),
						textFieldServerPort.getText(),
						textFieldNick.getText());
				if (succ) {
					buttonConnect.setText(BUTTON_CONNECTING_TEXT);
					enableWidgets(false);
					pack();
					mChatClient.setState(ChatClient.CONNECTING);
				}
			}
			else if (mChatClient.getState() == ChatClient.CONNECTING) {
				buttonConnect.setText(BUTTON_CONNECT_TEXT);
				enableWidgets(true);
				pack();
				mChatClient.setState(ChatClient.IDLE);
			}
			else if (mChatClient.getState() == ChatClient.CONNECTED) {
				boolean succ = mChatClient.connect(
						false,
						textFieldServerIp.getText(),
						textFieldServerPort.getText(),
						textFieldNick.getText());
				if (succ) {
					buttonConnect.setText(BUTTON_DISCONNECTING_TEXT);
					pack();
					mChatClient.setState(ChatClient.TERMINATING);
				}
			}
		}
		if (source == textFieldMessage) {
			if (mChatClient.getState() == ChatClient.CONNECTED) {
				String nick = textFieldNick.getText();
				String msg = textFieldMessage.getText();
				mChatClient.sendMessage(Util.MESSAGE, 
						textFieldServerIp.getText(), 
						Integer.parseInt(textFieldServerPort.getText()), 
						nick, 
						msg);
				textFieldMessage.setText("");
			}
		}
	}

	@Override
	public void conneced() {
		buttonConnect.setText(BUTTON_DISCONNECT_TEXT);
		pack();
	}

	@Override
	public void usedNick() {
		buttonConnect.setText(BUTTON_CONNECT_TEXT);
		enableWidgets(true);
		pack();
		JOptionPane.showMessageDialog(this,
				"Used nick",
				"Connect error",
				JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void usedIpPortPair() {
		buttonConnect.setText(BUTTON_CONNECT_TEXT);
		enableWidgets(true);
		pack();
	}

	@Override
	public void newMessageReceived(Message message) {
		textAreaResponse.append(String.format("%s > %s\n",
				message.getNick(),
				message.getText()));
	}

	@Override
	public void updateUsers(Message message) {
		textAreaNicks.setText("");
		String[] msg = message.getText().split(" ");
		for (String m : msg) {
			textAreaNicks.append(m + "\n");
		}
	}

	@Override
	public void disconnected() {
		buttonConnect.setText(BUTTON_CONNECT_TEXT);
		textAreaNicks.setText("");
		enableWidgets(true);
		pack();
	}

	@Override
	public void watchdogTimedOut() {
		buttonConnect.setText(BUTTON_CONNECT_TEXT);
		enableWidgets(true);
		pack();
		JOptionPane.showMessageDialog(this,
				"Server does not available",
				"Connect error",
				JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void handlePing(Message message) {
//		Util.sendMessage(Util.PONG, 
//				textFieldServerIp.getText(), 
//				Integer.parseInt(textFieldServerPort.getText()), 
//				textFieldNick.getText(),
//				Util.DUMMY);

	}


}
