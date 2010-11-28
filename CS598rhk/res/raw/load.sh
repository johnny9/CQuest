#!/system/bin/sh
insmod /data/data/edu.illinois.CS598rhk/bin/crc7.ko
insmod /data/data/edu.illinois.CS598rhk/bin/compat.ko
insmod /data/data/edu.illinois.CS598rhk/bin/cfg80211.ko
insmod /data/data/edu.illinois.CS598rhk/bin/mac80211.ko
insmod /data/data/edu.illinois.CS598rhk/bin/wl1251.ko
insmod /data/data/edu.illinois.CS598rhk/bin/wl1251_sdio.ko
insmod /data/data/edu.illinois.CS598rhk/bin/msm_wifi.ko
iw dev wlan0 set power_save off
iw dev wlan0 set type ibss

