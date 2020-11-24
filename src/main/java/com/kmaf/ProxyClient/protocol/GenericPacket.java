package com.kmaf.ProxyClient.protocol;

public class GenericPacket {
	
	private final int packetID;
	private final byte[] data;
	
	public GenericPacket(int packetID, byte[] data) {
		this.packetID = packetID;
		this.data = data;
	}
	
	public int getPacketID() {
		return packetID;
	}
	
	public byte[] getData() {
		return data;
	}
}
