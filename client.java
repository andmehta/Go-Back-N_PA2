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
		writeToClientSeqnum(packets.get(packetNum).getSeqNum());
		incNextSeqnum();
	}
	
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
 
	public boolean checkWindow() {
		int difference = nextseqnum - sendBase;
		
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
	
	public void printPackets() {
		for(int i = 0; i < packets.size(); i++) {
			packets.get(i).printContents();
		}
	}
	
	public static void main(String args[]) {
		boolean end = false;
		boolean VERBOSE = false; 
		if(true /*args[4] != null*/) { //TODO TEMPORARY true, swap to commented
			VERBOSE = true;
		}
		//client myclient = new client(args[0], args[1], args[2], args[3]);
		
		client testClient = new client("localhost", "6000", "6001", "test.txt");
		
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
					testClient.sendPacket(packetNum);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				if(VERBOSE) {
						System.out.println("sent packet: " + packetNum + " seqnum: " 
											+ testClient.packets.get(packetNum).getSeqNum());
				}
					
				packetNum++;
			} //end while
			try {
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
				System.out.println("received ACK\n");
				ACK.printContents();
			}
			if(ACK.getSeqNum() == testClient.packets.get(0).getSeqNum()) { //if ACK is seqnum expected
				testClient.packets.remove(0);
				packetNum--; //removed a packet, make sure to decrement packetNum
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
				//testClient.nextseqnum = ACK.getSeqNum();
				packetNum = 0;
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
