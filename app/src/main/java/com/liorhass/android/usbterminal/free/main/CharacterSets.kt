// Copyright 2022 Lior Hass
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.liorhass.android.usbterminal.free.main

val iso_8859_1 = charArrayOf(
    '\u0000', // 0 Invalid (ASCII NUL)
    '\u0001', // 1 Unused (ASCII SOH)
    '\u0002', // 2 Unused (ASCII STX)
    '\u0003', // 3 Unused (ASCII ETX)
    '\u0004', // 4 Unused (ASCII EOT)
    '\u0005', // 5 Unused (ASCII ENQ)
    '\u0006', // 6 Unused (ASCII ACK)
    '\u0007', // 7 Unused (ASCII BEL, audible bell)
    '\b', // 8 (ASCII BS, backspace)
    '\t', // 9 Horizontal tab (ASCII HT)
    '\n', // A Line feed (ASCII NL, newline)
    '\n', // B (ASCII VT, vertical tab - interpreted as LF. See Table 3-10 at https://vt100.net/docs/vt100-ug/chapter3.html)
    '\n', // C (ASCII NP a.k.k FF, new page - interpreted as LF)
    '\r', // D Carriage Return (ASCII CR)
    '\u000E', // E Unused (ASCII SO)
    '\u000F', // F Unused (ASCII SI)
    '\u0010', // 10 Unused (ASCII DLE)
    '\u0011', // 11 Unused (ASCII DC1)
    '\u0012', // 12 Unused (ASCII DC2)
    '\u0013', // 13 Unused (ASCII DC3)
    '\u0014', // 14 Unused (ASCII DC4)
    '\u0015', // 15 Unused (ASCII NAK)
    '\u0016', // 16 Unused (ASCII SYN)
    '\u0017', // 17 Unused (ASCII ETB)
    '\u0018', // 18 Unused (ASCII CAN)
    '\u0019', // 19 Unused (ASCII EM)
    '\u001A', // 1A Unused (ASCII SUB)
    '\u001B', // 1B Unused (ASCII ESC, escape)
    '\u001C', // 1C Unused (ASCII FS)
    '\u001D', // 1D Unused (ASCII GS)
    '\u001E', // 1E Unused (ASCII RS)
    '\u001F', // 1F Unused (ASCII US)
    ' ', // 20 Space (ASCII SP)
    '!', // Exclamation mark
    '"', // Quotation mark (&quot;)
    '#', // Number sign
    '$', // Dollar sign
    '%', // Percent sign
    '&', // Ampersand (&amp;)
    '\'', // Apostrophe (right single quote)
    '(', // Left parenthesis
    ')', // Right parenthesis
    '*', // Asterisk
    '+', // Plus sign
    ',', // Comma
    '-', // Hyphen
    '.', // Period (fullstop)
    '/', // Solidus (slash)
    '0', // Digit 0
    '1',
    '2',
    '3',
    '4',
    '5',
    '6',
    '7',
    '8',
    '9', // Digit 9
    ':', // Colon
    ';', // Semi-colon
    '<', // Less than (&lt;)
    '=', // Equals sign
    '>', // Greater than (&gt;)
    '?', // Question mark
    '@', // Commercial at-sign
    'A', // Uppercase letter A
    'B',
    'C',
    'D',
    'E',
    'F',
    'G',
    'H',
    'I',
    'J',
    'K',
    'L',
    'M',
    'N',
    'O',
    'P',
    'Q',
    'R',
    'S',
    'T',
    'U',
    'V',
    'W',
    'X',
    'Y',
    'Z', // Uppercase letter Z
    '[', // Left square bracket
    '\\', // Reverse solidus (backslash)
    ']', // Right square bracket
    '^', // Caret
    '_', // Horizontal bar (underscore)
    '`', // Reverse apostrophe (left single quote)
    'a', // Lowercase letter a
    'b',
    'c',
    'd',
    'e',
    'f',
    'g',
    'h',
    'i',
    'j',
    'k',
    'l',
    'm',
    'n',
    'o',
    'p',
    'q',
    'r',
    's',
    't',
    'u',
    'v',
    'w',
    'x',
    'y',
    'z', // Lowercase letter z
    '{', // Left curly brace
    '|', // Vertical bar
    '}', // Right curly brace
    '~', // Tilde
    '???', // Unused (ASCII DEL)
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Unused
    '???', // Non-breaking space (&nbsp;)
    '??', // Inverted exclamation
    '??', // Cent sign
    '??', // Pound sterling
    '??', // General currency sign
    '??', // Yen sign
    '??', // Broken vertical bar
    '??', // Section sign
    '??', // Umlaut (dieresis)
    '??', // Copyright
    '??', // Feminine ordinal
    '??', // Left angle quote, guillemotleft
    '??', // Not sign
    '??', // Soft hyphen
    '??', // Registered trademark
    '??', // Macron accent
    '??', // Degree sign
    '??', // Plus or minus
    '??', // Superscript two
    '??', // Superscript three
    '??', // Acute accent
    '??', // Micro sign
    '??', // Paragraph sign
    '??', // Middle dot
    '??', // Cedilla
    '??', // Superscript one
    '??', // Masculine ordinal
    '??', // Right angle quote, guillemotright
    '??', // Fraction one-fourth
    '??', // Fraction one-half
    '??', // Fraction three-fourths
    '??', // Inverted question mark
    '??', // Capital A, grave accent
    '??', // Capital A, acute accent
    '??', // Capital A, circumflex accent
    '??', // Capital A, tilde
    '??', // Capital A, dieresis or umlaut mark
    '??', // Capital A, ring
    '??', // Capital AE dipthong (ligature)
    '??', // Capital C, cedilla
    '??', // Capital E, grave accent
    '??', // Capital E, acute accent
    '??', // Capital E, circumflex accent
    '??', // Capital E, dieresis or umlaut mark
    '??', // Capital I, grave accent
    '??', // Capital I, acute accent
    '??', // Capital I, circumflex accent
    '??', // Capital I, dieresis or umlaut mark
    '??', // Capital Eth, Icelandic
    '??', // Capital N, tilde
    '??', // Capital O, grave accent
    '??', // Capital O, acute accent
    '??', // Capital O, circumflex accent
    '??', // Capital O, tilde
    '??', // Capital O, dieresis or umlaut mark
    '??', // Multiply sign
    '??', // Capital O, slash
    '??', // Capital U, grave accent
    '??', // Capital U, acute accent
    '??', // Capital U, circumflex accent
    '??', // Capital U, dieresis or umlaut mark
    '??', // Capital Y, acute accent
    '??', // Capital THORN, Icelandic
    '??', // Small sharp s, German (sz ligature)
    '??', // Small a, grave accent
    '??', // Small a, acute accent
    '??', // Small a, circumflex accent
    '??', // Small a, tilde
    '??', // Small a, dieresis or umlaut mark
    '??', // Small a, ring
    '??', // Small ae dipthong (ligature)
    '??', // Small c, cedilla
    '??', // Small e, grave accent
    '??', // Small e, acute accent
    '??', // Small e, circumflex accent
    '??', // Small e, dieresis or umlaut mark
    '??', // Small i, grave accent
    '??', // Small i, acute accent
    '??', // Small i, circumflex accent
    '??', // Small i, dieresis or umlaut mark
    '??', // Small eth, Icelandic
    '??', // Small n, tilde
    '??', // Small o, grave accent
    '??', // Small o, acute accent
    '??', // Small o, circumflex accent
    '??', // Small o, tilde
    '??', // Small o, dieresis or umlaut mark
    '??', // Division sign
    '??', // Small o, slash
    '??', // Small u, grave accent
    '??', // Small u, acute accent
    '??', // Small u, circumflex accent
    '??', // Small u, dieresis or umlaut mark
    '??', // Small y, acute accent
    '??', // Small thorn, Icelandic
    '??', // Small y, dieresis or umlaut mark
)