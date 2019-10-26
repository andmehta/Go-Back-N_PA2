
/************************
 *                      *
 * Andrew Mehta am3258  *
 * Jake Manning jsm652  *
 *                      *
 ************************/

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class server {

	/*********************
	 * Private variables * 
	 *********************/
	private int receiveFromEmulator;
	private int sendToEmulator;
	private int expected_seqnum;
	private DatagramSocket socket = null;
	private BufferedWriter writer = null;
	private BufferedWriter LogWriter = null;
	private String emulator;
	private boolean firstWrite = true;

	/*********************
	 * Private Functions * 
	 *********************/
	private void writeToTextfile(String info) {
		try {
			if(firstWrite) {
				writer.write(info);
				firstWrite = false;
			} else {
				writer.append(info);
			}
		} catch (IOException io) {
			io.printStackTrace();
		}
	}
	
	private void openWriter(String outputFileName) {
		File outputFile = new File(outputFileName);
		try {
			writer = new BufferedWriter(new FileWriter(outputFileName, true));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void openLogger(String outputFileName) {
		File outputFileLog = new File(outputFileName);
		try {
			LogWriter = new BufferedWriter(new FileWriter(outputFileName, true));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void openSocket() {
		try {
			socket = new DatagramSocket(receiveFromEmulator);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private void writeToArrivalLog(int seqnum) {
		try {
			LogWriter.write(seqnum + "\n");
			//LogWriter.flush();
		} catch (IOException io) {
			io.printStackTrace();
		}
	}

	private int moveWindow() {
		expected_seqnum = (expected_seqnum + 1) % 8;

		return expected_seqnum;
	}

	/********************
	 * Public Functions * 
	 ********************/

	// Constructor
	public server(String emulatorName, String receivingPort, String sendingPort, String outputFileName) {
		// first parse arguments into variables for later
		receiveFromEmulator = Integer.parseInt(receivingPort);
		sendToEmulator = Integer.parseInt(sendingPort);
		emulator = emulatorName;

		//Open necessary peripherals
		openSocket();
		openLogger("arrival.log");
		openWriter(outputFileName);
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
	
	public int receivePacket(DatagramPacket datagramPacket) {
		try {
			socket.receive(datagramPacket);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.err.println("socket.receive");
		}
		
		packet MaxsPacket = null;
		try {
			MaxsPacket = (packet) toObject(datagramPacket.getData());
			if (MaxsPacket.getSeqNum() == expected_seqnum) {
				writeToTextfile(MaxsPacket.getData());
			}
			
			
			writeToArrivalLog(MaxsPacket.getSeqNum());
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		
		return MaxsPacket.getType();
	}

	public void sendACK() {//TODO sendACK back to where it came
		packet ACK = new packet(0, expected_seqnum, 0, null);
		
		byte[] sendBuf = new byte[125];
		try {
			sendBuf = toBytes(ACK);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		DatagramPacket datagramPacket = null;
		try {
			datagramPacket = new DatagramPacket(sendBuf, sendBuf.length, InetAddress.getByName(emulator), sendToEmulator);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			socket.send(datagramPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		moveWindow();
	}
	
	public void sendEOT() {//TODO sendACK back to where it came
		packet EOT = new packet(2, expected_seqnum, 0, null);
		
		byte[] sendBuf = new byte[125];
		try {
			sendBuf = toBytes(EOT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		DatagramPacket datagramPacket = null;
		try {
			datagramPacket = new DatagramPacket(sendBuf, sendBuf.length, InetAddress.getByName(emulator), sendToEmulator);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			socket.send(datagramPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void closeSocket() {
		socket.close();
	}
	
	public void closeWriters() {
		try {
			writer.close();
			LogWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {
		boolean end = false;
		boolean VERBOSE = false; 
		int count = 0;
		if(true /*args[4] != null*/) { //TEMPORARY true, swap to commented TODO
			VERBOSE = false;
		}
		// server newServer = new server(args[0], args[1], args[2], args[3]);

		server testServer = new server("localhost", "6002", "6000", "output.txt");
		
		if(VERBOSE) {
			System.out.println("opened Server");
		}
		while(!end) {
			byte[] rcvBuf = new byte[125];
			DatagramPacket receivedSerialPacket = new DatagramPacket(rcvBuf, rcvBuf.length);
			if(VERBOSE) {
				System.out.println("waiting to receive packet");
			}
			if(testServer.receivePacket(receivedSerialPacket) == 1) { //returns Type, which if 1 is a data packet, else is EOT
				if(VERBOSE) {
					count++;
					System.out.println("received packet num " + count);
				}
				testServer.sendACK();
				if(VERBOSE) {
					System.out.println("Sent ACK num " + count + "\n");
				}
			} else {
				testServer.sendEOT();
				end = true;
			}
		
		}
		// close the socket and writers
		testServer.closeWriters();
		testServer.closeSocket();
		if(VERBOSE) {
			System.out.println("closed sockets and writers. Exiting server");
		}
	}
}
