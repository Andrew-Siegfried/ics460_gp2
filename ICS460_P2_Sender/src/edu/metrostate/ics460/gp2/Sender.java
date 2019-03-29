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
    private String filename = "center_earth.txt";
    private DatagramSocket socket;

    private static InetAddress address;
    private byte[] buffer;
    private static int timeout = 2000;
    private static double corrupt_data = 0;
    private static int packet_size = 500;
    private static int port = 4445;

    /**
     * Public constructor that creates a new socket on the local network
     */
    public Sender() {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeout);
            address = InetAddress.getByName("localhost");
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

                while(missedPacket) {

                    byte[] packetData = new Packet((short) 0,(short) (buffer.length + 12), 0, seqno, buffer).getData(buffer.length);

                    DatagramPacket packet = new DatagramPacket(packetData, packetData.length, address, 4445);
                    socket.send(packet);
                    String text = String.format("[%d][%d][%d] : Sender", seqno, seqno * packet_size, seqno * packet_size + buffer.length);
                    seqno++;
                    missedPacket = false;

                    System.out.println("\n" + text);
                    try {
                        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                        socket.receive(responsePacket);
                    } catch (SocketTimeoutException ex) {
                        System.out.println("Timed out on packet " + seqno);
                        seqno--;
                        missedPacket = true;
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

    public static void main(String[] args) {
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