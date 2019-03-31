package edu.metrostate.ics460.gp2;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Receiver for a passed binary file split up in chunks.
 * <p>
 * ICS460 - Group Project #1
 *
 * @author Michael, Andrew, Troy (Team 5)
 */

public class Receiver{
    private DatagramSocket socket;
    private boolean running;
    private static InetAddress address;
    private static final int BUFFER_SIZE = 1024;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private static double corrupt_data = 1;
    private static int port = 4445;
    final long startTime;

    /**
     * Public constructor that creates an open socket on port 4445
     */
    public Receiver() {
    	startTime = System.nanoTime();
        try {
            socket = new DatagramSocket(port,address);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * The thread that will constantly loop to read in new packets.
     */
    public void loop() {
        int packets = 0;
        running = true;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        boolean droppedPacket = false;
        while (running) { //loop until the end of the data is received
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.receive(packet);
                Packet truePacket = Packet.generatePacket(packet.getData());


                //If final statement, stop running
                for (byte b : packet.getData()) {
                    if (b != 0) {
                        running = true;
                        break;
                    }
                    running = false;
                }
                if (!running) {
                    continue;
                }

                //Return ACK
                if (Math.random() > corrupt_data) {
                	
                	String text = String.format("RECV %s %d RECV", getTime(), packets);
                	if(droppedPacket) {
                    	text = String.format("DUPL %s %d RECV", getTime(), packets);
                    }
                	droppedPacket = false;
                    System.out.println(text);
                	byteArrayOutputStream.write(truePacket.data);
                	System.out.println(String.format("SENDing ACK %d %s SENT", packets,getTime()));
                	packets++;
                    Packet response = new Packet((short) 0, (short) 8, truePacket.seqno);
                    byte[] responseData = response.getData();
                    
                    InetAddress return_address = packet.getAddress();
                    DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, return_address, packet.getPort());
                    socket.send(responsePacket);
                } else {
                	System.out.println(String.format("SENDing ACK %d %s DROP", packets,getTime()));
                	droppedPacket = true;
                }
      
                buffer = new byte[BUFFER_SIZE];
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        try {
            byte[] result = byteArrayOutputStream.toByteArray();
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("output.txt"));
            outputStream.write(result);
            outputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        socket.close();
    }
    
    public String getTime() {
    	long milliseconds = (System.nanoTime() - startTime) / 1000000;
    	
    	return String.valueOf(milliseconds);
    }

    public static void main(String[] args) {
    	try {
			address = InetAddress.getByName("localhost"); //assign localhost as default
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
        if (args.length < 1) {
            System.out.println("You can provide specifications for size of packet, timeout, ip address/port and percent of corrupt data.");
        }
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {

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
                            e.printStackTrace();
                        }
                        port = Integer.parseInt(args[(i + 1)]);
                    }
            }
        }
        System.out.printf("Params: corrupt data percent %.2f ip %s port %d", corrupt_data, address, port);
        Receiver receiver = new Receiver();
        receiver.loop();
    }
}