package com.kmaf.ProxyClient.serverConnection;

import com.kmaf.ProxyClient.Util;
import com.kmaf.ProxyClient.protocol.GenericPacket;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class PacketReader implements Runnable {
	private final EncryptionHandler handler;
	private final ServerConnectionManager manager;
	private final AtomicBoolean pauseReading = new AtomicBoolean(false);
	private int compressionThreshold = -1;
	
	public PacketReader(EncryptionHandler handler, ServerConnectionManager manager) {
		this.handler = handler;
		this.manager = manager;
	}
	
	@Override
	public void run() {
		GenericPacket currentPacket;
		try {
			while (compressionThreshold >= -1) {
				synchronized (pauseReading) {
					if (pauseReading.get()) {
						pauseReading.wait(50);
					}
				}
				if (handler.getInputStream().available() <= 0) {
					Thread.sleep(50);
					continue;
				}
				if (compressionThreshold == -1) {
					/*
					|============================================================================|
					|Field Name  |Field Type   |Notes                                            |
					|------------|-------------|-------------------------------------------------|
					|Length      |VarInt       |Length of packet data + length of the packet ID  |
					| - - - - - -|- - - - - - -|- - - - - - - - - - - - - - - - - - - - -- - - - |
					|Packet ID   |VarInt       |                                                 |
					| - - - - - -|- - - - - - -|- - - - - - - - - - - - - - - - - - - - -- - - - |
					|Data        |Byte Array   |Depends on the connection state and packet ID    |
					|============================================================================|
					 */
					int length = Util.readVarInt(handler.getInputStream());
					int packetID = Util.readVarInt(handler.getInputStream());
					byte[] data = new byte[length - Util.varIntLength(packetID)];
					handler.getInputStream().readFully(data);
					currentPacket = new GenericPacket(packetID, data);
				} else {
					int packetLength = Util.readVarInt(handler.getInputStream());
					int dataLength = Util.readVarInt(handler.getInputStream());
					if (dataLength == 0) {
						/*
						|============================================================================================================|
						|Compressed? |Field Name    |Field Type   |Notes                                                             |
						|------------|--------------|-------------|------------------------------------------------------------------|
						|No          |Packet Length |VarInt       |Length of Data Length + compressed length of (Packet ID + Data)   |
						|- - - - - - | - - - - - - -|- - - - - - -|- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - |
						|No          |Data Length   |VarInt       |0                                                                 |
						|- - - - - - | - - - - - - -|- - - - - - -|- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - |
						|No          |Packet ID     |VarInt       |                                                                  |
						|            | - - - - - - -|- - - - - - -|- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - |
						|            |Data          | Byte Array  |Depends on the connection state and packet ID                     |
						|============================================================================================================|
						 */
						int packetID = Util.readVarInt(handler.getInputStream());
						byte[] data = new byte[packetLength - 1 - Util.varIntLength(packetID)];
						handler.getInputStream().readFully(data);
						currentPacket = new GenericPacket(packetID, data);
					} else {
						/*
						|============================================================================================================|
						|Compressed? |Field Name    |Field Type   |Notes                                                             |
						|------------|--------------|-------------|------------------------------------------------------------------|
						|No          |Packet Length |VarInt       |Length of Data Length + compressed length of (Packet ID + Data)   |
						|- - - - - - | - - - - - - -|- - - - - - -|- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - |
						|No          |Data Length   |VarInt       |Length of uncompressed (Packet ID + Data) or 0                    |
						|- - - - - - | - - - - - - -|- - - - - - -|- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - |
						|Yes         |Packet ID     |VarInt       |                                                                  |
						|            | - - - - - - -|- - - - - - -|- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - |
						|            |Data          | Byte Array  |Depends on the connection state and packet ID                     |
						|============================================================================================================|
						 */
						byte[] compressed = new byte[packetLength - Util.varIntLength(dataLength)];
						handler.getInputStream().readFully(compressed);
						byte[] decompressed = Util.decompress(compressed);
						DataInputStream in = new DataInputStream(new ByteArrayInputStream(decompressed));
						int packetID = Util.readVarInt(in);
						byte[] data = new byte[dataLength - Util.varIntLength(packetID)];
						in.readFully(data);
						in.close();
						currentPacket = new GenericPacket(packetID, data);
					}
				}
				System.out.println("[Network]: Received packet with ID: 0x" + Integer.toHexString(currentPacket.getPacketID()) + ", Length: " + currentPacket.getData().length);
				manager.addIncomingPacket(currentPacket);
			}
		} catch (InterruptedException | IOException e) {
			compressionThreshold = -2;
			e.printStackTrace();
		}
	}
	
	public void setPauseReading(boolean value) {
		synchronized (pauseReading) {
			pauseReading.set(value);
		}
	}
	
	public void setCompressionThreshold(int compressionThreshold) {
		this.compressionThreshold = compressionThreshold;
	}
}
