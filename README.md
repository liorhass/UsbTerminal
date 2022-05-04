# UsbTerminal

This is an Android app that acts as a terminal emulator (sometimes called "monitor"). It is intended to be used with a physical connection to the device
via the phone or tablet’s USB port.
The phone or tablet must support USB-Host Mode a.k.a USB On-The-Go (USB-OTG),
and a USB-OTG cable is required.
Typical use-cases for this app are:
* Controlling an IoT device like an Arduino, ESP32, etc
* Controlling a communication device such as a router that has a serial console connector (this may require a USB to RS232 converter cable)

## Features:
* Support devices with the following USB to Serial protocols/chips
  * CDC-ACM based devices (e.g. Arduino Uno R3)
  * FTDI based devices (Devices using FT232R, FT232H, FT2232H, FT4232H,
    FT230X, FT231X, FT234XD)
  * Prolific PL2303
  * Qinheng CH340, CH341A
  * Silabs CP2102, CP210x (e.g. most ESP32 DevKit boards from Espressif)
* Support two keyboard input modes:
  1. Auto - Like on a “real” terminal, there is no dedicated input field.
     Characters are sent to the serial device immediately as keys are
     clicked on the keyboard. This is the default mode
  2. Dedicated input field - Keyboard input goes to a dedicated input
     field and is sent to the device only after a “Send” button is pressed
* Partial support of VT100 escape codes including text coloring
* Two display modes: Text and Hex
  1. Text - “Normal” text terminal mode
  2. Hex - Data is displayed as Hex-dump
  
  The user can switch between these modes back and forth without loosing
  data
* Background communication - the app can maintain connection and
  continue receiving data even when it is in the background (not the app
  the user is currently interacting with)
* Log sessions to files. These log files can then be shared in order to
  be viewed or analyzed with external tools
* Extensive built-in list of Vendor-ID/Product-ID (VID/PID) pairs that
  map to the appropriate device type. This means that usually the app
  detects on its own the type of device. In case the app encounters an
  unrecognized VID/PID pair, it allows the user to specify the device
  type (provided it is one of the five supported types - i.e. it is 
  one of CDC/ACM, FTDI, PL2303, CH34x, CP21xx)
* Support sending control character (e.g. Ctrl-C)
* Support controlling of DTR and CTS
* Large scroll-back buffer (configurable scroll-back buffer size)
* Blinking cursor that indicates the current cursor position on the screen
* Status line indicating connection state, error messages, screen size,
  cursor location and display mode
* Build-in help. No need to be online to read the help screen
* Build-in shortcut to reset an Arduino (by pulling both DTR and RTS
  low for 10mSec)
* Build-in shortcut to reset an ESP32 dev-board (by setting both DTR and
  RTS to high, then pulling RTS low for 400mSec then setting it high again)
* No root required

A note to Arduino users:
One advantage of UsbTerminal is the way it handles DTR. Typically when an Arduino board is connected to a PC, it will reboot every time a terminal emulator application is launched and connected to its serial port. This is because the PC drops the DTR signal low whenever a connection is formed, and Arduino is designed to reset when the DTR line is dropped low. UsbTerminal on the other hand, doesn’t automatically set or reset the DTR signal. When you connect a phone or tablet to an Arduino and open UsbTerminal, your Arduino continues whatever it was doing at the time. If you want it to reboot, you can easily do so using the built-in shortcut.


