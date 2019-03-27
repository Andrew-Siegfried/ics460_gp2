package edu.metrostate.ics460.gp2;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.beans.value.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

/**
 * Sender for a passed binary file split up in chunks.
 *   
 * ICS460 - Group Project #1
 * 
 * @author Michael, Andrew, Troy (Team 5)
 * 
 */
public class Sender extends Application {
	
	private Stage window = new Stage();
	private static TextArea textArea;
	private String filename = "H:\\eula.txt";
    private DatagramSocket socket;
    private static InetAddress address;
    private static final int BUFFER_SIZE = 16;
    private byte[] buffer;
    private static int timeout = 2000;
    private static double corrupt_data = 0;
    private static int packet_size = 100;
    private static int port = 4445;
    private static int stop_wait = 1;

    /**
	 * Public constructor that creates a new socket on the local network
	 */
    public Sender() {
        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName("localhost");
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    /**
	 * Sends the file one packet at a time over the specified socket and port
	 * 
	 * @param fileName
	 * 			the path to the input file that we will be sending to the receiver
	 */
    public void sendFile(String fileName) {
        int packets = 0;
        
        try {
        	FileInputStream file = new FileInputStream(fileName);
            InputStream inputStream = new BufferedInputStream(file);
            buffer = new byte[packet_size];
            
            while (inputStream.read(buffer) != -1) { //create a new packet with a specified size and send it through the socket
            	
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 4445);

                socket.send(packet);
                String text = String.format("[%d][%d][%d] : Sender", packets, packets * BUFFER_SIZE, packets * BUFFER_SIZE + buffer.length);
                textArea.appendText(text+"\n");
                
                System.out.println(text);
                
                try {
                	DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                	socket.setSoTimeout(timeout); //this times out if no packet is returned but can't get it working..
                	socket.receive(responsePacket);
                	System.out.println(responsePacket.getData().toString());
                } catch (SocketException ex) {
                	System.out.println("No ACK");
                }
                packets++;
            }
            
            buffer = "end".getBytes(); //we have sent everything, send a signal to the receiver that this is the ending packet
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 4445);
            socket.send(packet);
            socket.close();
            inputStream.close(); //close everything up
        } catch (IOException ex) {
            //ex.printStackTrace();
        }
    }
    
    @Override
	public void start(Stage primaryStage) throws Exception {

		window.setTitle("Sender");

		GridPane grid = new GridPane();
		grid.setPadding(new Insets(1, 1, 1, 1));
		grid.setVgap(1);
		grid.setHgap(10);
		
		textArea = new TextArea();
		textArea.setStyle("-fx-text-fill: red;");
		grid.add(textArea, 0, 0);
		
		GridPane area = new GridPane(); 
		area.setPadding(new Insets(1, 1, 1, 1)); //spacing a bit
		area.setVgap(1);
		area.setHgap(10);
		grid.add(area,0, 1);
  
		Button create = new Button("Start Sender");
  
		grid.add(create, 0, 4);

		create.setOnAction(e -> 
		{ 
			 Sender client = new Sender();
		     client.sendFile(filename);
		});

		Scene scene = new Scene(grid);
		window.setScene(scene);
		window.showAndWait();

	}

    public static void main(String[] args) {
        if (args.length < 1) { 
            System.out.println("You can provide specifications for size of packet, timeout, ip address/port and percent of corrupt data.");
        }
        for(int i=0;i<args.length;i++) {
    		switch(args[i])
    		{
    		   case "-s":
    			   packet_size = Integer.parseInt(args[(i+1)]);
    			   i++;
    		      break;    
    		   case "-t":
    			   timeout = Integer.parseInt(args[(i+1)]);
    			   i++;
    		      break;    
    		   case "-d":
    			   corrupt_data = Double.parseDouble(args[(i+1)]);
    			   i++;
     		      break;  
    		   case "localhost":
    			   port = Integer.parseInt(args[(i+1)]);
      		      break;
    		   default: 
    			   if(args[i].contains(".")) {
    				   try {
						address = InetAddress.getByName(args[i]);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    				   port = Integer.parseInt(args[(i+1)]);
    			   }
    		}
        }
        System.out.printf("Params: packet size: %d timeout %d corrupt data percent %.2f ip %s port %d",packet_size,timeout,corrupt_data,address,port);
        launch(args);
    }
}