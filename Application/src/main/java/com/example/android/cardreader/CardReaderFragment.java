/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.cardreader;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.common.logger.Log;


/**
 * Generic UI for sample discovery.
 */
public class CardReaderFragment extends Fragment implements BallotCardReader.BallotCallback {

    public VotarProtocol VotoMaestro;

    public static final String TAG = "CardReaderFragment";
    // Recommend NfcAdapter flags for reading from other Android devices. Indicates that this
    // activity is interested in NFC-A devices (including other Android devices), and that the
    // system should not check for the presence of NDEF-formatted data (e.g. Android Beam).
    public static int READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_NFC_BARCODE | NfcAdapter.FLAG_READER_NFC_F| NfcAdapter.FLAG_READER_NFC_V | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;

    private View mView;
    public BallotCardReader mBallotCardReader;
    private TextView mAccountField;
    private TextView mTitleField;
    private Button mButton;

    /** Called when sample is created. Displays generic UI with welcome text. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.main_fragment, container, false);
        if (mView != null) {
            mAccountField = (TextView) mView.findViewById(R.id.card_account_field);
            mAccountField.setText("Esperando voto...");

            mTitleField = (TextView) mView.findViewById(R.id.card_title);
            mTitleField.setText("");

            mButton = (Button) mView.findViewById(R.id.btOk);
            //mButton.hasOnClickListeners() setText("Sin Maestro");
            mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MasterMode(false);
                }
            });
            mBallotCardReader = new BallotCardReader(this);

            // Disable Android Beam and register our card reader callback
            enableReaderMode();
        }

        return mView;
    }

    @Override
    public void onPause() {
        super.onPause();
        disableReaderMode();
    }

    @Override
    public void onResume() {
        super.onResume();
        enableReaderMode();
    }

    private void enableReaderMode() {
        Log.i(TAG, "Enabling reader mode");
        Activity activity = getActivity();
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(activity);
        if (nfc != null) {
            nfc.enableReaderMode(activity, mBallotCardReader, READER_FLAGS, null);
        }
    }

    private void disableReaderMode() {
        Log.i(TAG, "Disabling reader mode");
        Activity activity = getActivity();
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(activity);
        if (nfc != null) {
            nfc.disableReaderMode(activity);
        }
    }

    private void makeNotification(Boolean justVibrate, Boolean isError){
        try {
            long[] pattern = {0, 200, 100,200, 100,200, 100};
            Vibrator v = (Vibrator) getActivity().getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            if (isError) {
              v.vibrate(pattern, -1);
            } else {
              v.vibrate(500);
            }

        } catch (Exception e) {
            Log.i(TAG, "Error: " + e.toString());
        }

        if (!justVibrate) {
            try {
                Ringtone r = RingtoneManager.getRingtone(getActivity().getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
    private void MasterMode(final boolean playSound) {
        if (VotoMaestro != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTitleField.setText("");
                    mView.setBackgroundColor(Color.YELLOW);
//                    mAccountField.setText("Maestro " + VotoMaestro.tagID());
                    mAccountField.setText("VOTO REGISTRADO");
                    mButton.setVisibility(View.INVISIBLE);
                    if (playSound) {
                        makeNotification(true, false);
                    }

                }
            });

        }

    }

    private void RetryMode() {
        if (VotoMaestro != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTitleField.setText("El voto no pudo ser leído");
                    mView.setBackgroundColor(Color.CYAN);
                    mAccountField.setText("Reintente");
                    makeNotification(true, true);
                }
            });

        }

    }


    private void GoodBallotMode() {
        if (VotoMaestro != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTitleField.setText("");
                    mView.setBackgroundColor(Color.GREEN);
                    mAccountField.setText("IGUAL");
                    mButton.setVisibility(View.VISIBLE);
                    makeNotification(true, false);
                }
            });

        }

    }


    private void BadBallotMode() {
        if (VotoMaestro != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTitleField.setText("");
                    mView.setBackgroundColor(Color.RED);
                    mAccountField.setText("DISTINTO");
                    mButton.setVisibility(View.VISIBLE);
                    makeNotification(true, false);
                }
            });

        }

    }



    @Override
    public void onBallotFailed(final String Error) {
        RetryMode();
    }
    @Override
    public void onBallotReceived(final VotarProtocol mVotarProtocol) {
        // This callback is run on a background thread, but updates to UI elements must be performed
        // on the UI thread.
        if ( VotoMaestro == null) {
            VotoMaestro = mVotarProtocol;
            MasterMode(true);
        } else {
            if (mVotarProtocol.voteData.equals(VotoMaestro.voteData)  ) {
              GoodBallotMode();
            } else {
               BadBallotMode();
            }

        }
    }
}
