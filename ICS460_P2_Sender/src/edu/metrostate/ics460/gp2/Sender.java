package edu.metrostate.ics460.gp2;

import java.io.*;
import java.net.*;
import java.util.Arrays;

/**
 * Sender for a passed binary file split up in chunks.
 * <p>
 * ICS460 - Group Project #1
 *
 * @author Michael, Andrew, Troy (Team 5)
 */
public class Sender{
    private DatagramSocket socket;
    private static InetAddress address;
    private byte[] buffer;
    private static int timeout = 2000;
    private static double corrupt_data = 0;
    private static int packet_size = 500;
    private static int port = 4445;
    final long startTime;

    /**
     * Public constructor that creates a new socket on the local network
     */
    public Sender() {
    	startTime = System.nanoTime();
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeout);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Sends the file one packet at a time over the specified socket and port
     *
     * @param fileName the path to the input file that we will be sending to the receiver
     */
    public void sendFile(String fileName) {
        int seqno = 0;
        boolean missedPacket;
        int bytesRead;

        try {
            InputStream inputStream = new BufferedInputStream(new FileInputStream(fileName));
            buffer = new byte[packet_size];

            while ((bytesRead = inputStream.read(buffer)) >= 0) { //loop while data exists to be read
                buffer = Arrays.copyOfRange(buffer, 0 ,bytesRead); //prevents garbage data in last packet
                missedPacket = true;
                boolean droppedPacket = false;
                int lastAck = -1;
                while(missedPacket) {
                	
                	if (Math.random() > corrupt_data) {
                		
                		

						byte[] packetData = new Packet((short) 0,(short) (buffer.length + 12), 0, seqno, buffer).getData();
	
	                    DatagramPacket packet = new DatagramPacket(packetData, packetData.length, address, port);
	                    socket.send(packet);
	                    String text = String.format("SENDing %d %d:%d %s SENT", seqno, seqno * packet_size, seqno * packet_size + buffer.length,getTime());
	                    if(Packet.generatePacket(packetData).cksum != 0) {
	                    	text = String.format("SENDing. %d %d:%d %s ERR", seqno, seqno * packet_size, seqno * packet_size + buffer.length,getTime());
	                    }
	                    if(droppedPacket) {
	                    	text = String.format("ReSend. %d %d:%d %s SENT", seqno, seqno * packet_size, seqno * packet_size + buffer.length,getTime());
	                    	if(Packet.generatePacket(packetData).cksum != 0) {
		                    	text = String.format("ReSend. %d %d:%d %s ERR", seqno, seqno * packet_size, seqno * packet_size + buffer.length,getTime());
		                    }
	                    }
	                    droppedPacket = false;
	                    missedPacket = false;
	
	                    System.out.println(text);
	                    try {
	                        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
	                        socket.receive(responsePacket);
	                        Packet truePacket = Packet.generatePacket(responsePacket.getData());
	                        
	                        String ackText = String.format("AckRcvd %d MoveWnd", seqno);
	                        if(truePacket.cksum != 0) {
	                        	ackText = String.format("AckRcvd %d ErrAck", seqno);
	                        }
	                        
	                        /*if(truePacket.ackno == lastAck) {
	                        	ackText = String.format("AckRcvd %d DuplAck", seqno);
	                        }*/ //not working because not correct info is returned
	                        
	                        lastAck = truePacket.ackno;
	                        
	                        System.out.println(ackText);
	                        System.out.println(String.format("Ack INFO: %d %d %d", truePacket.cksum,truePacket.len,truePacket.ackno));
	                        seqno++;
	                        //need dup ACK, err ACK, ERR in send
	                    } catch (SocketTimeoutException ex) {
	                        //seqno--;
	                        System.out.println("TimeOut " + seqno);
	                        missedPacket = true;
	                    }
                	} else {
                		System.out.println(String.format("SENDing %d %d:%d %s DROP", seqno, seqno * packet_size, seqno * packet_size + buffer.length,getTime()));
                		droppedPacket = true;
                	}
                }
            }

            buffer = new byte[0]; //we have sent everything, send a signal to the receiver that this is the ending packet
            System.out.println(buffer.length);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 4445);
            socket.send(packet);
            socket.close();
            inputStream.close(); //close everything up
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public String getTime() {
    	long milliseconds = (System.nanoTime() - startTime) / 1000000;
    	
    	//return String.format("%02d:%02d.%d", ((milliseconds / (1000 * 60)) % 60), ((milliseconds / 1000) % 60), (milliseconds % 1000));
    	return String.valueOf(milliseconds);
    }

    public static void main(String[] args) {
        try {
			address = InetAddress.getByName("localhost"); //assign localhost as default
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        if (args.length < 1) {
            System.out.println("You can provide specifications for size of packet, timeout, ip address/port and percent of corrupt data.");
        }
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-s":
                    packet_size = Integer.parseInt(args[(i + 1)]);
                    i++;
                    break;
                case "-t":
                    timeout = Integer.parseInt(args[(i + 1)]);
                    i++;
                    break;
                case "-d":
                    corrupt_data = Double.parseDouble(args[(i + 1)]);
                    i++;
                    break;
                case "localhost":
                    port = Integer.parseInt(args[(i + 1)]);
                    break;
                default:
                    if (args[i].contains(".")) {
                        try {
                            address = InetAddress.getByName(args[i]);
                        } catch (UnknownHostException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        port = Integer.parseInt(args[(i + 1)]);
                    }
            }
        }
        System.out.printf("Params: packet size: %d timeout %d corrupt data percent %.2f ip %s port %d", packet_size, timeout, corrupt_data, address, port);
        Sender sender = new Sender();
        sender.sendFile("center_earth.txt");
    }
}