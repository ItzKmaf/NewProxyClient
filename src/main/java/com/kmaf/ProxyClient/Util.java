package com.kmaf.ProxyClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Util {
	
	public static int varIntLength(int varInt) {
		int size = 0;
		while (true) {
			size++;
			if ((varInt & 0xFFFFFF80) == 0)
				return size;
			varInt >>>= 7;
		}
	}
	
	public static void writeVarInt(int value, DataOutputStream out) throws IOException {
		do {
			byte temp = (byte) (value & 0b01111111);
			// Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
			value >>>= 7;
			if (value != 0) {
				temp |= 0b10000000;
			}
			out.writeByte(temp);
		} while (value != 0);
	}
	
	public static int readVarInt(DataInputStream in) throws IOException {
		int numRead = 0;
		int result = 0;
		byte read;
		do {
			read = in.readByte();
			int value = (read & 0b01111111);
			result |= (value << (7 * numRead));
			
			numRead++;
			if (numRead > 5) {
				throw new RuntimeException("VarInt is too big");
			}
		} while ((read & 0b10000000) != 0);
		
		return result;
	}
	
	public static void writeString(String string, DataOutputStream out) throws IOException {
		writeVarInt(string.length(), out);
		out.write(string.getBytes(StandardCharsets.UTF_8));
	}
	
	public static String readStringAscii(DataInputStream in) throws IOException {
		int length = readVarInt(in);
		byte[] data = new byte[length];
		in.readFully(data);
		return new String(data, StandardCharsets.US_ASCII);
	}
	
	public static byte[] readVarByteArray(DataInputStream in, int length) throws IOException {
		if (length <= 1)
			throw new IOException("Invalid array length");
		byte[] data = new byte[length];
		in.read(data);
		return data;
	}
	
	public static void writeVarByteArray(byte[] data, int length, DataOutputStream out) throws IOException {
		writeVarInt(length, out);
		out.write(data);
	}
	
	public static byte[] compress(byte[] bytesToCompress) {
		Deflater deflater = new Deflater();
		deflater.setInput(bytesToCompress);
		deflater.finish();
		
		byte[] bytesCompressed = new byte[Short.MAX_VALUE];
		
		int numberOfBytesAfterCompression = deflater.deflate(bytesCompressed);
		
		byte[] returnValues = new byte[numberOfBytesAfterCompression];
		
		System.arraycopy
				(
						bytesCompressed,
						0,
						returnValues,
						0,
						numberOfBytesAfterCompression
				);
		
		return returnValues;
	}
	
	public static byte[] decompress(byte[] bytesToDecompress) {
		byte[] returnValues = null;
		
		Inflater inflater = new Inflater();
		
		int numberOfBytesToDecompress = bytesToDecompress.length;
		
		inflater.setInput
				(
						bytesToDecompress,
						0,
						numberOfBytesToDecompress
				);
		
		int bufferSizeInBytes = numberOfBytesToDecompress;
		
		int numberOfBytesDecompressedSoFar = 0;
		List<Byte> bytesDecompressedSoFar = new ArrayList<Byte>();
		
		try {
			while (!inflater.needsInput()) {
				byte[] bytesDecompressedBuffer = new byte[bufferSizeInBytes];
				
				int numberOfBytesDecompressedThisTime = inflater.inflate
						(
								bytesDecompressedBuffer
						);
				
				numberOfBytesDecompressedSoFar += numberOfBytesDecompressedThisTime;
				
				for (int b = 0; b < numberOfBytesDecompressedThisTime; b++) {
					bytesDecompressedSoFar.add(bytesDecompressedBuffer[b]);
				}
			}
			
			returnValues = new byte[bytesDecompressedSoFar.size()];
			for (int b = 0; b < returnValues.length; b++) {
				returnValues[b] = bytesDecompressedSoFar.get(b);
			}
			
		} catch (DataFormatException dfe) {
			dfe.printStackTrace();
		}
		
		inflater.end();
		
		return returnValues;
	}
	
	public static String byteArrayString(byte[] data) {
		StringBuilder buffer = new StringBuilder();
		for (byte b : data) {
			if (buffer.length() == 0)
				buffer.append("new byte[] { ");
			else
				buffer.append(", ");
			buffer.append("0x");
			if (b <= 0xF)
				buffer.append(0);
			buffer.append(Integer.toHexString(b & 0xFF).toUpperCase());
		}
		return buffer.append(" };").toString();
	}
}
