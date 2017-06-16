package com.mobib.reader;


import android.app.Activity;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.ReaderCallback;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;

import com.mobib.reader.IsoDepTransceiver.OnMessageReceived;

public class MainActivity extends Activity implements OnMessageReceived, ReaderCallback {

    private NfcAdapter nfcAdapter;
    private ListView listView;
    private IsoDepAdapter isoDepAdapter;
    private IsoDepTransceiver isoDepTransceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.listView);
        isoDepAdapter = new IsoDepAdapter(getLayoutInflater());
        listView.setAdapter(isoDepAdapter);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        TabHost host = (TabHost)findViewById(R.id.tabHost);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("Scan");
        spec.setContent(R.id.linearLayoutScan);
        spec.setIndicator("Scan");
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("Help");
        spec.setContent(R.id.linearLayoutHelp);
        spec.setIndicator("Help");
        host.addTab(spec);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (nfcAdapter == null) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            try {
                wait(500);
            }
            catch(Exception ex)
            {

            }
        }
        nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK | NfcAdapter.FLAG_READER_NFC_B,
                null);
    }

    @Override
    public void onPause() {
        super.onPause();
        nfcAdapter.disableReaderMode(this);
        nfcAdapter = null;
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        IsoDep isoDep = IsoDep.get(tag);
        this.isoDepTransceiver = new IsoDepTransceiver(isoDep, this);
        Thread thread = new Thread(this.isoDepTransceiver);
        thread.start();

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(100);
    }

    @Override
    public void onMessage(final byte[] message) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                //isoDepAdapter.addMessage(bytesToHex(message));
                isoDepAdapter.addMessage(new String(message).replace("0A","10"));
            }
        });
    }

    @Override
    public void clearmessages() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                //isoDepAdapter.addMessage(bytesToHex(message));
                isoDepAdapter.clearMessages();
            }
        });
    }


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public void onError(Exception exception) {
        String errormsg = exception.getMessage();
        if (errormsg != null) {
            onMessage(errormsg.getBytes());
        } else {
            onMessage("Communication error, please try again".getBytes());
        }

        this.isoDepTransceiver = null;
        nfcAdapter.disableReaderMode(this);
        nfcAdapter = null;
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK | NfcAdapter.FLAG_READER_NFC_B,
                null);
    }
}
