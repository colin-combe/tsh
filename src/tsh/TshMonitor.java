package tsh;

import udp.SendUDPWord;
import java.util.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;

public class TshMonitor {

    //  >> Here are your controls, John <<
    private static final String boxIP = "127.0.0.1";//you'll need to change this
    private static final int boxPort = 5010;
    
    private static final String defaultTrafficClip = "TRAFFIC7";
    
    // traffic clips
    private static String[] trafficClips = new String[72];
    
    //For levels it goes:
    private static final Float high = 0.6f;
    private static final Float midHigh = 0.4f;
    private static final Float midLow = 0.2f;

    //For level change bandings (normstable, etc) it now goes
    private static final Float highChange = 0.5f;
    private static final Float lowChange = 0.2f;

    //traffic cut offs - don't really fiddle with these or you traffic flow rate averages wont be what you think
//    private static final int highTruck = 500;
//    private static final int highCar = 1500;

    private static final Float noChangeTolerance = 0.01f;
    
    //private static final Map<String, CSVRecord> trafficMap = new HashMap<String, CSVRecord>();
    private static boolean loop = true;

    public static void main(String argv[]) {
        Calendar cal = Calendar.getInstance();
        //the default setting - if not fri or sat
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
        System.out.println("> " + dayOfWeek + " - " + hourOfDay);
       
        // DAY 0     
        //   10AM
        trafficClips[0] = "TRAFFIC1";       
        //   11PM
        trafficClips[1] = "TRAFFIC2";     
        //   12PM
        trafficClips[2] = "TRAFFIC2";   
        //    13PM
        trafficClips[3] = "TRAFFIC2";       
        //    14PM
        trafficClips[4] = "TRAFFIC2";  
        //    15PM
        trafficClips[5] = "TRAFFIC21";       
        //    16PM
        trafficClips[6] = "TRAFFIC3";
        //    17PM
        trafficClips[7] = "TRAFFIC2";
        //    18PM
        trafficClips[8] = "TRAFFIC1";
        //    19PM
        trafficClips[9] = "TRAFFIC7";
        //    20PM
        trafficClips[10] = "TRAFFIC7";
        
        // DAY 1
        //   10AM
        trafficClips[11] = "TRAFFIC27";       
        //   11PM
        trafficClips[12] = "TRAFFIC28";     
        //   12PM
        trafficClips[13] = "TRAFFIC28";   
        //   13PM
        trafficClips[14] = "TRAFFIC28";       
        //   14PM
        trafficClips[15] = "TRAFFIC28";  
        //   15PM
        trafficClips[16] = "TRAFFIC28";       
        //   16PM
        trafficClips[17] = "TRAFFIC29";
        //   17PM
        trafficClips[18] = "TRAFFIC4";
        //   18PM
        trafficClips[19] = "TRAFFIC12";
        //   19PM
        trafficClips[20] = "TRAFFIC30";
        //   20PM
        trafficClips[21] = "TRAFFIC31";
        
        // DAY 2
        //   10AM
        trafficClips[22] = "TRAFFIC32";       
        //   11PM
        trafficClips[23] = "TRAFFIC25";     
        //   12PM
        trafficClips[24] = "TRAFFIC25";   
        //    13PM
        trafficClips[25] = "TRAFFIC26";       
        //    14PM
        trafficClips[26] = "TRAFFIC14";  
        //    15PM
        trafficClips[27] = "TRAFFIC5";       
        //    16PM
        trafficClips[28] = "TRAFFIC5";
        //    17PM
        trafficClips[29] = "TRAFFIC14";
        //    18PM
        trafficClips[30] = "TRAFFIC6";
        //    19PM
        trafficClips[31] = "TRAFFIC7";
        //    20PM
        trafficClips[32] = "TRAFFIC7";
        
        
         
        while (loop) {
            try {
                makeWords();
            } catch (Exception ex) {
                ex.printStackTrace();
            } 
            if (argv.length == 0) {
                loop = false;
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

    static Pattern garlsCraig = Pattern.compile(".*?Value\":\"(.*?)\"}");
    
    public static String getRainData() {
        String inputString = readURL("https://apps.sepa.org.uk/rainfall/api/Hourly/301168");
        Matcher m = garlsCraig.matcher(inputString);
        String lastValue = null;
        
        while (m.find()) {
            lastValue = m.group(1);
//            System.out.println(m.group(1));
        }
        return lastValue;
    }

    public static String[] getLevelData() {
        String dataString = readURL("https://apps.sepa.org.uk/database/riverlevels/504722-SG.csv");

        String lines[] = dataString.split("\\r?\\n");
        int linesLen = lines.length;

        float pv = Float.parseFloat(lines[linesLen - 2].split(",")[1]);
        float v = Float.parseFloat(lines[linesLen - 1].split(",")[1]);

        String changeWord;
        //System.out.println(dataString);
        if (v > TshMonitor.highChange) {
            changeWord = "HIGH";
        } else if (v < TshMonitor.lowChange) {
            changeWord = "LOW";
        } else {
            changeWord = "NORM";
        }
        float diff = Math.abs(v - pv);
        if (v > pv && diff > TshMonitor.noChangeTolerance && v < TshMonitor.highChange) {
            changeWord += "RISE";
        } else if (v < pv && diff > TshMonitor.noChangeTolerance && v > TshMonitor.lowChange) {
            changeWord += "FALL";
        } else {
            changeWord += "STABLE";
        }
        
        String levelWord;
        //System.out.println(dataString);
        if (v > TshMonitor.high) {
            levelWord = "HIGH";
        } else if (v > TshMonitor.midHigh) {
            levelWord = "MIDHIGH";
        } else if (v > TshMonitor.midLow) {
            levelWord = "MIDLOW";
        } else {
            levelWord = "LOW";
        }
        
        System.out.println(levelWord + " " + changeWord + " [" + pv + " > " + v + "]");

        return new String[]{levelWord, changeWord};
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
    
    public static String getTrafficWord(){
        Calendar cal = Calendar.getInstance();
        //the default setting - if sunday
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek > 1 || dayOfWeek < 5) {
            return defaultTrafficClip;
        }
        else {
            int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
            if (hourOfDay > 8 && hourOfDay < 21) {
                if (dayOfWeek == 1){
                    dayOfWeek = 8;
                }
                int clipIndex = hourOfDay - 9 + ((dayOfWeek - 5) * 12);
                return trafficClips[clipIndex];
            } else {
               return defaultTrafficClip; 
            }
        }        
    }

    public static void makeWords() {
        String time = now();
        System.out.println("*Cinema programme " + time +"* - should update in 1/4 hour");
        boolean rainy = hasRainedThisHour();
        System.out.println("Has rained this hour? " + rainy);
        String[] levels = getLevelData();
        System.out.println("River level:" + levels[0] + " " +  levels[1]);
        String trafficWord = getTrafficWord();
                
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
        int udpFreq = 100; //every 1/10 secs (same interval as berwick time)
        int udpSendCount = (millisInHour / udpFreq) / 4;// quarterHourly updates;
        System.out.println("sending " + rainWord + levels[0] + " " +  levels[1] 
                + " " +  trafficWord + " " + udpSendCount + " times");
        
        int wordCount = 3;
        
        for (int u = 0; u < udpSendCount; u++) {
            new SendUDPWord(boxIP, boxPort, rainWord + levels[0]);
            try {
                Thread.sleep(udpFreq / wordCount);
            } catch (InterruptedException ex) {
                Logger.getLogger(TshMonitor.class.getName()).log(Level.SEVERE, null, ex);
            }

            new SendUDPWord(boxIP, boxPort, levels[1]);
            try {
                Thread.sleep(udpFreq / wordCount);
            } catch (InterruptedException ex) {
                Logger.getLogger(TshMonitor.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            new SendUDPWord(boxIP, boxPort, trafficWord);
            try {
                Thread.sleep(udpFreq / wordCount);
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
