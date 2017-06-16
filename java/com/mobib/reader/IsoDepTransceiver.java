package com.mobib.reader;

import android.nfc.tech.IsoDep;

import java.io.IOException;

public class IsoDepTransceiver implements Runnable {


    public interface OnMessageReceived {

        void onMessage(byte[] message);

        void onError(Exception exception);

        void clearmessages();
    }

    private IsoDep isoDep;
    private OnMessageReceived onMessageReceived;
    private byte[] select_apdu;
    private byte[] readrecord_apdu;

    public IsoDepTransceiver(IsoDep isoDep, OnMessageReceived onMessageReceived) {
        this.isoDep = isoDep;
        this.onMessageReceived = onMessageReceived;
        this.select_apdu = new byte[] { (byte) 0x94, (byte)0xA4, 0x00, 0x00, (byte) 0x02, (byte)0x20, 0x69, 0x00 };
        this.readrecord_apdu = new byte[] { (byte) 0x94, (byte)0xB2, 0x01, 0x04, 0x00 };
    }

    @Override
    public void run() {

        boolean errorfound = false;
        onMessageReceived.clearmessages();
        try{
            isoDep.close();
            isoDep.connect();
        }
        catch(IOException e)
        {
            errorfound = true;
            onMessageReceived.onMessage("Unable to connect to card, please try again!".getBytes());
        }
        if(!errorfound) {
            try {
                byte[] response = new byte[0];
                isoDep.transceive(select_apdu);
                response = isoDep.transceive(readrecord_apdu);
                if (response != null) {
                    byte contractcounters;
                    for (int i = 0; (i*3)+3 < response.length -1; i++) {
                        contractcounters = (byte) (response[(i * 3)] + response[(i * 3) + 1] + response[(i * 3) + 2]);
                        if (contractcounters > 0) {
                            byte[] message = ("Ticket " + (i + 1) + ": ").getBytes();
                            byte[] ridesleftmsg = " rides left.".getBytes();
                            byte[] logmessage = new byte[message.length + ridesleftmsg.length + 2];
                            System.arraycopy(message, 0, logmessage, 0, message.length);
                            byte[] currentcounter = new byte[1];
                            currentcounter[0] = contractcounters;
                            byte[] currentcounterbytes = bytesToHex(currentcounter).getBytes();
                            logmessage[message.length] = currentcounterbytes[0];
                            logmessage[message.length + 1] = currentcounterbytes[1];
                            System.arraycopy(ridesleftmsg, 0, logmessage, message.length + 2, ridesleftmsg.length);
                            onMessageReceived.onMessage(logmessage);
                        }
                    }
                }
            } catch (Exception e) {
                onMessageReceived.onError(e);
            }
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
