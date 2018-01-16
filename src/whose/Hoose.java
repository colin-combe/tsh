package whose;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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

public class Hoose {

    //  >> Here are your controls, John <<
    private static final String boxIP = "192.168.0.104";//you'll need to change this
    private static final int port = 7474;//and this

    private static DatagramSocket socket = null;
    private static InetAddress boxAddress = null;

    private static List<String> credentials;
    private static int calmCount = 0;
    private static int calmPosition = -1;
    private static Random randomGenerator = new Random();

    public static void main(String[] args) {
        try {
            credentials = Files.readAllLines(Paths.get("C:/Users/Oven/cred.txt"));
        } catch (IOException ex) {
            Logger.getLogger(Hoose.class.getName()).log(Level.SEVERE, null, ex);
        }
        //System.out.println(credentials.get(1));

        try {
            boxAddress = InetAddress.getByName(boxIP);
        } catch (UnknownHostException ex) {
            Logger.getLogger(Hoose.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            socket = new DatagramSocket(port, InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
        } catch (UnknownHostException ex) {
            Logger.getLogger(Hoose.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SocketException ex) {
            Logger.getLogger(Hoose.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (socket != null) {
            try {
                while (true) {
                    loop();
                }
            } catch (IOException ex) {
                Logger.getLogger(Hoose.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(Hoose.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                socket.close();
            }
        }

//        getNextFilm();
//        getPlaylist();
//        getAllFilms();
//        setPlayed(107);
    }

    static void loop() throws IOException, InterruptedException {

        PlaylistItem nextFilm = getNextFilm();
        String udpWord = nextFilm.name;

        byte[] sendData = new byte[1024];
        sendData = udpWord.getBytes();

        if (udpWord.startsWith("calm")) {
            //send udp word for a while
            int secsToSend = 27;
            int udpFreq = 100; //every 1/10 secs (same interval as berwick time)

            int udpSendCount = secsToSend * 1000 / udpFreq;// quarterHourly updates;

            System.out.println("sending " + udpWord + " " + udpSendCount + " times");
            for (int u = 0; u < udpSendCount; u++) {
                // System.out.println("Attempting to send data:" + s1);
                DatagramPacket sendPacket
                        = new DatagramPacket(sendData, sendData.length, boxAddress, port);
                socket.send(sendPacket);
                Thread.sleep(udpFreq);
            }
        } else {
            boolean confirmed = false;
            while (!confirmed) {
                DatagramPacket sendPacket
                        = new DatagramPacket(sendData, sendData.length, boxAddress, port);
                socket.send(sendPacket);
                byte[] receiveData = new byte[16];

                DatagramPacket receivePacket = new DatagramPacket(receiveData,
                        receiveData.length);
                socket.setSoTimeout(100);
                try {
                    socket.receive(receivePacket);
                } catch (SocketTimeoutException ste) {
                }
                String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                //System.out.println("RECEIVED: " + sentence);
                if (sentence.startsWith("ply-T"+udpWord.toUpperCase())) {
                    confirmed = true;
                    System.out.println("Yay! Confirmation received: " + sentence);
                    if (nextFilm.id > -1) {
                        setStatus(nextFilm.id, "pending");
                    }
                }

            }
            //listen for interstital starting
            confirmed = false;
            int secsToListen = 34;
            byte[] receiveData = new byte[16];

            DatagramPacket receivePacket = new DatagramPacket(receiveData,
                    receiveData.length);
            //serverSocket = new DatagramSocket(7474,InetAddress.getByName("192.168.0.10"));
//            serverSocket.setSoTimeout(secsToListen * 1000);
            long stopTime = System.currentTimeMillis() + (secsToListen * 1000);
            // && System.currentTimeMillis() < stopTime
            while (!confirmed && System.currentTimeMillis() < stopTime) {
                socket.setSoTimeout(100);
                try {
                    socket.receive(receivePacket);
                } catch (SocketTimeoutException ste) {
                }
                String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                //System.out.println("RECEIVED: " + sentence);
                if (sentence.startsWith("ply-TINTER")) {
                    confirmed = true;
                    System.out.println("Yay! Confirmation received: " + sentence);
                    if (nextFilm.id > -1) {
                        setStatus(nextFilm.id, "publish");
                    }
                }
            }
        }
        System.out.println("proceeding");
    }

    static PlaylistItem getNextFilm() {
        System.out.println();
        System.out.println();
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
                int randChoice = randomGenerator.nextInt(allFilms.size());
                nextFilm = allFilms.get(randChoice);
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
        System.out.println(allFilms.size() + " total published films.");
        return allFilms;
    }

    public static void setStatus(int id, String status) {
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
            int result = statement.executeUpdate("UPDATE wp_posts SET post_status = \""+status+"\" WHERE ID=" + id + ";");

            System.out.println("Set post_status for " + id + " to "+status+"; result: " + result);

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
