# UsbTerminal

This is an Android app that emulates a terminal (sometimes called a "monitor"). It is intended to be used with a physical connection to the device
via the phone or tablet’s USB port.
The phone or tablet must support USB-Host Mode a.k.a USB On-The-Go (USB-OTG),
and a USB-OTG cable is required.
Typical use-cases for this app are:
* Controlling an IoT device like an Arduino, ESP32, etc
* Controlling a communication device such as a router that has a serial console connector (this may require a USB to RS232 converter cable)

The app is available at the [Google Play Store](https://play.google.com/store/apps/details?id=com.liorhass.android.usbterminal.free)

<img src="Art/Screenshots/Screenshot_01.png" width="300">

[More screenshots](Art/Screenshots/README.md) 

## Features:
* Support devices with the following USB to Serial protocols/chips
  * CDC-ACM based devices (e.g. Arduino Uno R3)
  * FTDI based devices (Devices using FT232R, FT232H, FT2232H, FT4232H,
    FT230X, FT231X, FT234XD)
  * Prolific PL2303
  * Qinheng CH340, CH341A
  * Silabs CP2102, CP210x (e.g. most ESP32 DevKit boards from Espressif)
* Support two keyboard input modes (determined in _Settings_):
  1. Auto - Work as if you’re on a “real” terminal.
     Characters are sent to the serial device immediately as keys are
     clicked on the keyboard. There is no dedicated input field or any
     Of that rubbish. This is the default mode
  2. Dedicated input field - If you insist, you can have a dedicated
     Input field. Keyboard input goes to that input
     field and is sent to the device only after a “Send” button is pressed
* Partial support of ANSI/VT100 escape sequences including text coloring. See
  [Supported escape sequences](#Supported escape sequences) below
* Two display modes: Text and Hex
  1. Text - “Normal” text terminal mode
  2. Hex - Data is displayed as Hex-dump
  
  The user can switch between these modes back and forth without loosing
  data
* Background communication - the app can maintain connection and
  continue receiving data even when it is in the background (not the app
  the user is currently interacting with)
* Log sessions to files. These log files can then be viewed or shared in
  order to be analyzed with external tools
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
* Built-in help. No need to be online to read the help screen
* Built-in shortcut to reset an Arduino (by pulling both DTR and RTS
  low for 10mSec)
* Build-in shortcut to reset an ESP32 dev-board (by setting both DTR and
  RTS to high for 200mSec, then pulling RTS low for 400mSec then setting
  it high again)
* No root required

### A note to Arduino users:
One advantage of UsbTerminal is the way it handles DTR. Typically when an
Arduino board is connected to a PC, it will reboot every time a terminal
emulator application is launched and connected to its serial port. This is
because the PC drops the DTR signal low whenever a connection is formed, and
Arduino is designed to reset when the DTR line is dropped low. UsbTerminal on
the other hand, doesn’t automatically set or reset the DTR signal. When you
connect a phone or tablet to an Arduino and open UsbTerminal, your Arduino
continues whatever it was doing at the time. If you want it to reboot, you can
easily do so using the built-in shortcut.

## Supported escape sequences
UsbTerminal supports some control characters and a subset of
[ANSI/VT100 escape sequences](https://en.wikipedia.org/wiki/ANSI_escape_code).
In particular, it supports all the escape-sequences used by the
[`linenoise`](https://github.com/antirez/linenoise) library which is used
by the ESP32’s [console module](https://docs.espressif.com/projects/esp-idf/en/latest/esp32/api-reference/system/console.html).

### Supported control characters and escape codes
```
* \n - New line. Cursor moved one line down and to start of line
* \r - Carriage return. Cursor move to start of line
* \b - Backspace. Cursor moved left one place unless it’s at the line’s beginning
* \t - Tab - Move cursor forward to next tab-stop (tab stops are 8 characters apart)
* \007 - ^G Bell. make a beep sound
* Esc [ Pn A - Cursor Up. Pn is considered 1 if it's missing, 0 or 1
* Esc [ Pn B - Cursor Down. Pn is considered 1 if it's missing, 0 or 1
* Esc [ Pn C - Cursor Forward. Pn is considered 1 if it's missing, 0 or 1
* Esc [ Pn D - Cursor Backward. Pn is considered 1 if it's missing, 0 or 1
* Esc [ Ps K - Ps 0 or missing - Erase from cursor to EOL
               Ps 1 - Erase from beginning of line to cursor
               Ps 2 - Erase line containing cursor
* Esc [ Ps J - Ps 0 or missing - Erase from cursor to end of screen
               Ps 2 - Erase entire screen
* Esc [ H    - Cursor to Home Position.
* Esc H      - Cursor to Home Position. (Same as Esc[H)
* Esc [ Pn ; Pn H - Cursor Position. First parameter is line number and
                    the second is column
* Esc [ Ps n - DSR (Device Status Report)
               Ps 6 - Terminal reports the current cursor position as
                      ESC [ n ; m R  where n is the row and m is the column
               Ps 5 - Terminal sends “OK” status report as Esc [ 0 n
* Esc [ ps ; ps ; ps... m - Select Graphic Rendition
               Ps 0 - Clear all graphic renditions
               Ps 30..37 and 90..97 - Set text color.
```
For mapping of code to color when selecting graphic rendition see
[ANSI Escape Codes Colors](https://en.wikipedia.org/wiki/ANSI_escape_code#Colors).

If you find some unsupported escape-codes that are important to you, please
[create an issue](https://github.com/liorhass/UsbTerminal/issues) or drop
me an email.


## Credits
UsbTerminal uses these libraries:

[usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)

[Compose-Settings](https://github.com/alorma/Compose-Settings)



<!--
Basic markdown syntax:
https://docs.github.com/en/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax
-->



