package com.kmaf.ProxyClient.serverConnection;

import com.kmaf.ProxyClient.Util;
import com.kmaf.ProxyClient.protocol.GenericPacket;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PacketWriter implements Runnable {
	
	private final Queue<GenericPacket> outgoingPacketQueue = new ConcurrentLinkedQueue<>();
	private final ServerConnectionManager manager;
	private final AtomicBoolean pauseWriting = new AtomicBoolean(false);
	private final EncryptionHandler handler;
	private int compressionThreshold = -1;
	
	public PacketWriter(EncryptionHandler handler, ServerConnectionManager manager) {
		this.handler = handler;
		this.manager = manager;
	}
	
	@Override
	public void run() {
		GenericPacket currentPacket;
		int dataLength;
		try {
			while (compressionThreshold >= -1) {
				synchronized (pauseWriting) {
					if (pauseWriting.get()) {
						pauseWriting.wait(50);
					}
				}
				synchronized (outgoingPacketQueue) {
					currentPacket = outgoingPacketQueue.poll();
					if (currentPacket == null) {
						outgoingPacketQueue.wait(500);
						continue;
					}
				}
				dataLength = Util.varIntLength(currentPacket.getPacketID()) + currentPacket.getData().length;
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
					Util.writeVarInt(dataLength, handler.getOutputStream()); // Length
					Util.writeVarInt(currentPacket.getPacketID(), handler.getOutputStream()); // Packet ID
					handler.getOutputStream().write(currentPacket.getData()); // Packet Data
				} else {
					if (compressionThreshold > dataLength) {
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
						Util.writeVarInt(1 + dataLength, handler.getOutputStream()); // Packet Length (Length of PacketID + Data + Length of Data Length (Which is 1 as it is zero)
						Util.writeVarInt(0, handler.getOutputStream()); // Data Length which is zero as the packet has not been compressed
						Util.writeVarInt(currentPacket.getPacketID(), handler.getOutputStream()); // Packet ID
						handler.getOutputStream().write(currentPacket.getData()); // Packet Data
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
						ByteArrayOutputStream dataAndIDStream = new ByteArrayOutputStream();
						DataOutputStream temp = new DataOutputStream(dataAndIDStream);
						Util.writeVarInt(currentPacket.getPacketID(), temp);
						temp.write(currentPacket.getData());
						temp.close();
						byte[] compressed = Util.compress(dataAndIDStream.toByteArray());
						Util.writeVarInt(Util.varIntLength(dataLength) + compressed.length, handler.getOutputStream()); //Packet Length
						Util.writeVarInt(dataLength, handler.getOutputStream()); // Data Length
						handler.getOutputStream().write(compressed); //Compressed PacketID and Data Length
					}
				}
				handler.getOutputStream().flush();
				System.out.println("[Network]: Sent packet with ID: 0x" + Integer.toHexString(currentPacket.getPacketID()) + ", Length: " + currentPacket.getData().length);
				manager.addOutgoingProcessPacket(currentPacket);
			}
		} catch (InterruptedException | IOException e) {
			compressionThreshold = -2;
			e.printStackTrace();
		}
	}
	
	public void setCompressionThreshold(int compressionThreshold) {
		this.compressionThreshold = compressionThreshold;
	}
	
	public void addOutgoingPacket(GenericPacket packet) {
		synchronized (outgoingPacketQueue) {
			outgoingPacketQueue.add(packet);
			outgoingPacketQueue.notify();
		}
	}
	
	public void setPauseWriting(boolean value) {
		synchronized (pauseWriting) {
			pauseWriting.set(value);
		}
	}
}
