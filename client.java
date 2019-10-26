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
import java.util.logging.SimpleFormatter;
import java.io.*;

public class client {
	
	/*********************
	 * Private variables * 
	 *********************/
	private DatagramSocket socket;
	private String emulator = "";
	private int nextseqnum = 0; 
	private int sendBase = 0;
	private int UDP_Port_receiving;
	private int UDP_Port_sending;
	private List<packet> packets = new ArrayList<packet> ();	
	private BufferedWriter ClientACKWriter = null;
	private BufferedWriter ClientSeqnumWriter = null;
	
	/*********************
	 * Private functions * 
	 *********************/
	
	private int incNextSeqnum() {
		nextseqnum = (nextseqnum + 1) % 8;
		
		return nextseqnum;
	}
	
	private int incSendBase() {
		sendBase = (sendBase + 1) % 8;
		
		return sendBase;
	}
	
	private String readFile(String filename) throws IOException {
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
	 
	 private void openSocket()  {
			try {
				socket = new DatagramSocket(UDP_Port_sending);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
	 
	 private void openWriters() {
		 	File ClientACKFile = new File("clientACK.log");
		 	File ClientSeqnumFile = new File("clientseqnum.log");
			try {
				ClientACKWriter = new BufferedWriter(new FileWriter("clientACK.log", false));
				ClientSeqnumWriter = new BufferedWriter(new FileWriter("clientseqnum.log", false));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	 
	 private void writeToClientSeqnum(int seqnum) {
			try {
				ClientSeqnumWriter.write(seqnum + "\n");
				ClientSeqnumWriter.flush();
			} catch (IOException io) {
				io.printStackTrace();
			}
		}
	 
	 private void writeToClientACK(int seqnum) {
			try {
				ClientACKWriter.write(seqnum + "\n");
				ClientACKWriter.flush();
			} catch (IOException io) {
				io.printStackTrace();
			}
		}
	
	/********************
	 * Public functions * 
	 ********************/
	
	 //Constructor
	public client(String emulatorAddress, String emulatorReceivingPort, String emulatorSendingPort, String filename) {
		UDP_Port_receiving = Integer.parseInt(emulatorReceivingPort);
		UDP_Port_sending = Integer.parseInt(emulatorSendingPort);
		emulator = emulatorAddress;
		
		//make a list of packets
		makePackets(filename);
		
		//open peripherals
		openSocket();
		openWriters();
		
	}
	
	public void closeSocket() {
		socket.close();
	}
	
	public void closeWriters() {
		try {
			ClientACKWriter.close();
			ClientSeqnumWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static byte[] toBytes(Object obj) throws IOException {
		ByteArrayOutputStream oSt = new ByteArrayOutputStream();
		ObjectOutputStream ooSt = new ObjectOutputStream(oSt);
		ooSt.writeObject(obj);
		ooSt.flush();
		return oSt.toByteArray();
	}

	public static Object toObject(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream b = new ByteArrayInputStream(bytes);
		ObjectInputStream o = new ObjectInputStream(b);
		return o.readObject();
	}
	
	public void sendPacket(int packetNum) throws UnknownHostException {
		byte[] sendBuf = new byte[125];
		try {
			sendBuf = toBytes(packets.get(packetNum)); 
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		DatagramPacket datagramPacket = new DatagramPacket(sendBuf, sendBuf.length, InetAddress.getByName(emulator), UDP_Port_receiving);
		try {
			socket.send(datagramPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		incNextSeqnum();
		writeToClientSeqnum(packets.get(packetNum).getSeqNum());
	}
	
	public int receiveACK() {
		byte[] rcvBuf = new byte[125];
		DatagramPacket DataACK = new DatagramPacket(rcvBuf, rcvBuf.length);
		try {
			socket.receive(DataACK);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		packet ACK = null;
		try {
			ACK = (packet) toObject(DataACK.getData());
			if (ACK.getSeqNum() == sendBase) {
				incSendBase();
			}
			
			
			writeToClientACK(ACK.getSeqNum());
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		
		return ACK.getType();
	}
 
	public boolean checkWindow() {
		int difference = nextseqnum - sendBase;
		System.out.println("difference = " + difference + ": " + ((difference != 7) && (difference != -1)));
		
		return (difference != 7) && (difference != -1);
	}
	
	public int windowSize() {
		return nextseqnum - sendBase;
	}
	
	public void makePackets(String filename) {
		List<String> packetData = null;
		try {
			packetData = splitEqually(readFile(filename), 30);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for(int i = 0; i < packetData.size(); i++) {
			packet mypacket = new packet(1, nextseqnum, packetData.get(i).length(), packetData.get(i));
			packets.add(mypacket);
			incNextSeqnum();
		}
		packet mypacket = new packet(3, nextseqnum, 0, null);
		packets.add(mypacket);
		nextseqnum = 0;
		
	}
	
	public static void main(String args[]) {
		boolean end = false;
		boolean VERBOSE = false; 
		if(true /*args[4] != null*/) { //TEMPORARY true, swap to commented TODO
			VERBOSE = false;
		}
		//client myclient = new client(args[0], args[1], args[2], args[3]);
		
		client testClient = new client("localhost", "6000", "6001", "test.txt");
		
		
		if(VERBOSE) {
			System.out.println("opened client\n");
		}
		int packetNum = 0;
		while(!end) {
			try {
				while(testClient.checkWindow() && packetNum < testClient.packets.size()) { 
					if(VERBOSE) {
						System.out.println("window open.");
					}
					testClient.sendPacket(packetNum);
					if(VERBOSE) {
						System.out.println("sent packet: " + packetNum + "\n");
					}
					packetNum++;
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			//wait(45); //TODO figure out this timer thing
			if(VERBOSE) {
				if(!testClient.checkWindow()) {
					System.out.println("window closed.");
				}
				System.out.println("Wait for ACK");
			}
			if(testClient.receiveACK() == 2) { //returns packet.type, which if 2 is EOT
				end = true; 
				if(VERBOSE) {
					System.out.println("received EOTACK\n");
				}
			} else {
				//TODO NOthing for now;
				if(VERBOSE) {
					System.out.println("received ACK\n");
				}
			}	
		} //end while
		
		//out of the while, close writers and socket
		testClient.closeWriters();
		testClient.closeSocket();
		if(VERBOSE) {
			System.out.println("closed socket and writers. Exiting client");
		}
	}
}
