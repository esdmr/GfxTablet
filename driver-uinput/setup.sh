#!/bin/bash
set -euo pipefail

# Set up USB tethering for an Android device.
# Usage: adb-usb-tether [USB-VENDOR USB-PRODUCT]
# If USB vendor/product is unspecified, use first USB network interface.
# On the Android side, tethering is enabled via adb shell.

if [[ $# -eq 2 ]]
then
    any=false
    vendor=$1
    product=$2
else
    any=true
fi

function adb() {
   /home/esdmr/.local/share/android/sdk/platform-tools/adb "$@"
}

function find_if() {
    local path if
    for path in /sys/class/net/*
    do
        if=$(basename "$path")
        if [[ "$(readlink "$path")" == */usb* ]]
        then
            local ifproduct ifvendor
            ifproduct=$(cat "$(realpath "$path")/../../../idProduct")
            ifvendor=$(cat "$(realpath "$path")/../../../idVendor")
            if $any || [[ "$ifproduct" == "$product" && "$ifvendor" == "$vendor" ]]
            then
                echo "Found interface: $if" 1>&2
                echo "$if"
                return
            fi
        fi
    done
}

function adb_shell() {
    adb shell "$(printf " %q" "$@")"
}

function adb_su() {
    local quoted
    quoted="$(printf " %q" "$@")"
    adb shell su -c "$(printf %q "$quoted")"
}

if=$(find_if)
if [[ -z "$if" ]]
then
    echo "Requesting interface:" 1>&2
    adb_su setprop sys.usb.config rndis,adb
    echo " >> OK" 1>&2
fi

while [[ -z "$if" ]]
do
    echo "Waiting for network device..." 1>&2
    sleep 1
    if=$(find_if)
done

while ! ( ip link | grep -qF "$if" )
do
    echo "Waiting for interface..." 1>&2
    sleep 1
done

function configure_net() {
    local name="$1"
    local if="$2"
    local ip="$3"
    local table="$4"
    local cmdq="$5" # Query command
    local cmdx="$6" # Configuration command

    if ! ( "$cmdq" ip addr show dev "$if" | grep -qF 192.168.42."$ip" )
    then
        echo "Configuring $name interface address:" 1>&2
        "$cmdx" ip addr add 192.168.42."$ip"/24 dev "$if"
        echo " >> OK" 1>&2
    fi

    if ( "$cmdq" ip addr show dev "$if" | grep -qF 'state DOWN' )
    then
        echo "Bringing $name interface up:" 1>&2
        "$cmdx" ip link set dev "$if" up
        sleep 1
        echo " >> OK" 1>&2
    fi

    if ! ( "$cmdq" ip route show table "$table" | grep -qF "192.168.42.0/24 dev $if" )
    then
        echo "Configuring $name route:" 1>&2
        "$cmdx" ip route add table "$table" 192.168.42.0/24 dev "$if"
        echo " >> OK" 1>&2
    fi
}

configure_net local  "$if"   128 main  command   sudo
configure_net device rndis0  129 local adb_shell adb_su