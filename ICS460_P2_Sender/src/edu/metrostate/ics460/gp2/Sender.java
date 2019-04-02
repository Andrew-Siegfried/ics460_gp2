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
                boolean damagePacket = false;
                int lastAck = -1;
                while(missedPacket) {

                    if (Math.random() < corrupt_data) {
                    	damagePacket = true;
                    }
                    
                    if(damagePacket == false) {
                        byte[] packetData = new Packet((short) 0,(short) (buffer.length + 12), 0, seqno, buffer).getData();

                        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, address, port);
                        socket.send(packet);
                        String text = String.format("SENDing %d %d:%d %s SENT", seqno, seqno * packet_size, seqno * packet_size + buffer.length,getTime());
                        
                        if(droppedPacket) {
                            text = String.format("ReSend. %d %d:%d %s SENT", seqno, seqno * packet_size, seqno * packet_size + buffer.length,getTime());
                        }
                        droppedPacket = false;
                        missedPacket = false;

                        System.out.println(text);
                        try {
                        	boolean damaged = false;
                            byte arr[] = new byte[1000]; //fixes not getting data from ACK
                            DatagramPacket responsePacket = new DatagramPacket(arr, arr.length);
                            socket.receive(responsePacket);
                            Packet responseACK = Packet.generatePacket(responsePacket.getData());

                            String ackText = String.format("AckRcvd %d MoveWnd", seqno);
                            if(responseACK.cksum != 0) {
                                ackText = String.format("AckRcvd %d ErrAck", seqno);
                                damaged = true;
                            }
	                        
	                        if(responseACK.ackno == lastAck) {
	                        	ackText = String.format("AckRcvd %d DuplAck", seqno);
	                        	damaged = true;
	                        } 
	                        System.out.println(ackText);
	                        if(damaged == false) {
	                        	 lastAck = responseACK.ackno;
	                             //System.out.println(String.format("Ack INFO: %d %d %d", responseACK.cksum,responseACK.len,responseACK.ackno));
	                             seqno++;
	                        } else {
	                        	missedPacket = true;
	                        }
                        } catch (SocketTimeoutException ex) {
                            System.out.println("TimeOut " + seqno);
                            missedPacket = true;
                        }
                    } else {
                    	//Damage the packet here, either send it corrupt OR drop it from source.
                    	if (Math.random() < 0.5) { //50% we corrupt and send
                    		byte[] packetData = new Packet((short) 1,(short) (buffer.length + 12), 0, seqno, buffer).getData();
                            DatagramPacket packet = new DatagramPacket(packetData, packetData.length, address, port);
                            socket.send(packet);
                            
                            String text = String.format("SENDing. %d %d:%d %s ERR", seqno, seqno * packet_size, seqno * packet_size + buffer.length,getTime());
                        	if(droppedPacket) {
                        		text = String.format("ReSend. %d %d:%d %s ERR", seqno, seqno * packet_size, seqno * packet_size + buffer.length,getTime());
                            }
                        	
                        	System.out.println(text);
                    	} else { //50% we drop from source
                    		System.out.println(String.format("SENDing %d %d:%d %s DROP", seqno, seqno * packet_size, seqno * packet_size + buffer.length,getTime()));
                            droppedPacket = true;
                    	}    
                    }
                    damagePacket = false;
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
        sender.sendFile("eula.txt");
    }
}