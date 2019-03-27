package edu.metrostate.ics460.gp2;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

/**
 * Receiver for a passed binary file split up in chunks.
 *   
 * ICS460 - Group Project #1
 * 
 * @author Michael, Andrew, Troy (Team 5)
 * 
 */

public class Receiver extends Application {
	
	private Stage window = new Stage();
	private static TextArea textArea;
    private DatagramSocket socket;
    private boolean running;
    private static InetAddress address;
    private static final int BUFFER_SIZE = 1024;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private static double corrupt_data = 0;
    private static int port = 4445;
    private static int stop_wait = 1;

    /**
	 * Public constructor that creates an open socket on port 4445
	 */
    public Receiver() { 
        try {
            socket = new DatagramSocket(4445);
            //DatagramSocket(int port, InetAddress laddr)
        }catch(IOException ex){
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

        while (running) { //loop until the end of the data is received
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                socket.receive(packet);
               
                if (new String(packet.getData(), 0, packet.getLength()).trim().equals("end")) {
                    running = false;
                    continue;
                }
                byte[] trim = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, trim, 0, packet.getLength());
                byteArrayOutputStream.write(trim); //Print out the packet information that will match the senders information
                String text = (String.format("[%d][%d][%d] : Receiver", packets, packets * BUFFER_SIZE, packets * BUFFER_SIZE + buffer.length));
                textArea.appendText(text+"\n");
                
                System.out.println(text);
                if (Math.random() < corrupt_data) {
	                String response = "Recieved " + String.valueOf(packet);
	                byte[] responseData = response.getBytes("UTF-8");
	                InetAddress address = packet.getAddress();
	                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, address, packet.getPort());
	                socket.send(responsePacket);
                }
                packets++;
                buffer = new byte[BUFFER_SIZE];
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }
        try {
            byte[] result = byteArrayOutputStream.toByteArray();
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("output.txt"));
            outputStream.write(result);
            outputStream.close();
        } catch(IOException ex) {
            ex.printStackTrace();
        }
        socket.close();
    }
    
    @Override
	public void start(Stage primaryStage) throws Exception {

		window.setTitle("Receiver");

		GridPane grid = new GridPane();
		grid.setPadding(new Insets(1, 1, 1, 1));
		grid.setVgap(1);
		grid.setHgap(10);
		
		textArea = new TextArea();
		textArea.setStyle("-fx-text-fill: blue;");
		grid.add(textArea, 0, 0);
		
		GridPane area = new GridPane(); 
		area.setPadding(new Insets(1, 1, 1, 1)); //spacing a bit
		area.setVgap(1);
		area.setHgap(10);
		grid.add(area,0, 1);
  
		Button create = new Button("Start Receiver");
  
		grid.add(create, 0, 4);

		create.setOnAction(e -> 
		{ 
			Platform.runLater(new Runnable() {          
			    @Override
			    public void run() {
			        loop();
			    }
			});
		});

		Scene scene = new Scene(grid);
		window.setScene(scene);
		window.showAndWait();
	}

    /**
	 * Starts our javaFX.
	 */
    public static void main(String[] args) {
    	 if (args.length < 1) { 
             System.out.println("You can provide specifications for size of packet, timeout, ip address/port and percent of corrupt data.");
         }
         for(int i=0;i<args.length;i++) {
     		switch(args[i])
     		{
     		
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
         System.out.printf("Params: corrupt data percent %.2f ip %s port %d",corrupt_data,address,port);
        launch(args);
        Platform.setImplicitExit(false);
    }
}