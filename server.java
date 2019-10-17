/************************
 *                      *
 * Andrew Mehta am3258  *
 * Jake Manning jsm652  *
 *                      *
 ************************/

import java.net.*;
import java.io.*;

public class server {

	//Private Variables
	private int 			UDP_Port_receiving;
	private int 			UDP_Port_sending;
	private DatagramSocket  socket   			= null;
	private byte[] 			buf      			= new byte[256];
	
	
	//private functions
	private void writeToTextfile(String info) {
		
	}
	
	private void writeToArrivalLog(packet arrivedPacket) {
		
	}
	
	private String PacketToString(packet packet) {
		return "";
	}
	/********************
	 * Public Functions *
	 *                  *
	 ********************/
	
	//Constructor
	public server(String emulatorName, String receivingPort, String sendingPort, String outputFile) {
		//first parse arguments into variables for later
		UDP_Port_receiving = Integer.parseInt(receivingPort);
		UDP_Port_sending   = Integer.parseInt(sendingPort);
		
		try {
		socket = new DatagramSocket(UDP_Port_receiving);
		}
		catch(IOException io) {
			System.err.println(io);
		}
	}
	
	
	public void closeSocket() {
		try {
			socket.close();
		}
		catch (IOException io) {
			System.err.println(io);
		}
	}
	
	public static void main(String args[]) {
		server newServer = new server(args[0], args[1], args[2], args[3]);
		
		
		//close the socket
		newServer.closeSocket();
	}
}
