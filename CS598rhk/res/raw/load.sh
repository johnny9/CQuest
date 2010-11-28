#!/system/bin/sh
insmod /data/data/edu.illinois.CS598rhk/bin/crc7.ko
insmod /data/data/edu.illinois.CS598rhk/bin/compat.ko
insmod /data/data/edu.illinois.CS598rhk/bin/cfg80211.ko
insmod /data/data/edu.illinois.CS598rhk/bin/mac80211.ko
insmod /data/data/edu.illinois.CS598rhk/bin/wl1251.ko
insmod /data/data/edu.illinois.CS598rhk/bin/wl1251_sdio.ko
insmod /data/data/edu.illinois.CS598rhk/bin/msm_wifi.ko
sleep 5
iw dev wlan0 set power_save off
iw dev wlan0 set type ibss
ifconfig wlan0 up $1
iw dev wlan0 ibss join G1Tether 2437 fixed-freq 00:11:22:33:44:55

