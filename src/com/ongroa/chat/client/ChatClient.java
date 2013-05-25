package com.ongroa.chat.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.ongroa.chat.Host;
import com.ongroa.chat.Message;
import com.ongroa.chat.Util;

public class ChatClient  {

	static final int IDLE = 0;
	static final int CONNECTING = 1;
	static final int CONNECTED = 2;
	static final int TERMINATING = 3;
	private int mState;

	private Timer mTimerWatchdog;
	
	private static final int TIMEOUT = 0;

	private static int WATCHDOG_TIMEOUT = 2000;
	
	private Host mClient;

	private ClientWorker mClientWorker;

	private UserInterface mUserInterface;

	public ChatClient() throws SocketException {
		mState = IDLE;
		mClient = new Host();
		mClient.setIpAddress(Util.getIPv4InetAddress());
		mClient.setPort(Util.port);
		mClient.setTimeout(TIMEOUT);
		mClientWorker = new ClientWorker();
		mClientWorker.execute();
		mUserInterface = new UiClientSwing(this);
	}

	public Host getClient() {
		return mClient;
	}

	public void setClient(Host client) {
		this.mClient = client;
	}

	public ClientWorker getClientWorker() {
		return mClientWorker;
	}

	public void setClientWorker(ClientWorker clientWorker) {
		this.mClientWorker = clientWorker;
	}

	public UserInterface getUserInterface() {
		return mUserInterface;
	}

		public void setUserInterface(UserInterface userInterface) {
			this.mUserInterface = userInterface;
		}

	public int getState() {
		return mState;
	}

	public void setState(int state) {
		this.mState = state;
	}

	public Timer getTimer() {
		return mTimerWatchdog;
	}

	public void setTimer(Timer mTimer) {
		this.mTimerWatchdog = mTimer;
	}

	/**
	 * Sends Util.CONNECT or Util.DISCONNECT
	 * <p>
	 * @param con sends Util.CONNECT if true, sends Util.DISCONNECT otherwise
	 * @param ipAddress Server address
	 * @param port Server port
	 * @param nick Nick name
	 * @return success
	 */
	public boolean connect(boolean con, String ipAddress, String port, String nick) {
		boolean succ = false;
		int server_port = 0;
		DatagramSocket s = null;
		try {
			server_port = Integer.parseInt(port);
			InetAddress ip_address = InetAddress.getByName(
					ipAddress);
			mUserInterface.setNick(nick);
			if (server_port > 1024 && server_port < 32678 && 
					! ipAddress.equals("") && 
					! mClient.getIpAddress().equals("") && 
					mClient.getPort() > 1024 && 
					mClient.getPort() < 32768 && 
					! nick.equals("")) {
				String cmd = "";
				if (con) cmd = Util.CONNECT;
				else cmd = Util.DISCONNECT;
				String msg = String.format("%s %s %s %s %s", 
						cmd, 
						mClient.getIpAddress(), 
						mClient.getPort(), 
						nick, 
						Util.DUMMY);
				byte[] message = msg.getBytes();
				s = new DatagramSocket();
				DatagramPacket p = new DatagramPacket(
						message,
						message.length,
						ip_address,
						server_port);
				s.send(p);
				setWatchdog();
				succ = true;
			}
		}
		catch (UnknownHostException e) {
			System.err.println("Unknown host! " + e.getMessage());
		}
		catch (NumberFormatException e) {
		}
		catch (SocketException e) {
			System.err.println("UDP port error! " + e.getMessage());
		}
		catch (IOException e) {
			System.err.println("Sending not succeded! " + e.getMessage());
		}
		finally {
			if (s != null)
				s.close();
		}
		return succ;
	}

	/**
	 * Sets watchdog timer. It is meant to be used after every message
	 * sending. The purpose of this timer is to handle the situation when
	 * no response arrives.
	 */
	public void setWatchdog() {
		mTimerWatchdog = new Timer();
		mTimerWatchdog.schedule(new WatchdogTask(), WATCHDOG_TIMEOUT);
	}
	
	/**
	 * Cancels watchdog timer. It is meant to be used it the answer arrives.
	 */
	public void cancelWatchdog() {
		mTimerWatchdog.cancel();
	}

	/**
	 * Sends a message.
	 * <p>
	 * @param type Type of the message 
	 * @param ip IP address of the server
	 * @param port Port of the server
	 * @param nick Nick name of the user
	 * @param msg Message to be sent
	 */
	public void sendMessage(String type, String ip, int port,
			String nick, String msg) {
		Util.sendMessage(type, ip, port, nick, msg);
		setWatchdog();
	}

	/**
	 * Parses messages coming from server.
	 * <p>
	 * It Handles incoming messages according to the state machine.
	 * <p>
	 * @param text Message coming from server.
	 */
	private void parseMessage(String text) {
		if (text != null) {
			System.out.format("Client receives: %s, mState: %d\n", text, mState);
			Message message = new Message(text);
			switch (mState) {
			case IDLE:
				break;
			case CONNECTING:
				if (message.getType().equals(Util.RESPONSE_200_OK)) {
					cancelWatchdog();
					mUserInterface.conneced();
					mState = CONNECTED;
				}
				if (message.getType().equals(Util.RESPONSE_300_USED_NICK)) {
					cancelWatchdog();
					mUserInterface.usedNick();
					mState = IDLE;
				}
				if (message.getType().equals(Util.RESPONSE_301_USED_IP_PORT_PAIR)) {
					cancelWatchdog();
					mUserInterface.usedIpPortPair();
					mState = IDLE;
				}
				break;
			case CONNECTED: 
				if (message.getType().equals(Util.MESSAGE)) {
					cancelWatchdog();
					mUserInterface.newMessageReceived(message);
				}
				if (message.getType().equals(Util.USERS)) {
					mUserInterface.updateUsers(message);
				}
				if (message.getType().equals(Util.PING)) {
					mUserInterface.handlePing(message);
				}
				break;
			case TERMINATING: 
				if (message.getType().equals(Util.RESPONSE_200_OK)) {
					cancelWatchdog();
					mUserInterface.disconnected();
					mState = IDLE;
				}
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Worker thread, does the port listening.
	 * <p>
	 * If new message arrives it publishes the message to the process() 
	 * method of the main thread.
	 * If listening fails it increases the port number and tries again.
	 */
	private class ClientWorker extends SwingWorker<Void, String> {

		@Override
		protected Void doInBackground() {
			do {
				String text = Util.listen(mClient.getPort(), 
						mClient.getTimeout());
				if (text != null) {
					publish(text);
				} else {
					mClient.setPort(mClient.getPort() + 1);
				}
			} while (!isCancelled());
			return null;
		}

		@Override
		protected void process(List<String> texts) {
			for (String text : texts) {
				parseMessage(text);
			}
		}
	}

	/**
	 * Implements watchdog timer.
	 */
	private class WatchdogTask extends TimerTask {

		@Override
		public void run() {
			mState = IDLE;
			mTimerWatchdog.cancel();
			mUserInterface.watchdogTimedOut();
		}
		
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				try {
					new ChatClient();
				} catch (SocketException e) {
					e.printStackTrace();
				}
			}
		});
	}

}

