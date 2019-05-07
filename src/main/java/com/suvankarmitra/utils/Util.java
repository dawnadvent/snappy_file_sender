package com.suvankarmitra.utils;

import java.io.*;
import java.net.Inet4Address;
import java.net.URL;
import java.net.UnknownHostException;

public class Util {

    public static String checkPublicIP() throws IOException {
        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
        String ip = in.readLine(); //you get the IP as a String
        return ip;
    }

    public static String checkLocalIP() throws UnknownHostException {
        return Inet4Address.getLocalHost().getHostAddress();
    }

    public static String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String s = sw.toString();
        return s;
    }
}
