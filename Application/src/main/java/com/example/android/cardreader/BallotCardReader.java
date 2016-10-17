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

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;

import com.example.android.common.logger.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Callback class, invoked when an NFC card is scanned while the device is running in reader mode.
 *
 * Reader mode can be invoked by calling NfcAdapter
 */

public class BallotCardReader implements NfcAdapter.ReaderCallback {

    private static final byte cmdReadMultipleBlocks = 0x23;
    private static final byte cmdWriteMultipleBlocks = 0x24;
    private static final byte cmdInfo = 0x2B;

    private static final String TAG = "BallotCardReader";

    // Weak reference to prevent retain loop. mBallotCallback is responsible for exiting
    // foreground mode before it becomes invalid (e.g. during onPause() or onStop()).
    private WeakReference<BallotCallback> mBallotCallback;

    interface BallotCallback {
        void onBallotReceived(VotarProtocol mVotarProtocol);
        void onBallotFailed(String error);
    }

    public BallotCardReader(BallotCallback ballotCallback) {
        mBallotCallback = new WeakReference<BallotCallback>(ballotCallback);
    }

    public void WriteTag(Tag tag, byte[] dataBlock) {
        try {
            NfcV nfcvTag = NfcV.get(tag);
            if (nfcvTag != null) {
                nfcvTag.connect();
                byte[] xdata = nfcvTag.transceive(getCommand(tag.getId(), cmdWriteMultipleBlocks, dataBlock));
            //que hago con xdata? verifico algo??? calculo que si, no?
            }
        } catch (IOException e) {
            Log.e(TAG, "Error communicating with card: " + e.toString());
            mBallotCallback.get().onBallotFailed(e.toString());
        }
    }
    /**
     * Callback when a new tag is discovered by the system.
     *
     * <p>Communication with the card should take place here.
     *
     * @param tag Discovered tag
     */
    @Override
    public void onTagDiscovered(Tag tag) {
        Log.i(TAG, "New tag discovered");
        Log.i(TAG, "Tag: " + VotarProtocol.BuildTagId(tag.getId(),0));
        VotarProtocol mVotarProtocol;
        Log.i(TAG, "antes del get");
        NfcV nfcvTag = NfcV.get(tag);
        Log.i(TAG, "despue del get");

        if (nfcvTag != null) {
            mVotarProtocol = new VotarProtocol();
            try {
                // Connect to the remote NFC device
                // set up read command buffer
                nfcvTag.connect();

                mVotarProtocol.setSystemInfo(nfcvTag.transceive(getCommand(tag.getId(), true)));

                Log.i(TAG, "-----------------------");
                Log.i(TAG, "Tag ID: " + mVotarProtocol.tagID());
                Log.i(TAG, "-----------------------");
                Log.i(TAG, "antes de transceive");
                byte[] xdata = nfcvTag.transceive(getCommand(tag.getId(), false));
                Log.i(TAG, "despue del transceive");
                mVotarProtocol.setBallotData(xdata);

                //Log.i(TAG, VotarProtocol.ByteArrayToHexString(mVotarProtocol.rawdata));
                //Log.i(TAG, "-----------------------");

                Log.i(TAG, mVotarProtocol.toString());
                Log.i(TAG, "------xxxx--------------");

                //aca quiero que mande la cosa entera
                mBallotCallback.get().onBallotReceived(mVotarProtocol);


            } catch (IOException e) {
                Log.e(TAG, "Error communicating with card: " + e.toString());
                mBallotCallback.get().onBallotFailed(e.toString());
            }
        }

    }

    public byte[] getCommand(byte[] TagId, boolean getInfo) {
        if (getInfo) {
            return getCommand(TagId, cmdInfo, null);
        } else {
            return getCommand(TagId, cmdReadMultipleBlocks, null);
        }
    }

    public byte[] getCommand(byte[] TagId, byte command, byte[] dataBlock) {
        int offset = 0;  // offset of first block to read
        int blocks = VotarProtocol.BLOCKS;  // number of blocks to read
        byte[] cmd = new byte[]{0x00, (byte) 0x00};

        //cmdWriteMultipleBlocks
        switch (command) {
            case cmdInfo:
                cmd = new byte[]{0x00, (byte) 0x2B
//                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // placeholder for tag UID
                };
            case cmdReadMultipleBlocks: {
                cmd = new byte[]{
                        (byte) 0x60,                  // flags: addressed (= UID field present)
                        (byte) 0x23,                  // command: READ MULTIPLE BLOCKS
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // placeholder for tag UID
                        (byte) (offset & 0x0ff),      // first block number
                        (byte) ((blocks - 1) & 0x0ff) // number of blocks (-1 as 0x00 means one block)
                };
                System.arraycopy(TagId, 0, cmd, 2, TagId.length); // copy ID
            }
            case cmdWriteMultipleBlocks: {
                //tengo que poner los bytes de blocking en 0 para que no queden bloqueados...
                cmd = new byte[]{
                        (byte) 0x60,                  // flags: addressed (= UID field present)
                        (byte) 0x24,                  // command: READ MULTIPLE BLOCKS sera 24?
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,  // placeholder for tag UID
                        (byte) (offset & 0x0ff),      // first block number
                        (byte) ((blocks - 1) & 0x0ff) // number of blocks (-1 as 0x00 means one block)
                };
                System.arraycopy(TagId, 0, cmd, 2, TagId.length); // copy ID
                System.arraycopy(dataBlock, 0, cmd, offset & 0x0ff, dataBlock.length); // copy data
            }
        }
        return cmd;
    }
}
