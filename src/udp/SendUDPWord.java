package udp;

import java.net.*;
import java.util.*;

public class SendUDPWord {

//John - if you remove the "//" from the start of the lines below (16, 17, 28)
// then you will see debugging info on the UDP messages being sent
    
    public SendUDPWord(String sHostName, int port, String word) {
        InetAddress IPAddress = null;
        String s1;
        ArrayList lines = new ArrayList();
        int size;

        try {
            IPAddress = InetAddress.getByName(sHostName);


//            System.out.println("Attemping to connect to " + IPAddress
//                    + " via UDP port " + port);
        } catch (UnknownHostException ex) {
            System.err.println(ex);
            System.exit(1);
        }

        try {
            s1 = word;
            DatagramSocket clientSocket = new DatagramSocket(port);
            byte[] sendData = new byte[1024];
            sendData = s1.getBytes();
           // System.out.println("Attempting to send data:" + s1);
            DatagramPacket sendPacket
                    = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            clientSocket.send(sendPacket);
            clientSocket.close();
        } catch (Exception ex) {
            System.err.println(ex);
        }

    }

    public static void main(String args[]) throws Exception {
        String hostname = new String("127.0.0.1");
        new SendUDPWord(hostname, 5010, "bananas");
    }
}
