package com.mobib.reader;


import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.ReaderCallback;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.widget.ListView;
import com.mobib.reader.IsoDepTransceiver.OnMessageReceived;

public class MainActivity extends Activity implements OnMessageReceived, ReaderCallback {

    private NfcAdapter nfcAdapter;
    private ListView listView;
    private IsoDepAdapter isoDepAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView)findViewById(R.id.listView);
        isoDepAdapter = new IsoDepAdapter(getLayoutInflater());
        listView.setAdapter(isoDepAdapter);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK | NfcAdapter.FLAG_READER_NFC_B,
                null);
    }

    @Override
    public void onPause() {
        super.onPause();
        nfcAdapter.disableReaderMode(this);
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        IsoDep isoDep = IsoDep.get(tag);
        IsoDepTransceiver transceiver = new IsoDepTransceiver(isoDep, this);
        Thread thread = new Thread(transceiver);
        thread.start();
    }

    @Override
    public void onMessage(final byte[] message) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                //isoDepAdapter.addMessage(bytesToHex(message));
                isoDepAdapter.addMessage(new String(message));
            }
        });
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

    @Override
    public void onError(Exception exception) {
        String errormsg = exception.getMessage();
        if(errormsg != null) {
            onMessage(errormsg.getBytes());
        }
    }
}
