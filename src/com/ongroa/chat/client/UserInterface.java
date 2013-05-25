package com.ongroa.chat.client;

import com.ongroa.chat.Message;

/**
 * Java Interface of the ChatClient GUI.
 */
public interface UserInterface {

	void conneced();

	void usedNick();

	void usedIpPortPair();

	void newMessageReceived(Message message);

	void updateUsers(Message message);

	void disconnected();

	String getNick();

	void setNick(String nick);

	void watchdogTimedOut();

	void handlePing(Message message);

}
