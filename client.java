/*
 * Andrew Mehta (amasdf)
 * Jake Manning (jsm652)
 * Data Communication and Networking
 * Programming Assignment #2 : GBN Implementation
 * 10/28/2019
 *
 * Client File
 */
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
	
	//------------------- Private Variable Definitions -------------------//
	
	private DatagramSocket socket;
	private String emulator = "";
	private int nextseqnum = 0; 
	private int sendBase = 0;
	private int UDP_Port_receiving;
	private int UDP_Port_sending;
	private List<packet> packets = new ArrayList<packet> ();	
	private BufferedWriter ClientACKWriter = null;
	private BufferedWriter ClientSeqnumWriter = null;
	
	
	//------------------- Begin Private Function Definitions -------------------//
    
    

	//--- incNextSeqNum - Increments the sequence private variable nextseqnum. This is returned as an interger. 
	private int incNextSeqnum() {
		nextseqnum = (nextseqnum + 1) % 8;
		
		return nextseqnum;
	}
	//--- incSendBase - Increments the private variable sendBase to regulate the sender window. This is returned as an interger.
	private int incSendBase() {
		sendBase = (sendBase + 1) % 8;
		
		return sendBase;
	}
	//--- readFile - Inputs a file name which it then opens and reads in a a block. This is returned as a string. 
	private String readFile(String filename) throws IOException {
		String data = "";
		data = new String (Files.readAllBytes(Paths.get(filename)));
		
		return data;
	}
	//--- splitEqually - Inputs the file data and divides it into the correct packet size (which is 30). This is returned as a list of strings
	 private static List<String> splitEqually(String text, int size) {
	        List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);

	        for (int start = 0; start < text.length(); start += size) {
	            ret.add(text.substring(start, Math.min(text.length(), start + size)));
	        }

	        return ret;
	    }
	//--- openSocket - Opens the UDP socket used for Sending data. No return.
	 private void openSocket()  {
			try {
				socket = new DatagramSocket(UDP_Port_sending);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
	//--- openWrites - Opens the files that are written to log the program. No return.
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
	//--- writeToClientSeqnum - Writes the seqnum received to the log file. No return.
	 private void writeToClientSeqnum(int seqnum) {
			try {
				ClientSeqnumWriter.write(seqnum + "\n");
				ClientSeqnumWriter.flush();
			} catch (IOException io) {
				io.printStackTrace();
			}
		}
	//--- writeToClientACK - Writes the ACK received to the log file. No return. 
	 private void writeToClientACK(int seqnum) {
			try {
				ClientACKWriter.write(seqnum + "\n");
				ClientACKWriter.flush();
			} catch (IOException io) {
				io.printStackTrace();
			}
		}
	
	//------------------- End Private Function Definitions -------------------//
	 
	        
	 
	 
	//------------------- Begin Public Function Definitions -------------------//
	
	 //Class Constructor - has 4 inputs
	public client(String emulatorAddress, String emulatorReceivingPort, String emulatorSendingPort, String filename) {
		UDP_Port_receiving = Integer.parseInt(emulatorReceivingPort);
		UDP_Port_sending = Integer.parseInt(emulatorSendingPort);
		emulator = emulatorAddress;
		
		//make a list of packets
		makePackets(filename);
		
		//open peripheral logs
		openSocket();
		openWriters();
		
	}
	
	public void closeSocket() {
		socket.close();
	}
	//--- closeWriters - Closes the log files that were being written to. No return. 
	public void closeWriters() {
		try {
			ClientACKWriter.close();
			ClientSeqnumWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	//--- toBytes - Serializes the data in preparation to send. Returns the serialized data as a byte. 
	public static byte[] toBytes(Object obj) throws IOException {
		ByteArrayOutputStream oSt = new ByteArrayOutputStream();
		ObjectOutputStream ooSt = new ObjectOutputStream(oSt);
		ooSt.writeObject(obj);
		ooSt.flush();
		return oSt.toByteArray();
	}
	//--- toObject - Deserializes the data after receiving it. Returns the deserialized data as an object. 
	public static Object toObject(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream b = new ByteArrayInputStream(bytes);
		ObjectInputStream o = new ObjectInputStream(b);
		return o.readObject();
	}
	//--- sendPacket - Inputs the packet to be sent and sends it to the destination. No return.  
	public void sendPacket(packet sendPacket) throws UnknownHostException {
		byte[] sendBuf = new byte[125];
		try {
			sendBuf = toBytes(sendPacket); 
		} catch (IOException e) {
			e.printStackTrace();
		}
		DatagramPacket datagramPacket = new DatagramPacket(sendBuf, sendBuf.length, InetAddress.getByName(emulator), UDP_Port_receiving);
		try {
			socket.send(datagramPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		writeToClientSeqnum(sendPacket.getSeqNum());
		incNextSeqnum();
//		assert(nextseqnum == packets.get(packetNum).getSeqNum());
	}
	//--- receiveACK - Receives the ACK from the server and sends it to be deserialzed. The ACK packet is then returned. 
	public packet receiveACK() {
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
			writeToClientACK(ACK.getSeqNum());
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		
		return ACK;
	}
	//--- checkWindow - Checks the sending window to see if it is full. Returns a boolean. 
	public boolean checkWindow() {
		int difference = windowSize();
		
		return (difference != 7) && (difference != -1);
	}
	//--- windowSize - Calculates teh size of the current window. Returns this as an integer. 
	public int windowSize() {
		return nextseqnum - sendBase;
	}
	//--- makePackets - Inputs a filename that is sent to SplitEqually. That returned list is then broken up in a for loop and placed into inividiual packets that are stored in the private variable packet list. 
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
	//--- printPackets - Walks through the private packet list variable and prints the contents of each. No return. 
	public void printPackets() {
		for(int i = 0; i < packets.size(); i++) {
			packets.get(i).printContents();
		}
	}
	//--- main - Writes the ACK received to the log file. No return. 
	public static void main(String args[]) {
		boolean end = false;
		boolean VERBOSE = false; //exists for testing 
		List<packet> sentPackets = new ArrayList<packet> ();
		client testClient = new client(args[0], args[1], args[2], args[3]);
		
		//client testClient = new client("localhost", "6000", "6001", "test.txt");
		
		if(VERBOSE) {
			System.out.println("opened client\n");
		}
		int packetNum = 0;
		while(!end) {
			while(testClient.checkWindow() && packetNum < testClient.packets.size()) { 
				if(VERBOSE) {
					System.out.println("window open.");
				}
				try {
					testClient.sendPacket(testClient.packets.get(packetNum));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				sentPackets.add(testClient.packets.get(packetNum));
				if(VERBOSE) {
						System.out.println("sent packet: " + packetNum + " seqnum: " 
											+ testClient.packets.get(packetNum).getSeqNum());
				}
					
				packetNum++;
			} //end while
			try {
				if(VERBOSE) {
					System.out.println("\n-----SLEEP-----\n");
				}
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(VERBOSE) {
				if(!testClient.checkWindow()) {
					System.out.println("window closed.");
				}
				System.out.println("Wait for ACK");
			}
			packet ACK = testClient.receiveACK();
			if(VERBOSE) {
				System.out.println("received ACK. ACK = ");
				ACK.printContents();
			}
			if(ACK.getSeqNum() == sentPackets.get(0).getSeqNum()) { //if ACK is seqnum expected
				sentPackets.remove(0);
				testClient.incSendBase();
				if(VERBOSE) {
					System.out.println("popped packet" + " seqnum: " + ACK.getSeqNum());
				}
				if(ACK.getType() == 2) { //ACK of EOT
					end = true;
					if(VERBOSE) {
						System.out.println("received EOTACK\n");
					}
				}	
			} else {
				if(VERBOSE) {
					System.out.println("------BAD ACK------");
					System.out.println("sending sentPackets.get(0)");
				}
				try {
					testClient.sendPacket(sentPackets.get(0));
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
