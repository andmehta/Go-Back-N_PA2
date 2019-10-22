
/************************
 *                      *
 * Andrew Mehta am3258  *
 * Jake Manning jsm652  *
 *                      *
 ************************/

import java.net.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.io.*;

public class server {

	/*********************
	 * Private variables * 
	 *********************/
	private int UDP_Port_receiving;
	private int UDP_Port_sending;
	private int expected_seqnum;
	private DatagramSocket socket = null;
	private byte[] buf = new byte[256];
	private Logger logger = null;
	private FileHandler fileHandler = null;
	private File outputFile = null;
	private BufferedWriter writer = null;
	private String emulator;

	/*********************
	 * Private Functions * 
	 *********************/
	private void writeToTextfile(String info) {
		try {
			writer.write(info);
			writer.flush();
		} catch (IOException io) {
			io.printStackTrace();
		}
	}
	
	private void openWriter(String outputFileName) {
		outputFile = new File(outputFileName);
		try {
			writer = new BufferedWriter(new FileWriter(outputFileName, false));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void openLogger() {//TODO ask about how the LOG should be formatted. SimpleFormatter?
		logger = Logger.getLogger("myLog");
		try {
			fileHandler = new FileHandler("arrival.log");
		} catch (SecurityException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.addHandler(fileHandler);
		logger.setUseParentHandlers(false); // turns off logging to console
	}
	
	private void openSocket() {
		try {
			socket = new DatagramSocket(UDP_Port_receiving);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void writeToArrivalLog(int seqnum) {
		logger.info(String.valueOf(seqnum)); 
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
		UDP_Port_receiving = Integer.parseInt(receivingPort);
		UDP_Port_sending = Integer.parseInt(sendingPort);
		emulator = emulatorName;

		//Open necessary peripherals
		openSocket();
		openLogger();
		openWriter(outputFileName);
	}

	public boolean receivePacket(packet packet) {
		bool ret = false;
		if (packet.getSeqNum() == expected_seqnum) {
			moveWindow();
			writeToTextfile(packet.getData());
			ret = true;
		}
		
		
		writeToArrivalLog(packet.getSeqNum());
		
		return ret;
	}

	public void sendACK(packet packet) {//TODO sendACK back to where it came

	}

	public void closeSocket() {
		socket.close();
	}
	
	public void closeWriter() {
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void testLog(String info) { 
		logger.info(info);
	}

	public static void main(String args[]) {
		// server newServer = new server(args[0], args[1], args[2], args[3]);

		server testServer = new server("localhost", "6000", "6002", "output.txt");
		
		
		// close the socket and writer
		testServer.closeWriter();
		testServer.closeSocket();
	}
}
