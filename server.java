/*
* Andrew Mehta (amasdf)
* Jake Manning (jsm652)
* Data Communication and Networking
* Programming Assignment #2 : GBN Implementation
* 10/28/2019
*
* Server File
*/

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

	//------------------- Private Variable Definitions -------------------//
    
	private int receiveFromEmulator;
	private int sendToEmulator;
	private int expected_seqnum = 0;
	private DatagramSocket socket = null;
	private BufferedWriter writer = null;
	private BufferedWriter LogWriter = null;
	private String emulator;
	private boolean firstWrite = true;

	//------------------- Begin Private Function Definitions -------------------//
    
    //--- writeToTextfile - Inputs a string that is then written to the open file. No return.
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
	//--- openWriter - Inputs a file name and opens the writer. No return.
	private void openWriter(String outputFileName) {
		File outputFile = new File(outputFileName);
		try {
			writer = new BufferedWriter(new FileWriter(outputFileName, false));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	//--- openLogger - Inputs the filename for the log file and uses it to open another writer. No return.
	private void openLogger(String outputFileName) {
		File outputFileLog = new File(outputFileName);
		try {
			LogWriter = new BufferedWriter(new FileWriter(outputFileName, false));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	//--- openSocket - Opens the datagram socket to receive data. No return.
	private void openSocket() {
		try {
			socket = new DatagramSocket(receiveFromEmulator);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
    //--- writetoArrivalLog - Inputs the received sequence number and write it to the arrival log file. No return.
	private void writeToArrivalLog(int seqnum) {
		try {
			LogWriter.write(seqnum + "\n");
			LogWriter.flush();
		} catch (IOException io) {
			io.printStackTrace();
		}
	}
    //--- moveWindow - This calculates the expected sequence number. Returns the expected sequence number as an interger.
	private int moveWindow() {
		expected_seqnum = (expected_seqnum + 1) % 8;
		return expected_seqnum;
	}

    //------------------- End Private Function Definitions -------------------//
     
            
     
     
    //------------------- Begin Public Function Definitions -------------------//

	// Class Constructor - has 4 inputs
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
	//--- receivePacket - Inputs a datagram packet and receives a packet from the client. No return.
	public packet receivePacket(DatagramPacket datagramPacket) {
		try {
			socket.receive(datagramPacket);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.err.println("socket.receive");
		}
		
		packet MaxsPacket = null;
		try {
			MaxsPacket = (packet) toObject(datagramPacket.getData());
			if (MaxsPacket.getSeqNum() == expected_seqnum && MaxsPacket.getType() == 1) {
				writeToTextfile(MaxsPacket.getData());
			}
			
			
			writeToArrivalLog(MaxsPacket.getSeqNum());
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		
		
		return MaxsPacket;
	}
    //--- sendACK - Inputs the received datagram packet and checks the sequence number. If correct, it sends the appropiate ACK. No return.
	public packet sendACK(packet rcvPacket) {
		packet ACK = new packet(0, expected_seqnum, 0, null);
		if(expected_seqnum == rcvPacket.getSeqNum()) {
			moveWindow();
		}
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
		
		return ACK;
	}
	//--- sendEOT - Called when the EOT occurs. Returns a packet of the EOT.
	public packet sendEOT() {
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
		
		return EOT;
	}
    //--- closeSocket - Closes the open datagram socket. No return.
	public void closeSocket() {
		socket.close();
	}
	//--- closeWriters - Closes the open writer files. No return.
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
		boolean VERBOSE = false; //exists for testing
		
		server testServer = new server(args[0], args[1], args[2], args[3]);
		//server testServer = new server("localhost", "6002", "6000", "output.txt");
		
		if(VERBOSE) {
			System.out.println("opened Server");
		}
		while(!end) {
			byte[] rcvBuf = new byte[125];
			DatagramPacket receivedSerialPacket = new DatagramPacket(rcvBuf, rcvBuf.length);
			if(VERBOSE) {
				System.out.println("waiting to receive packet");
			}
			packet rcvPacket = testServer.receivePacket(receivedSerialPacket);
			if(VERBOSE) {
				System.out.println("received packet num " + rcvPacket.getSeqNum());
			}
			if(rcvPacket.getType() == 1) { //1 = data, 3 = EOT
				packet ACK = testServer.sendACK(rcvPacket);
				if(VERBOSE) {
					System.out.println("Sent ACK num " + ACK.getSeqNum() + "\n");
				}
			} else {
				packet EOT = testServer.sendEOT();
				if(VERBOSE) {
					System.out.println("Sent ACKEOT. Seqnum: " + EOT.getSeqNum() + "\n");
				}
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
