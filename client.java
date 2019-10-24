/************************
 *                      *
 * Andrew Mehta am3258  *
 * Jake Manning jsm652  *
 *                      *
 ************************/
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.io.*;

public class client {
	
	/*********************
	 * Private variables * 
	 *********************/
	private DatagramSocket socket;
	private byte[] = new byte[256];
	private int expectedACK; 
	private int sendBase;
	private int UDP_Port_receiving;
	private int UDP_Port_sending;
	private List<packet> packets;
	
	/*********************
	 * Private functions * 
	 *********************/
	
	private int incExpectedACK() {
		expectedACK = (expectedACK + 1) % 8;
		
		return expectedACK;
	}
	
	private int incSendBase() {
		sendBase = (sendBase + 1) % 8;
		
		return sendBase;
	}
	
	private String readFile(String filename) {
		String data = "";
		data = new String (Files.readAllBytes(Paths.get(filename)));
		
		return data;
	}
	
	 private static List<String> splitEqually(String text, int size) {
	        List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);

	        for (int start = 0; start < text.length(); start += size) {
	            ret.add(text.substring(start, Math.min(text.length(), start + size)));
	        }

	        return ret;
	    }
	
	/********************
	 * Public functions * 
	 ********************/
	
	 //Constructor
	public client(String emulatorAddress, String emulatorReceivingPort, String emulatorSendingPort, String filename) {
		UDP_Port_receiving = Integer.parseInt(emulatorReceivingPort);
		UDP_Port_sending = Integer.parseInt(emulatorSendingPort);
		
		packets = makePackets(filename);
	}
	
	public static byte[] toBytes(Object obj) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream o = new ObjectOutputStream(b);
		o.writeObject(obj);
		return b.toByteArray();
	}

	public static Object toObject(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream b = new ByteArrayInputStream(bytes);
		ObjectInputStream o = new ObjectInputStream(b);
		return o.readObject();
	}
	
	public void sendPacket() {
		checkWindow();
		
		
	}
 
	public boolean checkWindow() {
		boolean ret = false;
		if(expectedACK - sendBase < 7) {
			ret = true;
		}
		
		return ret;
	}
	public List<packet> makePackets(String filename) {
		List<String> packetData = splitEqually(readFile(filename), 30);
		
		List<packet> packetList;
		for(int i = 0; i < packetData.size(); i++) {
			packet mypacket = new packet(1, expectedACK, packetData.get(i).length(), packetData.get(i));
			packetList.add(mypacket);
			incExpectedACK();
		}
		packet mypacket = new packet(3, expectedACK, 0, null);
		packetList.add(mypacket);
		expectedACK = 0;
		
		return packetList;
	}
	public static void main(String args[]) {
		
		//client myclient = new client(args[0], args[1], args[2], args[3]);
		
		client testClient = new client("localhost", "6000", "6001", "test.txt");
		if(testClient.checkWindow()) {
			testClient.sendPacket();
		}
	}
}
