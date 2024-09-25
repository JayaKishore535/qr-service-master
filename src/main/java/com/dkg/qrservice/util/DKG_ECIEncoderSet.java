package com.dkg.qrservice.util;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

public final class DKG_ECIEncoderSet {

  // List of encoders that potentially encode characters not in ISO-8859-1 in one byte.
  private static final List<CharsetEncoder> ENCODERS = new ArrayList<>();
  static {
    String[] names = { "IBM437",
                       "ISO-8859-2",
                       "ISO-8859-3",
                       "ISO-8859-4",
                       "ISO-8859-5",
                       "ISO-8859-6",
                       "ISO-8859-7",
                       "ISO-8859-8",
                       "ISO-8859-9",
                       "ISO-8859-10",
                       "ISO-8859-11",
                       "ISO-8859-13",
                       "ISO-8859-14",
                       "ISO-8859-15",
                       "ISO-8859-16",
                       "windows-1250",
                       "windows-1251",
                       "windows-1252",
                       "windows-1256",
                       "Shift_JIS" };
    for (String name : names) {
      if (DKG_CharacterSetECI.getCharacterSetECIByName(name) != null) {
        try {
          ENCODERS.add(Charset.forName(name).newEncoder());
        } catch (UnsupportedCharsetException e) {
          // continue
        }
      }
    }
  }

  private final CharsetEncoder[] encoders;
  private final int priorityEncoderIndex;


  public DKG_ECIEncoderSet(String stringToEncode, Charset priorityCharset, int fnc1) {
    List<CharsetEncoder> neededEncoders = new ArrayList<>();

    neededEncoders.add(StandardCharsets.ISO_8859_1.newEncoder());
    boolean needUnicodeEncoder = priorityCharset != null && priorityCharset.name().startsWith("UTF");

    //Walk over the input string and see if all characters can be encoded with the list of encoders 
    for (int i = 0; i < stringToEncode.length(); i++) {
      boolean canEncode = false;
      for (CharsetEncoder encoder : neededEncoders) {
        char c = stringToEncode.charAt(i);
        if (c == fnc1 || encoder.canEncode(c)) {
          canEncode = true;
          break;
        }
      }

      if (!canEncode) {
        //for the character at position i we don't yet have an encoder in the list
        for (CharsetEncoder encoder : ENCODERS) {
          if (encoder.canEncode(stringToEncode.charAt(i))) {
            //Good, we found an encoder that can encode the character. We add him to the list and continue scanning
            //the input
            neededEncoders.add(encoder);
            canEncode = true;
            break;
          }
        }
      }

      if (!canEncode) {
        //The character is not encodeable by any of the single byte encoders so we remember that we will need a
        //Unicode encoder.
        needUnicodeEncoder = true;
      }
    }
  
    if (neededEncoders.size() == 1 && !needUnicodeEncoder) {
      //the entire input can be encoded by the ISO-8859-1 encoder
      encoders = new CharsetEncoder[] { neededEncoders.get(0) };
    } else {
      // we need more than one single byte encoder or we need a Unicode encoder.
      // In this case we append a UTF-8 and UTF-16 encoder to the list
      encoders = new CharsetEncoder[neededEncoders.size() + 2];
      int index = 0;
      for (CharsetEncoder encoder : neededEncoders) {
        encoders[index++] = encoder;
      }

      encoders[index] = StandardCharsets.UTF_8.newEncoder();
      encoders[index + 1] = StandardCharsets.UTF_16BE.newEncoder();
    }
  
    //Compute priorityEncoderIndex by looking up priorityCharset in encoders
    int priorityEncoderIndexValue = -1;
    if (priorityCharset != null) {
      for (int i = 0; i < encoders.length; i++) {
        if (encoders[i] != null && priorityCharset.name().equals(encoders[i].charset().name())) {
          priorityEncoderIndexValue = i;
          break;
        }
      }
    }
    priorityEncoderIndex = priorityEncoderIndexValue;
    //invariants
    assert encoders[0].charset().equals(StandardCharsets.ISO_8859_1);
  }

  public int length() {
    return encoders.length;
  }

  public String getCharsetName(int index) {
    assert index < length();
    return encoders[index].charset().name();
  }

  public Charset getCharset(int index) {
    assert index < length();
    return encoders[index].charset();
  }

  public int getECIValue(int encoderIndex) {
    return DKG_CharacterSetECI.getCharacterSetECI(encoders[encoderIndex].charset()).getValue();
  }

  /*
   *  returns -1 if no priority charset was defined
   */
  public int getPriorityEncoderIndex() {
    return priorityEncoderIndex;
  }

  public boolean canEncode(char c, int encoderIndex) {
    assert encoderIndex < length();
    CharsetEncoder encoder = encoders[encoderIndex];
    return encoder.canEncode("" + c);
  }

  public byte[] encode(char c, int encoderIndex) {
    assert encoderIndex < length();
    CharsetEncoder encoder = encoders[encoderIndex];
    assert encoder.canEncode("" + c);
    return ("" + c).getBytes(encoder.charset());
  }

  public byte[] encode(String s, int encoderIndex) {
    assert encoderIndex < length();
    CharsetEncoder encoder = encoders[encoderIndex];
    return s.getBytes(encoder.charset());
  }
}
