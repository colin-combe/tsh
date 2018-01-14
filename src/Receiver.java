import java.io.IOException;
import java.net.*;

public class Receiver {

    public static void main(String[] args) {
        int port = args.length == 0 ? 57 : Integer.parseInt(args[0]);
        new Receiver().run(7474);
    }

    public void run(int port) {    
       try {
      System.out.printf("Listening on udp:%s:%d%n",
                InetAddress.getLocalHost().getHostAddress(), port);     
        InetSocketAddress address = new InetSocketAddress("192.168.0.10", 7474);
        DatagramSocket serverSocket = new DatagramSocket(7474, InetAddress.getByName("127.0.0.255"));
        
//        serverSocket.bind(address);
        
        
        byte[] receiveData = new byte[8];

        DatagramPacket receivePacket = new DatagramPacket(receiveData,
                           receiveData.length);

        while(true)
        {
              serverSocket.receive(receivePacket);
              String sentence = new String( receivePacket.getData(), 0,
                                 receivePacket.getLength() );
              System.out.println("RECEIVED: " + sentence);
              // now send acknowledgement packet back to sender     
              InetAddress IPAddress = receivePacket.getAddress();
//              String sendString = "polo";
//              byte[] sendData = sendString.getBytes("UTF-8");
//              DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
//                   IPAddress, receivePacket.getPort());
//              serverSocket.send(sendPacket);
        }
      } catch (IOException e) {
              System.out.println(e);
      }
      // should close serverSocket in finally block
    }
}