package berwick;

import tsh.SendUDPWord;
import java.util.*;
import java.io.*;
import javax.mail.*;
import com.sun.mail.imap.*;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.search.SubjectTerm;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class monitor {

    //  >> Here are your controls, John <<
    private static final String boxIP = "127.0.0.1";//you'll need to change this
    private static final int boxPort = 5010;

    private static final Float highThreshhold = 2.5f;
    private static final Float lowThreshhold = 0.25f;

    private static final Float noChangeTolerance = 0.01f;

    //dont play with this tho
    private static SortedMap<Date, Float[]> readings = new TreeMap<Date, Float[]>();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

    public static void main(String argv[]) {
        while (true) {//
            readings = new TreeMap<Date, Float[]>();

            IMAPFolder folder = null;
            Store store = null;
            try {

                Properties props = new Properties();
                props.put("mail.store.protocol", "imaps");

                Session session;

                session = Session.getDefaultInstance(props, null);
                store = session.getStore("imaps");
                store.connect("imap.gmail.com", "watergauge78@gmail.com", "wateryFlows");

                folder = (IMAPFolder) store.getFolder("Inbox");
                folder.open(Folder.READ_ONLY);

                Message[] messages = folder.search(new SubjectTerm("PLUMPEtest"));

                System.out.println(messages.length + " messages with subject PLUMPEtest");

                for (int m = messages.length - 1; m > (messages.length - 3); m--) {
//            for (int m = 0; m < messages.length; m++) {
                    Message message = messages[m];
                    readMessage(message);
                }

                makeWords();

            // code to write out all values
//            String allPLUMPE = "Date, PlumpeBridge, SolwayBank\n";
//            for (Map.Entry<Date, Float[]> entry : readings.entrySet()) {
//                Date key = entry.getKey();
//                Float[] value = entry.getValue();
//                allPLUMPE += key.toString() + ", " + value[0]
//                        + ", " + value[1] + "\n";
//            }
//
//            File f = new File("/tmp/allPLUMPE.csv");
//            FileOutputStream fos = new FileOutputStream(f);
//            PrintStream pos = new PrintStream(fos);
//            pos.print(allPLUMPE);
//            pos.close();
                System.out.println("MeasurementCount: " + readings.values().size());

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    folder.close(false);
                    store.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static void readMessage(Message message) {
        try {
            String subject = message.getSubject();
            System.out.println(message.getReceivedDate() + " " + subject);
            if (subject.equals("PLUMPEtest")) {
                Multipart multipart = (Multipart) message.getContent();

                // System.out.println(multipart.getCount());
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())
                            && !StringUtils.isNotBlank(bodyPart.getFileName())) {
                        continue; // dealing with attachments only
                    }
                    System.out.println("Yay! Got attachment");
                    InputStream is = bodyPart.getInputStream();
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(is, writer);//, encoding);

                    String theString = writer.toString();

//                    File f = new File("/tmp/" + bodyPart.getFileName());
//                    FileOutputStream fos = new FileOutputStream(f);
//                    PrintStream pos = new PrintStream(fos);
//                    pos.print(theString);
//                    pos.close();

                    String lines[] = theString.split("\\r?\\n");
                    int index = 0;
                    for (int l = 0; l < lines.length; l++) {
                        String line = lines[l];
                        if (!line.startsWith("#")) {
//                            System.out.println(lines[l]);
                            String[] fields = line.split(" ");
                            Date date = sdf.parse(fields[0]);
                            Float[] measurements = readings.get(date);
                            if (measurements == null) {
                                measurements = new Float[2];
                                readings.put(date, measurements);
                            }
                            Float measurement = Float.parseFloat(fields[1]);
                            measurements[index] = measurement;
                        } else if (line.contains("Dumfries/Plumpe Bridge")) {
                            index = 0;
                        } else if (line.contains("Dumfries/Solwaybank")) {
                            index = 1;
                        }
                    }
                }
            } else {
                System.out.println("Nah... ignoring.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void makeWords() {

        String[] words = new String[4];

        Set<Entry<Date, Float[]>> s = readings.entrySet();
        Object[] array = s.toArray();
        String rainCheck = "FAIR";

        int c = array.length;

        System.out.println();
        System.out.println("*Cinema programme*");

        for (int i = 0; i < 4; i++) {
            String word = "";
            Entry<Date, Float[]> entry = (Entry<Date, Float[]>) array[c - 4 + i];
            Float v = entry.getValue()[0];

            Entry<Date, Float[]> prevEntry = (Entry<Date, Float[]>) array[c - 5 + i];
            Float pv = prevEntry.getValue()[0];

            if (v > monitor.highThreshhold) {
                word += "HIGH";
            } else if (v < monitor.lowThreshhold) {
                word += "LOW";
            } else {
                word += "NORM";
            }
            float diff = Math.abs(v - pv);
            if (v > pv && diff > noChangeTolerance && v < monitor.highThreshhold) {
                word += "RISE";
            } else if (v < pv && diff > noChangeTolerance  && v > monitor.lowThreshhold) {
                word += "FALL";
            } else {
                word += "STABLE";
            }
            words[i] = word;
            System.out.println(word + " [" + pv + " > " + v + "][" + entry.getValue()[1] + "]");

            Float rainGauge = entry.getValue()[1];
            if (rainGauge != null && rainGauge > 0) {
                rainCheck = "RAIN";
            }
        }
        System.out.println(rainCheck);
        System.out.println();

        //UDP send
        int millisInHour = 1000 * 60 * 60;
        //take few seconds off - think this will help avoid gaps
        millisInHour -= 3 * 1000;
        int udpFreq = 100; //every 10 secs
        int wordCount = words.length;

        for (int w = 0; w < wordCount; w++) {
            String word = words[w];
            int udpSendCount = (millisInHour / udpFreq) / wordCount;
            System.out.println("sending " + word + " " + rainCheck + " " + udpSendCount + " times");
            for (int u = 0; u < udpSendCount; u++) {
                new SendUDPWord(boxIP, boxPort, word);
                try {
                    Thread.sleep(udpFreq / 2);
                } catch (InterruptedException ex) {
                    Logger.getLogger(monitor.class.getName()).log(Level.SEVERE, null, ex);
                }
                new SendUDPWord(boxIP, boxPort, rainCheck);
                try {
                    Thread.sleep(udpFreq / 2);
                } catch (InterruptedException ex) {
                    Logger.getLogger(monitor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
