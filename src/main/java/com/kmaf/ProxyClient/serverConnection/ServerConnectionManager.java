package com.kmaf.ProxyClient.serverConnection;

import com.kmaf.ProxyClient.Auth.AuthenticationException;
import com.kmaf.ProxyClient.Auth.Yggdrasil;
import com.kmaf.ProxyClient.protocol.GameState;
import com.kmaf.ProxyClient.protocol.GenericPacket;
import com.kmaf.ProxyClient.protocol.ProtocolX47.Server.incomingPackets.L_LoginSuccess_0x02_PB;
import com.kmaf.ProxyClient.protocol.ProtocolX47.Server.incomingPackets.L_SetCompression_0x03_PB;
import com.kmaf.ProxyClient.protocol.ProtocolX47.Server.incomingPackets.P_SetCompression_0x46_PB;
import com.kmaf.ProxyClient.protocol.ProtocolX47.Server.outgoingPackets.HS_Handshake_0x00_SB;
import com.kmaf.ProxyClient.protocol.ProtocolX47.Server.outgoingPackets.L_LoginStart_0x00_SB;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerConnectionManager {
	
	
	private final Yggdrasil authenticationProvider;
	private final ServerConnection serverConnection;
	private final EncryptionHandler encryptionHandler;
	private final Queue<GenericPacket> outgoingProcessPacket = new ConcurrentLinkedQueue<>();
	private final Queue<GenericPacket> incomingPacketQueue = new ConcurrentLinkedQueue<>();
	private final String serverDomain;
	private final int serverPort;
	private PacketWriter packetWriter;
	private PacketReader packetReader;
	private GameState gameState = GameState.HANDSHAKE;
	
	
	public ServerConnectionManager(String serverDomain, int serverPort, Yggdrasil authenticationProvider) {
		this.serverDomain = serverDomain;
		this.serverPort = serverPort;
		this.authenticationProvider = authenticationProvider;
		serverConnection = new ServerConnection(serverDomain, serverPort);
		encryptionHandler = new EncryptionHandler(this, serverConnection);
	}
	
	public void connect() throws IOException {
		serverConnection.connect();
		encryptionHandler.setStreams();
		packetWriter = new PacketWriter(encryptionHandler, this);
		Thread packetWriterThread = new Thread(packetWriter);
		packetWriterThread.start();
		packetReader = new PacketReader(encryptionHandler, this);
		Thread packetReaderThread = new Thread(packetReader);
		packetReaderThread.start();
		addOutgoingPacket(new HS_Handshake_0x00_SB(serverDomain, serverPort).generateOutgoingPacket());
		addOutgoingPacket(new L_LoginStart_0x00_SB(authenticationProvider.getUsername()).generateOutgoingPacket());
	}
	
	public void processPacket() throws IOException, InterruptedException {
		GenericPacket packet;
		synchronized (outgoingProcessPacket) {
			if (outgoingProcessPacket.isEmpty()) {
				outgoingProcessPacket.wait(50);
				return;
			}
			packet = outgoingProcessPacket.poll();
		}
		switch (gameState) {
			case HANDSHAKE: {
				if (packet.getPacketID() == 0x00) {
					gameState = GameState.LOGIN;
				} else {
					System.out.println("Error: Sent unknown packet as HANDSHAKE");
				}
			}
			break;
			case LOGIN: {
				switch (packet.getPacketID()) {
					case 0x01: {
						encryptionHandler.completeEncryption();
					}
					break;
				}
			}
			break;
		}
	}
	
	public void readIncomingPacket() throws IOException, InterruptedException {
		GenericPacket packet;
		synchronized (incomingPacketQueue) {
			if (incomingPacketQueue.isEmpty()) {
				incomingPacketQueue.wait(50);
				return;
			}
			packet = incomingPacketQueue.poll();
		}
		switch (gameState) {
			case PLAY: {
				if (packet.getPacketID() == 0x46) {
					P_SetCompression_0x46_PB compressionPacket = new P_SetCompression_0x46_PB();
					compressionPacket.readPacket(packet);
					setCompression(compressionPacket.getThreshold());
					resumeReading();
				}
			}
			return;
			case HANDSHAKE: {
				System.out.println("Received Packet while in Handshake state");
			}
			return;
			case LOGIN: {
				switch (packet.getPacketID()) {
					case 0x01: {
						encryptionHandler.handleEncryptionRequest(packet);
					}
					break;
					case 0x02: {
						L_LoginSuccess_0x02_PB loginSuccessPacket = new L_LoginSuccess_0x02_PB();
						loginSuccessPacket.readPacket(packet);
						this.gameState = GameState.PLAY;
						resumeReading();
					}
					break;
					case 0x03: {
						L_SetCompression_0x03_PB compressionPacket = new L_SetCompression_0x03_PB();
						compressionPacket.readPacket(packet);
						setCompression(compressionPacket.getThreshold());
						resumeReading();
					}
				}
			}
		}
	}
	
	public void pauseReading() {
		packetReader.setPauseReading(true);
	}
	
	public void resumeReading() {
		packetReader.setPauseReading(false);
	}
	
	public void login(String hash) throws AuthenticationException, IOException {
		authenticationProvider.login(hash);
	}
	
	public void addOutgoingPacket(GenericPacket packet) {
		packetWriter.addOutgoingPacket(packet);
	}
	
	public void addOutgoingProcessPacket(GenericPacket packet) {
		synchronized (outgoingProcessPacket) {
			outgoingProcessPacket.add(packet);
			outgoingProcessPacket.notify();
		}
	}
	
	public void setCompression(int threshold) {
		packetReader.setCompressionThreshold(threshold);
		packetWriter.setCompressionThreshold(threshold);
	}
	
	public void addIncomingPacket(GenericPacket packet) {
		switch (gameState) {
			case PLAY: {
				if (packet.getPacketID() == 0x46) {
					pauseReading();
				}
			}
			break;
			case LOGIN: {
				switch (packet.getPacketID()) {
					case 0x01:
					case 0x02:
					case 0x03: {
						pauseReading();
					}
					break;
				}
			}
			break;
		}
		incomingPacketQueue.add(packet);
	}
}
