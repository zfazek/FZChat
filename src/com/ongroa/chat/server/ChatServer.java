package com.ongroa.chat.server;

import java.util.List;
import java.util.Vector;

import javax.swing.SwingWorker;

import com.ongroa.chat.Host;
import com.ongroa.chat.Message;
import com.ongroa.chat.Util;

/**
 * Simple chat server.
 */
public class ChatServer {

	/**
	 * Timeout in milliseconds of listening the network interface.
	 * 0 means forever. 
	 */
	private static final int TIMEOUT = 0;

	/**
	 * Storing the own host related parameters.
	 * Eg. Ip address, port.
	 */
	private Host mHost;

	/**
	 * List for storing the nick names of the clients.
	 * Nick names should be unique.
	 */
	private Vector<String> mNicks = new Vector<String>();

	/**
	 * List for storing the clients.
	 */
	private Vector<Host> mClients = new Vector<Host>();

	ChatServer() {
		mHost = new Host();
		mHost.setTimeout(TIMEOUT);
	}

	public Host getHost() {
		return mHost;
	}

	public void setHost(Host host) {
		this.mHost = host;
	}

	/**
	 * Starts the background thread that does the listening.
	 */
	private void start() {
		ServerWorker serverWorker = new ServerWorker();
		serverWorker.execute();
		while (true) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
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
	private class ServerWorker extends SwingWorker<Void, String> {

		@Override
		protected Void doInBackground() {
			do {
				String text = Util.listen(mHost.getPort(), mHost.getTimeout());
				if (text != null) {
					System.out.println("Server receives: " + text); 
					publish(text);
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
	 * Parses the incoming messages. Calls the appropriate handle methods.
	 * <p>
	 * @param text Message received
	 */
	private void parseMessage(String text) {
		Message message = new Message(text);
		if (message.getType().equals(Util.CONNECT)) {
			serverHandleConnect(message);
		}
		if (message.getType().equals(Util.DISCONNECT)) {
			serverHandleDisonnect(message);
		}
		if (message.getType().equals(Util.MESSAGE)) {
			serverHandleMessage(message);
		}
	}

	/**
	 * Handles the incoming MESSAGE type message.
	 * <p>
	 * Distributes the message to all the registered clients.
	 * @param message Incoming message
	 */
	private void serverHandleMessage(Message message) {
		String nick = message.getNick();
		String msg = message.getText();
		for (Host client : mClients) {
			Util.sendMessage(Util.MESSAGE, client.getIpAddress(), 
					client.getPort(), nick, msg);
		}
	}

	/**
	 * Handles the incoming DISCONNECT type message.
	 * <p>
	 * Deregisters the client and the nick.
	 * Sends 200 OK back to the client.
	 * Broadcasts the list of registered users to all the clients.
	 *  
	 * @param message Incoming message
	 */
	private void serverHandleDisonnect(Message message) {
		String localIp = message.getIpAddress();
		int localPort = message.getPort();
		String nick = message.getNick();
		Host host = new Host(localIp, localPort, nick);
		if (mClients.contains(host)) {
			mClients.remove(host);
			mNicks.remove(nick);
			printClients();
			Util.sendMessage(Util.RESPONSE_200_OK, localIp, localPort, 
					nick, Util.DUMMY);
			broadcastMessageUsers();
		}
	}

	/**
	 * Handles the incoming CONNECT type message.
	 * <p>
	 * Registers new client if client and nick is unique. Sends back 200 OK to
	 * the client. Broadcasts the list of registered users to all the clients.
	 * If client or nick is not unique sends back the proper error response.
	 * 
	 * @param message Incoming message
	 */
	private void serverHandleConnect(Message message) {
		String localIp = message.getIpAddress();
		int localPort = message.getPort();
		String nick = message.getNick();
		Host host = new Host(localIp, localPort, nick);
		if (mNicks.contains(nick)) {
			Util.sendMessage(Util.RESPONSE_300_USED_NICK, 
					localIp, localPort, nick, Util.DUMMY);
		} else if (mClients.contains(host)) {
			Util.sendMessage(Util.RESPONSE_301_USED_IP_PORT_PAIR, 
					localIp, localPort, nick, Util.DUMMY);
		} else if (! mNicks.contains(nick) && ! mClients.contains(host) ) {
			mClients.add(host);
			mNicks.add(nick);
			printClients();
			Util.sendMessage(Util.RESPONSE_200_OK, localIp, localPort, 
					nick, Util.DUMMY);
			broadcastMessageUsers();
		}
	}

	/**
	 * Broadcasts the list of registered users to all the clients.
	 * <p>
	 * It waits for a while in order not to be too fast for the clients.
	 * It is needed because of UDP protocol.
	 */
	private void broadcastMessageUsers() {
		String msg = "";
		for (Host client : mClients) {
			msg += client.getNick();
			msg += " ";
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
		for (Host client : mClients) {
			Util.sendMessage(Util.USERS, client.getIpAddress(), 
					client.getPort(), Util.SYSTEM, msg);
			System.out.println("Server sends USERS:" + msg);
		}
	}

	/**
	 * Prints the clients to the console.
	 */
	private void printClients() {
		System.out.println("Clients:");
		for (Host client : mClients) {
			System.out.format("\t%s %d %s\n", 
					client.getIpAddress(), client.getPort(), 
					client.getNick());
		}
	}

	/**
	 * Prints the usage and quits.
	 */
	private static void usage() {
		System.out.println("Usage: java ChatServer port_number");
		System.exit(-1);
	}

	/**
	 * Starts the program.
	 * <p>
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		ChatServer server = new ChatServer();
		if (args.length == 0) {
			server.getHost().setPort(Util.port);
		} else if (args.length > 1) {
			usage();
		} else {
			try {
				server.getHost().setPort(Integer.parseInt(args[0]));
			}
			catch (NumberFormatException e) {
				usage();
			}
		}
		server.start();
	}

}
