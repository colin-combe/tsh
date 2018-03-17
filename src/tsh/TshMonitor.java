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
    private static final int highTruck = 500;
    private static final int highCar = 1500;

    private static final Float noChangeTolerance = 0.01f;
    
    //private static final Map<String, CSVRecord> trafficMap = new HashMap<String, CSVRecord>();

    public static void main(String argv[]) {
        Calendar cal = Calendar.getInstance();
        //the default setting - if not fri or sat
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
        System.out.println("> " + dayOfWeek + " - " + hourOfDay);
       
        //MONDAY
        //   10AM
        trafficClips[0] = "TRAFFIC1";       
        //   11AM
        trafficClips[1] = "TRAFFIC2";       
        //   12PM
        trafficClips[2] = "TRAFFIC3";     
        //   13PM
        trafficClips[3] = "TRAFFIC4";   
        //    14PM
        trafficClips[4] = "TRAFFIC5";       
        //    15PM
        trafficClips[5] = "TRAFFIC6";  
        //    16PM
        trafficClips[6] = "TRAFFIC7";       
        //    17PM
        trafficClips[7] = "TRAFFIC8";
        //    18PM
        trafficClips[8] = "TRAFFIC9";
        //    19PM
        trafficClips[9] = "TRAFFIC10";
        //    20PM
        trafficClips[10] = "TRAFFIC7";
        //    21PM
        trafficClips[11] = "TRAFFIC7";
        
        //TUESDAY
        //   10AM
        trafficClips[12] = "TRAFFIC7";       
        //   11AM
        trafficClips[13] = "TRAFFIC1";       
        //   12PM
        trafficClips[14] = "TRAFFIC9";     
        //   13PM
        trafficClips[15] = "TRAFFIC9";   
        //    14PM
        trafficClips[16] = "TRAFFIC9";       
        //    15PM
        trafficClips[17] = "TRAFFIC9";  
        //    16PM
        trafficClips[18] = "TRAFFIC1";       
        //    17PM
        trafficClips[19] = "TRAFFIC7";
        //    18PM
        trafficClips[20] = "TRAFFIC7";
        //    19PM
        trafficClips[21] = "TRAFFIC7";
        //    20PM
        trafficClips[22] = "TRAFFIC7";
        //    21PM
        trafficClips[23] = "TRAFFIC7";
        
        //WEDNESDAY
        //   10AM
        trafficClips[24] = "TRAFFIC7";       
        //   11AM
        trafficClips[25] = "TRAFFIC1";       
        //   12PM
        trafficClips[26] = "TRAFFIC16";     
        //   13PM
        trafficClips[27] = "TRAFFIC1";   
        //    14PM
        trafficClips[28] = "TRAFFIC1";       
        //    15PM
        trafficClips[29] = "TRAFFIC12";  
        //    16PM
        trafficClips[30] = "TRAFFIC12";       
        //    17PM
        trafficClips[31] = "TRAFFIC7";
        //    18PM
        trafficClips[32] = "TRAFFIC7";
        //    19PM
        trafficClips[33] = "TRAFFIC7";
        //    20PM
        trafficClips[34] = "TRAFFIC7";
        //    21PM
        trafficClips[35] = "TRAFFIC7";
        
        //THURSDAY
        //   10AM
        trafficClips[36] = "TRAFFIC7";       
        //   11AM
        trafficClips[37] = "TRAFFIC1";       
        //   12PM
        trafficClips[38] = "TRAFFIC9";     
        //   13PM
        trafficClips[39] = "TRAFFIC2";   
        //    14PM
        trafficClips[40] = "TRAFFIC2";       
        //    15PM
        trafficClips[41] = "TRAFFIC2";  
        //    16PM
        trafficClips[42] = "TRAFFIC2";       
        //    17PM
        trafficClips[43] = "TRAFFIC1";
        //    18PM
        trafficClips[44] = "TRAFFIC1";
        //    19PM
        trafficClips[45] = "TRAFFIC17";
        //    20PM
        trafficClips[46] = "TRAFFIC18";
        //    21PM
        trafficClips[47] = "TRAFFIC19";
        
        //FRIDAY
        //   10AM
        trafficClips[48] = "TRAFFIC7";       
        //   11AM
        trafficClips[49] = "TRAFFIC1";       
        //   12PM
        trafficClips[50] = "TRAFFIC3";     
        //   13PM
        trafficClips[51] = "TRAFFIC3";   
        //    14PM
        trafficClips[52] = "TRAFFIC4";       
        //    15PM
        trafficClips[53] = "TRAFFIC4";  
        //    16PM
        trafficClips[54] = "TRAFFIC4";       
        //    17PM
        trafficClips[55] = "TRAFFIC7";
        //    18PM
        trafficClips[56] = "TRAFFIC7";
        //    19PM
        trafficClips[57] = "TRAFFIC7";
        //    20PM
        trafficClips[58] = "TRAFFIC7";
        //    21PM
        trafficClips[59] = "TRAFFIC7";
        
        //SATURDAY
        //   10AM
        trafficClips[60] = "TRAFFIC7";       
        //   11AM
        trafficClips[61] = "TRAFFIC1";       
        //   12PM
        trafficClips[62] = "TRAFFIC5";     
        //   13PM
        trafficClips[63] = "TRAFFIC5";   
        //    14PM
        trafficClips[64] = "TRAFFIC5";       
        //    15PM
        trafficClips[65] = "TRAFFIC6";  
        //    16PM
        trafficClips[66] = "TRAFFIC6";       
        //    17PM
        trafficClips[67] = "TRAFFIC7";
        //    18PM
        trafficClips[68] = "TRAFFIC7";
        //    19PM
        trafficClips[69] = "TRAFFIC7";
        //    20PM
        trafficClips[70] = "TRAFFIC7";
        //    21PM
        trafficClips[71] = "TRAFFIC7";
        
         
        while (true) {
            try {
                makeWords();
            } catch (Exception ex) {
                ex.printStackTrace();
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
        String inputString = readURL("http://apps.sepa.org.uk/rainfall/api/Hourly/301168");
        Matcher m = garlsCraig.matcher(inputString);
        String lastValue = null;
        
        while (m.find()) {
            lastValue = m.group(1);
//            System.out.println(m.group(1));
        }
        return lastValue;
    }

    public static String[] getLevelData() {
        String dataString = readURL("http://apps.sepa.org.uk/database/riverlevels/504722-SG.csv");

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
        if (dayOfWeek == Calendar.SUNDAY) {
            return defaultTrafficClip;
        }
        else {
            int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
            if (hourOfDay > 9 && hourOfDay < 22) {
                int clipIndex = hourOfDay - 10 + ((dayOfWeek - 2) * 12);
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
