package edu.metrostate.ics460.gp2;

import java.io.*;

public class Packet implements Serializable {
    short cksum; //16-bit 2-byte
    short len;    //16-bit 2-byte
    int ackno;    //32-bit 4-byte
    int seqno;    //32-bit 4-byte Data packet Only
    byte data[] = new byte[500]; //0-500 bytes. Data packet only. Variable

    public Packet(){

    }

    public Packet(short cksum, short len, int ackno, int seqno, byte[] data) {
        this.cksum = cksum;
        this.len = len;
        this.ackno = ackno;
        this.seqno = seqno;
        this.data = data;
    }

    public Packet(short cksum, short len, int ackno) {
        this.cksum = cksum;
        this.len = len;
        this.ackno = ackno;
    }

    //returns seriallized version of packet to be sent over datagram
    public byte[] getData(){
        //https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        byte[] dataPacket = new byte[0];
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            dataPacket = bos.toByteArray();
        } catch(IOException ex) {
        }finally
        {
            try {
                bos.close();
            } catch (IOException ex) {
            }
        }

        return dataPacket;
    }

    public static Packet generatePacket(byte[] packetData){
        ByteArrayInputStream bis = new ByteArrayInputStream(packetData);
        ObjectInput in = null;
        Packet o = new Packet();
        try {
            in = new ObjectInputStream(bis);
            o = (Packet) in.readObject();
        } catch(IOException | ClassNotFoundException ex){
        }
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
            }
        }

        return o;
    }
}
