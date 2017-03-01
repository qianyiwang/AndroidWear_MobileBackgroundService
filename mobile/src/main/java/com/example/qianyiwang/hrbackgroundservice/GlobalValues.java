package com.example.qianyiwang.hrbackgroundservice;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by qianyiwang on 2/8/17.
 */

public class GlobalValues {
    public static Socket socket = null;
    public static DatagramSocket udp_socket = null;
    public static InetAddress udpAddress;
    public static String udp_address = "192.168.0.86";
    public static int udpPort = 8001;
}
