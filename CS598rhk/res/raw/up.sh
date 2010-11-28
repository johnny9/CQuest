#!/system/bin/sh
ifconfig wlan0 $1 up
iw dev wlan0 ibss join G1Tether 2437 00:11:22:33:44:55
