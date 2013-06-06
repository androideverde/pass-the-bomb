package com.androideverde.passthebomb;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Parcelable;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.nio.charset.Charset;

public class MainActivity extends Activity {

    private TextView txtMessage;
    private TextView txtTimer;
    private Button btnStartGame;
    private Button btnPassBomb;
    private boolean isStarted = false;
    private final static long FULL_COUNTDOWN_TIME = 59000; // 59 seconds
    private final static long TIMER_UPDATE_INTERVAL = 1000; // 1 second
    private NfcAdapter nfcAdapter;
    private boolean nfcOK = false;
    private CountDownTimer timer;
    private long currentMillis;
    private long startMillis = FULL_COUNTDOWN_TIME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        while (!nfcOK) runPreStartChecks();
        setViewsAndListeners();
        setNfcCallback(); //init here instead of in btnPassBomb.onClick.
    }

    private void setViewsAndListeners() {
        txtMessage = (TextView) findViewById(R.id.txtMessage);
        txtTimer = (TextView) findViewById(R.id.txtTimer);
        btnStartGame = (Button) findViewById(R.id.btnStartGame);
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGame(startMillis);
            }
        });
        btnPassBomb = (Button) findViewById(R.id.btnPassBomb);
        btnPassBomb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                setNfcCallback();
            }
        });
    }

    private void startGame(long time) {
        if (!isStarted) {
            //start a new game with timer set to the given time
            isStarted = true;
            setTimerText(time);
            currentMillis = time;
            setMessageText(R.string.msgBomb);
            //start timer countdown
            timer = new CountDownTimer(time, TIMER_UPDATE_INTERVAL) {
                @Override
                public void onTick(long l) {
                    setTimerText(l);
                    currentMillis = l;
                }
                @Override
                public void onFinish() {
                    setTimerText(0);
                    currentMillis = 0;
                    setMessageText(R.string.msgBoom);
                    isStarted = false;
                }
            };
            timer.start();
        }
    }

    private void setMessageText(int msg) {
        txtMessage.setText(msg);
    }

    private void setTimerText(long millis) {
        int secs = (int) millis/1000;
        txtTimer.setText(String.valueOf(secs));
    }

    private void runPreStartChecks() {
        //nfc enabled? beam enabled?
        //if not, launch system settings so user can enable
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (!nfcAdapter.isEnabled()) {
            nfcOK = false;
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        } else if (!nfcAdapter.isNdefPushEnabled()) {
            nfcOK = false;
            startActivity(new Intent(Settings.ACTION_NFCSHARING_SETTINGS));
        } else {
            nfcOK = true;
        }
    }

    protected void setNfcCallback() {
        //set callback to use to create a message
        nfcAdapter.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
            public NdefMessage createNdefMessage(NfcEvent event) {
                NdefMessage message = createMessage();
                return message;
            }
        }, this);
        //set callback to use on beam completed
        nfcAdapter.setOnNdefPushCompleteCallback(new NfcAdapter.OnNdefPushCompleteCallback() {
            @Override
            public void onNdefPushComplete(NfcEvent nfcEvent) {
            bombPassed();
            }
        }, this);
    }

    private void bombPassed() {
        isStarted = false;
        timer.cancel();
        setMessageText(R.string.msgBombPassed);
    }

    /**
     * Creates a new NFC message. The NFC message payload is just the millis remaining in the countdown.
     * @return created NFC message.
     */
    private NdefMessage createMessage() {
        String mimeType = "application/com.androideverde.passthebomb";
        byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
        String payload = String.valueOf(currentMillis);
        NdefMessage nfcMessage = new NdefMessage(new NdefRecord[] {
            new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload.getBytes()),
                NdefRecord.createApplicationRecord("com.androideverde.passthebomb")
        });
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent, stores it into currentMillis and starts timer
     */
    void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        currentMillis = Long.parseLong(new String(msg.getRecords()[0].getPayload()));
        startGame(currentMillis);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // TODO: only for debug!
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_30:
                startMillis = 29000;
                return true;
            case R.id.action_60:
                startMillis = 59000;
                return true;
            case R.id.action_cancel:
                timer.cancel();
                setTimerText(0);
                currentMillis = 0;
                setMessageText(R.string.msgCancelled);
                isStarted = false;
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
