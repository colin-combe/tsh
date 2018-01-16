package udp;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SendUDPWord {

//John - if you remove the "//" from the start of the lines below (16, 17, 28)
// then you will see debugging info on the UDP messages being sent
    public SendUDPWord(String sHostName, int port, String word) {
        InetAddress IPAddress = null;
        String s1;

        try {
            IPAddress = InetAddress.getByName(sHostName);
        } catch (UnknownHostException ex) {
            Logger.getLogger(SendUDPWord.class.getName()).log(Level.SEVERE, null, ex);
        }

        s1 = word;
        DatagramSocket clientSocket = null;
        try {
            clientSocket = new DatagramSocket(port);
        } catch (SocketException ex) {
            Logger.getLogger(SendUDPWord.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] sendData = new byte[1024];
        sendData = s1.getBytes();
        // System.out.println("Attempting to send data:" + s1);
        DatagramPacket sendPacket
                = new DatagramPacket(sendData, sendData.length, IPAddress, port);
        if (clientSocket != null) {
            try {
                clientSocket.send(sendPacket);
            } catch (IOException ex) {
                Logger.getLogger(SendUDPWord.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                clientSocket.close();        
            }
        }
    }

    public static void main(String args[]) throws Exception {
        String hostname = new String("127.0.0.1");
        new SendUDPWord("192.168.0.104", 7474, "bananas");
    }
}
