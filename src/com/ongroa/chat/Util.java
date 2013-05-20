package com.ongroa.chat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Contains useful methods and contants to both the chat server and the client.
 */
public class Util {

// Message types
	final public static String CONNECT = "CONNECT";
	final public static String DISCONNECT = "DISCONNECT";
	final public static String MESSAGE = "MESSAGE";
	final public static String USERS = "USERS";
	final public static String DUMMY = "__DUMMY__";
	final public static String SYSTEM = "__SYSTEM__";
	
	final public static String RESPONSE_300_USED_NICK = "300";
	final public static String RESPONSE_301_USED_IP_PORT_PAIR = "301";
	final public static String RESPONSE_200_OK = "200";

	final public static int MTU = 1500;
	
// Default port. If it is used server will die, client will choose the next one
	final public static int port = 2000;


	/**
	 * Listens in port port for timeout milliseconds. Returns the received
	 * message.
	 * <p>
	 * @param port Listening port
	 * @param timeout Listens timeout milliseconds. If zero, forever.
	 * @return null or received message
	 */
	public static String listen(int port, int timeout) {
		String ret = null;
		DatagramPacket p = null;
		DatagramSocket s = null;
		try {
			byte[] message = new byte[Util.MTU];
			p = new DatagramPacket(message, message.length);
			s = new DatagramSocket(port);
			s.setSoTimeout(timeout);
			try {
				s.receive(p);
				String text = new String(message, 0, p.getLength());
				return text;
			} catch (SocketTimeoutException e) {
			}
		}
		catch (UnknownHostException e) {
			System.err.println("Unknown host! " + e.getMessage());
		}
		catch (IOException e) {
			System.err.println("Sending failed! " + e.getMessage() +
					 " port: " + port);
		} finally {
			if (s != null)
				s.close();
		}
		return ret;
	}

	/**
	 * Sends a message.
	 * <p>
	 * @param type Type of the message
	 * @param IpAddress Source address
	 * @param port Source port
	 * @param nick Nick name of the client
	 * @param text Sent message
	 */
	public static void sendMessage(
			String type, 
			String IpAddress, 
			int port, 
			String nick, 
			String text) {
		DatagramSocket s = null;
		try {
			String msg = String.format("%s %s %d %s %s", 
					type, 
					IpAddress, 
					port, 
					nick, 
					text);
			byte[] message = msg.getBytes();
			s = new DatagramSocket();
			DatagramPacket p = new DatagramPacket(
					message,
					message.length,
					InetAddress.getByName(IpAddress),
					port);
			s.send(p);
		} catch (UnknownHostException e) {
			System.err.println("Unknown host! " + e.getMessage());
		} catch (SocketException e) {
			System.err.println("UDP port error! " + e.getMessage());
		} catch (IOException e) {
			System.err.println("Sending not succeded! " + e.getMessage());
		} finally {
			if (s != null) {
				s.close();
			}
		}
	}

	public static String getIPv4InetAddress() throws SocketException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.
				getNetworkInterfaces();
		while (interfaces.hasMoreElements()){
			NetworkInterface current = interfaces.nextElement();
			if (!current.isUp() || 
					current.isLoopback() || 
					current.isVirtual()) continue;
			Enumeration<InetAddress> addresses = current.getInetAddresses();
			while (addresses.hasMoreElements()){
				InetAddress current_addr = addresses.nextElement();
				if (current_addr.isLoopbackAddress() || 
						current_addr instanceof Inet6Address) continue;
				return current_addr.getHostAddress();
			}
		}
		return null;
	}

}
