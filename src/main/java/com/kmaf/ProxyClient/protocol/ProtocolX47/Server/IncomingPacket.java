package com.kmaf.ProxyClient.protocol.ProtocolX47.Server;

import com.kmaf.ProxyClient.protocol.GenericPacket;

import java.io.IOException;

public interface IncomingPacket {
	
	void readPacket(GenericPacket packet) throws IOException;
}
