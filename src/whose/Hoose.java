package whose;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
//import tsh.TshMonitor;
import udp.SendUDPWord;

public class Hoose {

    //  >> Here are your controls, John <<
    private static final String boxIP = "127.0.0.1";//you'll need to change this
    private static final int boxPort = 5010;//and this
    private static final String udpRecieveIP = "127.0.0.1";//"192.168.0.10";//and this
    private static final int udpReceivePort = 7474;

    private static DatagramSocket serverSocket = null;
    
    private static List<String> credentials;
    private static int calmCount = 0;
    private static int calmPosition = 0;
    private static Random randomGenerator;

    public static void main(String[] args) {
        try {
            credentials = Files.readAllLines(Paths.get("/home/col/cred.txt"));
        } catch (IOException ex) {
            Logger.getLogger(Hoose.class.getName()).log(Level.SEVERE, null, ex);
        }
        //System.out.println(credentials.get(1));

        
        InetSocketAddress address = new InetSocketAddress(udpRecieveIP, udpReceivePort);
        try {
            serverSocket = new DatagramSocket(address);
        } catch (SocketException ex) {
            Logger.getLogger(Hoose.class.getName()).log(Level.SEVERE, null, ex);
        }
        
//        getNextFilm();
//        getPlaylist();
//        getAllFilms();
//        setPlayed(107);
        while (true) {
            loop();
        }
    }

    static void loop() {

        PlaylistItem nextFilm = getNextFilm();
        String udpWord = nextFilm.name;

        //update db - TODO: work in udp confirmation / now playing
        if (nextFilm.id > -1) {
            setPlayed(nextFilm.id);
        }

        //send udp word for a while
        int secsToSend = 25;
        int udpFreq = 100; //every 1/10 secs (same interval as berwick time)

        int udpSendCount = secsToSend * 1000 / udpFreq;// quarterHourly updates;

        System.out.println("sending " + udpWord + " " + udpSendCount + " times");
        for (int u = 0; u < udpSendCount; u++) {
            new SendUDPWord(boxIP, boxPort, udpWord);
            try {
                Thread.sleep(udpFreq);
            } catch (InterruptedException ex) {
                Logger.getLogger(Hoose.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //listen for interstital starting
        int secsToListen = 13;
        try {
            System.out.printf("Listening on udp:%s:%d%n",
                    udpRecieveIP, udpReceivePort);
            
            byte[] receiveData = new byte[16];

            DatagramPacket receivePacket = new DatagramPacket(receiveData,
                    receiveData.length);

            serverSocket.setSoTimeout(secsToListen * 1000);
            serverSocket.receive(receivePacket);
            String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("RECEIVED: " + sentence);
            if (sentence.startsWith("pre_tInter")) {
                System.out.println("Yay! Confrimation recieved: " + sentence);
            } else {
                System.out.println("Bored waiting / wronf udp word recieved; er... proceeding");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    static PlaylistItem getNextFilm() {
        PlaylistItem nextFilm = null;
        List<PlaylistItem> playlist = getPlaylist();
        if (playlist.size() > 0) {
            nextFilm = playlist.get(0);
            calmCount = 0;
        } else {
            if (calmCount < 5) {
                calmCount++;
                calmPosition++;
                if (calmPosition > Intermission.wordList.length - 1) {
                    calmPosition = 0;
                }
                nextFilm = new PlaylistItem(-1, "Playlist empty", Intermission.wordList[calmPosition]);
            } else {
                calmCount = 0;
                //get random film
                List<PlaylistItem> allFilms = getAllFilms();
                nextFilm = allFilms.get(randomGenerator.nextInt(allFilms.size()));
            }
        }
        System.out.println("Next up: " + nextFilm.toString());
        return nextFilm;
    }

    public static List<PlaylistItem> getPlaylist() {
        Connection connect = null;
        Statement statement = null;
        ResultSet resultSet = null;
        List<PlaylistItem> playlist = new ArrayList<PlaylistItem>();
        try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            connect = DriverManager.getConnection("jdbc:mysql://"
                    + credentials.get(0)
                    + "/hoose_wp_2017?user=" + credentials.get(1)
                    + "&password=" + credentials.get(2));

            // Statements allow to issue SQL queries to the database
            statement = connect.createStatement();
            // Result set get the result of the SQL query
            resultSet = statement
                    .executeQuery("SELECT wp1.ID, wp2.post_name, wp2.post_title FROM wp_posts AS wp1 LEFT JOIN wp_posts wp2 ON wp1.post_title = wp2.ID WHERE wp1.post_status = \"draft\" AND wp1.post_type = \"post_playlists\" ORDER BY wp1.post_date ASC");
            // ResultSet is initially before the first data set
            while (resultSet.next()) {
                playlist.add(new PlaylistItem(resultSet.getInt("ID"), resultSet.getString("post_title"), resultSet.getString("post_name")));

            }
//            writeMetaData(resultSet);
        } catch (Exception e) {
            Logger.getLogger(Hoose.class
                    .getName()).log(Level.SEVERE, null, e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }

                if (connect != null) {
                    connect.close();

                }
            } catch (SQLException ex) {
                Logger.getLogger(Hoose.class
                        .getName()).log(Level.SEVERE, null, ex);
            }

        }
        System.out.println(playlist.size() + " items in playlist.");
        return playlist;
    }

    public static List<PlaylistItem> getAllFilms() {
        Connection connect = null;
        Statement statement = null;
        ResultSet resultSet = null;
        List<PlaylistItem> allFilms = new ArrayList<PlaylistItem>();
        try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            connect = DriverManager.getConnection("jdbc:mysql://"
                    + credentials.get(0)
                    + "/hoose_wp_2017?user=" + credentials.get(1)
                    + "&password=" + credentials.get(2));

            // Statements allow to issue SQL queries to the database
            statement = connect.createStatement();
            // Result set get the result of the SQL query
            resultSet = statement
                    .executeQuery("SELECT post_name, post_title FROM wp_posts WHERE post_status = \"publish\" AND post_type = \"post_videos\"");
            // ResultSet is initially before the first data set

            while (resultSet.next()) {
                allFilms.add(new PlaylistItem(-1, resultSet.getString("post_title"), resultSet.getString("post_name")));
            }
            writeMetaData(resultSet);

        } catch (Exception e) {
            Logger.getLogger(Hoose.class
                    .getName()).log(Level.SEVERE, null, e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }

                if (connect != null) {
                    connect.close();

                }
            } catch (SQLException ex) {
                Logger.getLogger(Hoose.class
                        .getName()).log(Level.SEVERE, null, ex);
            }

        }
        System.out.println(allFilms.size() + " total published films.");
        return allFilms;
    }

    public static void setPlayed(int id) {
        Connection connect = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            connect = DriverManager.getConnection("jdbc:mysql://"
                    + credentials.get(0)
                    + "/hoose_wp_2017?user=" + credentials.get(1)
                    + "&password=" + credentials.get(2));

            // Statements allow to issue SQL queries to the database
            statement = connect.createStatement();
            // Result set get the result of the SQL query
            int result = statement.executeUpdate("UPDATE wp_posts SET post_status = \"publish\" WHERE ID=" + id + ";");

            System.out.println(": " + result);

        } catch (Exception e) {
            Logger.getLogger(Hoose.class
                    .getName()).log(Level.SEVERE, null, e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }

                if (connect != null) {
                    connect.close();

                }
            } catch (SQLException ex) {
                Logger.getLogger(Hoose.class
                        .getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

    private static void writeMetaData(ResultSet resultSet) throws SQLException {
        //  Now get some metadata from the database
        // Result set get the result of the SQL query

        System.out.println("The columns in the table are: ");

        System.out.println("Table: " + resultSet.getMetaData().getTableName(1));
        for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
            System.out.println("Column " + i + " " + resultSet.getMetaData().getColumnName(i));
        }
    }

}
