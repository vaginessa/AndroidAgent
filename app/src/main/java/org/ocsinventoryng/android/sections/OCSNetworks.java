/*
 * Copyright 2013-2016, OCSInventory-NG/AndroidAgent contributors
 *
 * This file is part of OCSInventory-NG/AndroidAgent.
 *
 * OCSInventory-NG/AndroidAgent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OCSInventory-NG/AndroidAgent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OCSInventory-NG/AndroidAgent. If not, see <http://www.gnu.org/licenses/>
 */
package org.ocsinventoryng.android.sections;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import org.ocsinventoryng.android.actions.OCSLog;
import org.ocsinventoryng.android.actions.Utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

public class OCSNetworks implements OCSSectionInterface {
    final private String sectionTag = "NETWORKS";
    private OCSLog ocslog;
    private ArrayList<OCSNetwork> networks;
    private int main = 0;

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public OCSNetworks(Context ctx) {
        ocslog = OCSLog.getInstance();

        this.networks = new ArrayList<OCSNetwork>();

        WifiManager wifii = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        if (wifii != null) {
            if (wifii.getWifiState() == WifiManager.WIFI_STATE_UNKNOWN) {
                return;
            }
            if (wifii.isWifiEnabled()) {
                DhcpInfo d = wifii.getDhcpInfo();

                OCSNetwork netw = new OCSNetwork("Wifi/3G interface");

                if (wifii.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                    netw.setStatus("Up");
                } else {
                    netw.setStatus("Down");
                }

                netw.setIpAdress(Utils.intToIp(d.ipAddress));
                netw.setIpGatewey(Utils.intToIp(d.gateway));
                netw.setIpMask(Utils.intToIp(d.netmask));
                netw.setIpDHCP(Utils.intToIp(d.serverAddress));

                WifiInfo wInfos = wifii.getConnectionInfo();
                netw.setMacaddr(wInfos.getMacAddress());
                netw.setDriver("Wifi");
                netw.setType("Wifi");

                String speed = String.valueOf(wInfos.getLinkSpeed());
                netw.setSpeed(speed + " Mb/s");

                //String s_dns1="DNS 1: "+String.valueOf(d.dns1);
                //String s_dns2="DNS 2: "+String.valueOf(d.dns2);

                networks.add(netw);
            }
        }

        // Check non wifi address, this method will return less informations, but at least we
        // have all the ip
        Enumeration<NetworkInterface> listeNI;
        try {
            listeNI = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            ocslog.error("Error : during call getNetworkInterfaces()");
            ocslog.error(e.getMessage());
            return;
        }
        while (listeNI.hasMoreElements()) {
            NetworkInterface ni = listeNI.nextElement();
            Enumeration<InetAddress> listeIPAdr = ni.getInetAddresses();
            String name = ni.getName();

            ocslog.debug("OCSNET Name :" + ni.getName());
            // android.util.Log.d("OCSNET HAdr", ni.getHardwareAddress());
            while (listeIPAdr.hasMoreElements()) {
                InetAddress ipAdr = listeIPAdr.nextElement();
                if (!ipAdr.isLoopbackAddress() && !ipAdr.isLinkLocalAddress()) {
                    OCSNetwork netw = new OCSNetwork(name);
                    String ipadr = ipAdr.getHostAddress();
                    netw.setIpAdress(ipadr);
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                        try {
                            netw.setMacaddr(Utils.bytesToHex(ni.getHardwareAddress()));
                        } catch (SocketException se) {
                        }
                    }
                    // this ip may be already presents as a wifi address
                    boolean isWifi = false;
                    for (OCSNetwork tmp : networks) {
                        if (tmp.ipAdress.equals(netw.ipAdress)) {
                            isWifi = true;
                            break;
                        }
                    }
                    if (!isWifi) {
                        networks.add(netw);
                    }
                }
            }
        }
    }

    /*
     * ex Linux
     <NETWORKS>
          <DESCRIPTION>eth0</DESCRIPTION>
          <DRIVER>atl1</DRIVER>
          <IPADDRESS>192.168.0.10</IPADDRESS>
          <IPDHCP></IPDHCP>
          <IPGATEWAY>192.168.0.254</IPGATEWAY>
          <IPMASK>255.255.255.0</IPMASK>
          <IPSUBNET>192.168.0"SIMS".0</IPSUBNET>
          <MACADDR>00:1f:c6:b6:a1:1e</MACADDR>
          <PCISLOT>0000:02:00.0</PCISLOT>
          <STATUS>Up</STATUS>
          <TYPE>Ethernet</TYPE>
          <VIRTUALDEV></VIRTUALDEV>
        </NETWORKS>
     */
    public String toXML() {
        StringBuilder strOut = new StringBuilder();
        for (OCSNetwork o : networks) {
            strOut.append(o.toXml());
        }
        return strOut.toString();
    }

    public String toString() {
        StringBuilder strOut = new StringBuilder();
        for (OCSNetwork o : networks) {
            strOut.append(o.toString());
        }
        return strOut.toString();
    }

    public int getMain() {
        return main;
    }

    public ArrayList<OCSNetwork> getNetworks() {
        return networks;
    }

    public ArrayList<OCSSection> getSections() {
        ArrayList<OCSSection> lst = new ArrayList<OCSSection>();
        for (OCSNetwork o : networks) {
            lst.add(o.getSection());
        }
        return lst;
    }

    public String getSectionTag() {
        return sectionTag;
    }
}
