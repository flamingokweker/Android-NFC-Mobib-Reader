package com.mobib.reader;

import java.io.IOException;

import android.nfc.tech.IsoDep;
import android.util.Log;

public class IsoDepTransceiver implements Runnable {


    public interface OnMessageReceived {

        void onMessage(byte[] message);

        void onError(Exception exception);
    }

    private IsoDep isoDep;
    private OnMessageReceived onMessageReceived;

    public IsoDepTransceiver(IsoDep isoDep, OnMessageReceived onMessageReceived) {
        this.isoDep = isoDep;
        this.onMessageReceived = onMessageReceived;
    }

    private static final byte[] CLA_INS_P1_P2 = { (byte) 0x94, (byte)0xA4, 0x00, 0x00 };
    private static final byte[] AID_ANDROID = { (byte)0x20, 0x69};

    private byte[] createSelectAidApdu(byte[] aid) {
        byte[] result = new byte[6 + aid.length];
        System.arraycopy(CLA_INS_P1_P2, 0, result, 0, CLA_INS_P1_P2.length);
        result[4] = (byte)aid.length;
        System.arraycopy(aid, 0, result, 5, aid.length);
        result[result.length - 1] = 0;
        return result;
    }

    @Override
    public void run() {
        int messageCounter = 0;
        byte[] response = new byte[0];
        try {
            isoDep.connect();
            response = isoDep.transceive(createSelectAidApdu(AID_ANDROID));
            //onMessageReceived.onMessage(response);
            byte[] record = { (byte) 0x94, (byte)0xB2, 0x01, 0x04, 0x1D };
            response = isoDep.transceive(record);
          }
        catch (IOException e) {
            //onMessageReceived.onError(e);
        }
        finally {
            byte[] contractcounters = new byte[16];
            for(int i = 0;i<5; i++){
                contractcounters[i] = (byte) (response[(i*3)+1] + response[(i*3)+2] + response[(i*3)+3]);
            }
            int i = 0;
              while (contractcounters[i] > 0){
                byte[] message = ("Ticket " + (i+1) + ": ").getBytes();
                 byte[] logmessage = new byte[message.length + 2];
                 System.arraycopy(message, 0, logmessage, 0, message.length);
                  byte[] currentcounter = new byte[1];
                  currentcounter[0] = contractcounters[i];
                  byte[]  currentcounterbytes = bytesToHex(currentcounter).getBytes();
                 logmessage[logmessage.length-2] = currentcounterbytes[0];
                  logmessage[logmessage.length-1] = currentcounterbytes[1];
                onMessageReceived.onMessage(logmessage);
            i++;
              }
          //  isoDep.close();
        }
    }
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
