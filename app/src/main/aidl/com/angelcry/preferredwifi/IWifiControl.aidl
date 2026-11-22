package com.angelcry.preferredwifi;

interface IWifiControl {
    String connectToSavedNetwork(String ssid);
    int getProcessUid();
}