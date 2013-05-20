package com.ongroa.chat;

public class Message {

	private String mType;
	private String mIpAddress;
	private int mPort;
	private String mNick;
	private String mText;

	public Message(String text) {
		mType = "";
		mIpAddress = "";
		mPort = 0;
		mNick = "";
		mText = "";
		String[] m = text.split(" ");
		if (m.length >= 5) {
			mType = m[0];
			mIpAddress = m[1];
			try {
				mPort = Integer.parseInt(m[2]);
			} catch (NumberFormatException e1) {
				mPort = 0;
			}
			mNick = m[3];
			String msg = "";
			for (int i = 4; i < m.length; i++) {
				if (i < m.length - 1) msg += m[i] + " ";
				else msg += m[i];
			}
			mText = msg;
		}
	}

	public String getIpAddress() {
		return mIpAddress;
	}

	public void setIpAddress(String ipAddress) {
		mIpAddress = ipAddress;
	}

	public int getPort() {
		return mPort;
	}

	public void setPort(int port) {
		this.mPort = port;
	}

	public String getNick() {
		return mNick;
	}

	public void setNick(String nick) {
		this.mNick = nick;
	}

	public String getType() {
		return mType;
	}

	public void setType(String mType) {
		this.mType = mType;
	}

	public String getText() {
		return mText;
	}

	public void setText(String mText) {
		this.mText = mText;
	}

}
