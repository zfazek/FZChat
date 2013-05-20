package com.ongroa.chat;


public class Host {
	
	private String mIpAddress;
	private int mPort;
	private String mNick;
	private int mTimeout;

	public Host() {
	}
	
	public Host(String ipAddress, int port, String nick) {
		mIpAddress = ipAddress;
		mPort = port;
		mNick = nick;
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

	public int getTimeout() {
		return mTimeout;
	}

	public void setTimeout(int timeout) {
		this.mTimeout = timeout;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		if (! (obj instanceof Host)) return false;
		Host o = (Host)obj;
		return o.mIpAddress.equals(this.mIpAddress) &&
				o.mPort == this.mPort;
	}
}
