package tsh;

import java.util.*;
import java.io.*;
import javax.mail.*;
import com.sun.mail.imap.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.search.SubjectTerm;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class TshMonitor {

    //  >> Here are your controls, John <<
    private static final String boxIP = "127.0.0.1";//you'll need to change this
    private static final int boxPort = 5010;

    private static final Float highThreshhold = 0.1f;//you'll want to change this
    private static final Float lowThreshhold = 0.04f;//you'll want to change this

    private static final Float noChangeTolerance = 0.01f;

    //dont play with this tho
//    private static SortedMap<Date, Float[]> readings = new TreeMap<Date, Float[]>();
//    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    public static void main(String argv[]) {
        while (true) {//
            try {
//            System.out.println("Has rained this hour? " + hasRainedThisHour());
//            System.out.println("River level:" + getLevelData());
                makeWords();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
//                try {
////                    folder.close(false);
////                    store.close();
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
            }
        }
    }

    public static boolean hasRainedThisHour() {
        String rainfallThisHour = getRainData();
        float rain;
        try {
            rain = Float.parseFloat(rainfallThisHour);
        } catch (NumberFormatException e) {
            //if dont get data for any reason default to 'not raining'
            return false;
        }

        if (rain > 0) {
            return true;
        } else {
            return false;
        }
    }

    static Pattern garlsCraig = Pattern.compile("\\s.*Garls Craig \\- (.*)mm.*");

    public static String getRainData() {
        String inputString = readURL("http://beta.sepa.org.uk/rainfall/#301168");
        //String inputString = ("tionHeader'>Garls CraGarls Craig - 2.4mm to 12/02/2017 06:00 GM");
        // System.out.println();
        Matcher m = garlsCraig.matcher(inputString);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public static String getLevelData() {
        String dataString = readURL("http://apps.sepa.org.uk/database/riverlevels/504722-SG.csv");

        String lines[] = dataString.split("\\r?\\n");
        int linesLen = lines.length;

        float pv = Float.parseFloat(lines[linesLen - 2].split(",")[1]);
        float v = Float.parseFloat(lines[linesLen - 1].split(",")[1]);

        String word = "";
        //System.out.println(dataString);
        if (v > TshMonitor.highThreshhold) {
            word += "HIGH";
        } else if (v < TshMonitor.lowThreshhold) {
            word += "LOW";
        } else {
            word += "NORM";
        }
        float diff = Math.abs(v - pv);
        if (v > pv && diff > TshMonitor.noChangeTolerance && v < TshMonitor.highThreshhold) {
            word += "RISE";
        } else if (v < pv && diff > TshMonitor.noChangeTolerance && v > TshMonitor.lowThreshhold) {
            word += "FALL";
        } else {
            word += "STABLE";
        }
        System.out.println(word + " [" + pv + " > " + v + "]");

        return word;
    }

    public static String readURL(String url) {
        InputStream in = null;
        String dataString = null;
        try {
            in = new URL(url).openStream();
            dataString = IOUtils.toString(in);
        } catch (Exception ex) {
            Logger.getLogger(TshMonitor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(TshMonitor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return dataString;
    }

    public static void makeWords() {
        String time = now();
        System.out.println("*Cinema programme " + time +"* - should update in 1/4 hour");
        boolean rainy = hasRainedThisHour();
        System.out.println("Has rained this hour? " + rainy);
        String level = getLevelData();
        System.out.println("River level:" + level);

        String rainWord = "FAIR";
        if (rainy) {
            rainWord = ("RAIN");
        }

        /*
        //UDP send
        
        //SHOULD CHANGE QUARTER HOURLY
 
         */
        int millisInHour = 1000 * 60 * 60;
        //take few seconds off - think this will help avoid gaps
        millisInHour -= 3 * 1000;
        int udpFreq = 1000; //every 1 secs

//        int wordCount = words.length;
//        for (int w = 0; w < wordCount; w++) {
//            String word = words[w];
        int udpSendCount = (millisInHour / udpFreq) / 4;// wordCount;
        System.out.println("sending " + rainWord + " " + level + " " + udpSendCount + " times");
        for (int u = 0; u < udpSendCount; u++) {
            new SendUDPWord(boxIP, boxPort, rainWord);
            try {
                Thread.sleep(udpFreq / 2);
            } catch (InterruptedException ex) {
                Logger.getLogger(TshMonitor.class.getName()).log(Level.SEVERE, null, ex);
            }
            new SendUDPWord(boxIP, boxPort, level);
            try {
                Thread.sleep(udpFreq / 2);
            } catch (InterruptedException ex) {
                Logger.getLogger(TshMonitor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    public static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }
}
