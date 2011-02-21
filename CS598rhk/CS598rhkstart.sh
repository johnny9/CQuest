#!/bin/bash
#Automation script for our bluetooth coordination software

adb=~/android-sdk-linux_x86/tools/adb
package_name=edu.illinois.CS598rhk
activity_name=AutoStartActivity
apk_location=CS598rhk.apk
ClockSync_location=ClockSync.apk
subnet=192.168.1.
devices=( `./adb devices | sed '/List of devices attached/d;s/\tdevice//g' ` )

start_activity ()
{
   for (( i = 0 ; i < ${#devices[@]} ; i++ ))  
   do 
       d=${devices[$i]}
       #write config file
       addr=$(($i + $1))
       $adb -s $d shell "mkdir /data/data/$package_name/files/"
       $adb -s $d shell "echo -e '$subnet$addr\n' > /data/data/$package_name/files/config"

       $adb -s $d shell am start -a android.intent.action.MAIN -c \
             android.intent.category.LAUNCHER -n \
             $package_name/$package_name.$activity_name &
       
   done
}

rebootall ()
{
   for d in ${devices[@]}  
   do 
       $adb -s $d shell reboot
   done
}

reinstall ()
{
   for d in ${devices[@]}  
   do 
      $adb -s $d install -r $apk_location
   done
}

sync_install ()
{
   for d in ${devices[@]}  
   do 
      $adb -s $d install -r $ClockSync_location
   done
}

uninstall ()
{
   for d in ${devices[@]}  
   do 
      $adb -s $d uninstall $package_name
   done
}

discoverable ()
{
   for d in ${devices[@]}  
   do 
      $adb -s $d shell am start -a android.bluetooth.adapter.action.REQUEST_ENABLE
      $adb -s $d shell am start -a android.bluetooth.adapter.action.REQUEST_DISCOVERABLE
   done
}


print_usage ()
{
   echo -e "CS598rhkstart.sh uninstall \t\t -removes CS598rhk from all devices"
   echo -e "CS598rhkstart.sh reinstall \t\t -reinstalls CS598rhk on all devices"
   echo -e "CS598rhkstart.sh rebootall \t\t -reboots all devices"
   echo -e "CS598rhkstart.sh start [octet] \t -starts CS598rhk on all devices, octet is last number in wifi ip"
   echo -e "CS598rhkstart.sh log \t\t\t -grabs the logs off all devices"
   echo -e "CS598rhkstart.sh discoverable \t\t\t -sets all devices to be discoverable over bt"
}

grab_logs ()
{
   for d in ${devices[@]}  
   do 
       $adb -s $d pull "/data/data/$package_name/files/power_log" "power_log.$d.csv"
       $adb -s $d pull "/data/data/$package_name/files/wifi_discovery" "wifi_discovery.$d.csv"	
       $adb -s $d pull "/data/data/$package_name/files/new_neighbor" "new_neighbor.$d.csv"
   done
}

clear_logs ()
{
   for d in ${devices[@]}  
   do 
       $adb -s $d shell rm "/data/data/$package_name/files/power_log" 
       $adb -s $d shell rm "/data/data/$package_name/files/wifi_discovery"	
       $adb -s $d shell rm "/data/data/$package_name/files/new_neighbor" 
   done
}


if [ $# -lt 1 ]; then
   print_usage
elif [ $1 = "uninstall" ]; then 
   uninstall
elif [ $1 = "reinstall" ]; then 
   reinstall
elif [ $1 = "rebootall" ]; then 
   rebootall
elif [ $1 = "discoverable" ]; then 
   discoverable
elif [ $1 = "sync" ]; then 
   sync_install
elif [ $1 = "start" ]; then 
   reinstall
   if [ $# -eq 2 ]; then
      start_activity $2
   else
      start_activity "1"
   fi
elif [ $1 = "log" ]; then 
   grab_logs
elif [ $1 = "clearlog" ]; then 
   clear_logs
else
   print_usage
fi
 
