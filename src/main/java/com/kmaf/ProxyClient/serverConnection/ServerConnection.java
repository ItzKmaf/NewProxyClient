package com.kmaf.ProxyClient.serverConnection;

import sun.plugin.dom.exception.InvalidStateException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class ServerConnection {
	
	private final SocketAddress socketAddress;
	private final Socket socket;
	
	public ServerConnection(String serverDomain, int serverPort) {
		socketAddress = new InetSocketAddress(serverDomain, serverPort);
		socket = new Socket();
	}
	
	public void connect() throws IOException {
		if (socket.isConnected()) {
			throw new SocketException("Socket is already connected");
		} else {
			try {
				socket.connect(socketAddress, 3000);
				socket.setSoTimeout(15000);
				System.out.println("Connected Socket to: " + socket.getInetAddress().toString() + ":" + socket.getPort());
			} catch (IOException e) {
				throw new IOException("Server Connection: Socket was already connected", e);
			}
		}
	}
	
	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}
	
	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}
	
	public void close() throws InvalidStateException, IOException {
		if (socket == null || socket.isClosed()) {
			throw new InvalidStateException("Socket was already closed");
		} else {
			try {
				socket.close();
			} catch (IOException e) {
				throw new IOException("Server Connection: Error closing Socket: " + e);
			}
		}
	}
}