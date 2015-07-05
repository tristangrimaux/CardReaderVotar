package com.example.android.cardreader;

/**
 * Created by tristan on 6/29/15.
 * Vot.ar formats
 */

import java.util.zip.CRC32;

public class VotarProtocol {
        // AID for our Ballot Card.
        private String ballot_id = "E004010000000000";

    public void setSystemInfo(byte[] info){
        ballot_id = BuildTagId(info, 2);
    }

    public String tagID(){
        return ballot_id;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Tarjeta VotAR\n");
        sb.append("Token: ");
        sb.append(token);
        sb.append("\n");
        sb.append("Card Type: ");
        sb.append(DataType.toString(cardtype));
        sb.append("\n");
        sb.append("Data Length: ");
        sb.append(datalength);
        sb.append("\n");
        sb.append("CRC: ");
        sb.append(CRC);
        sb.append(" validated CRC: ");
        sb.append(validateCRC());
        sb.append("\n");

        sb.append("Data: ");
        sb.append(ByteArrayToString(data));

        sb.append("\n");
        sb.append("Data length: ");
        sb.append(data.length);
        sb.append("\n");
        sb.append("Verificador: ");
        sb.append(ByteArrayToString(verif));
        sb.append("\n");
        sb.append("Blocks: ");
        sb.append(ByteArrayToHexString(blocks));
        sb.append("\n");

        return sb.toString();
    }

    public byte[] rawdata;

    private byte token;
    private DataType cardtype;

    private byte datalength;
    int CRC;
    private byte[] data;
    private byte[] verif;
    private byte[] blocks = new byte[BLOCKS];
    private byte[] mCRC = new byte[4];

    public String voteData;
    public void setBallotData(byte[] mdata) {

        // We cleanse mdata of a single offset and take away
        // the blocking bytes
        rawdata = new byte[BLOCK_SIZE * BLOCKS];
        int i;
        for (i = 0; i < (BLOCKS); i++) {
            // Blocking bytes
            System.arraycopy(mdata, OFFSET_CORRECTION + OFFSET_SEPARATOR + (i * (BLOCK_SIZE + OFFSET_SEPARATOR)), rawdata, (i * BLOCK_SIZE), BLOCK_SIZE); // copy data
            blocks[i] = mdata[OFFSET_CORRECTION + (i * (BLOCK_SIZE + OFFSET_SEPARATOR))];
        }

        token   = rawdata[OFFSET_TOKEN];
        byte[] xcardtype;
        xcardtype = new byte[2];
        System.arraycopy(rawdata, OFFSET_DATA_TYPE, xcardtype, 0, 2); // copy ID
        cardtype = DataType.fromValue(wordToInt(xcardtype));

        System.arraycopy(rawdata, OFFSET_CRC, mCRC, 0, BLOCK_SIZE); // copy ID
        CRC = byteArrayToIntBE(mCRC);
        datalength = rawdata[OFFSET_DATA_LENGTH];

        data     = new byte[BLOCK_SIZE * DATA_BLOCKS];
        System.arraycopy(rawdata, OFFSET_DATA, data, 0, BLOCK_SIZE * DATA_BLOCKS); // copy ID

       verif    = new byte[BLOCK_SIZE];
       System.arraycopy(rawdata, OFFSET_VERIFIER, verif, 0, BLOCK_SIZE); // copy ID

       voteData = ByteArrayToString(data);
    }

    public long validateCRC(){
        CRC32 mcrc32 = new CRC32();
        mcrc32.update(data, 0, datalength);
        return mcrc32.getValue();
    }


       // Format: [Class | Instruction | Parameter 1 | Parameter 2]
//        private static final byte[] SELECT_OK_SW = {(byte) 0x90, (byte) 0x00};


        /** Number of blocks within the RFID tag. */
        public static final int BLOCKS = 28;

        /** Number of available blocks to write data. */
        public static final int DATA_BLOCKS = 25;

        /** Size of each block within the RFID tag. */
        public static final int BLOCK_SIZE = 4;

        /** Id used as protocol identifier. */
//        public static final byte TOKEN = 0x1C;

        /** Verification identifier. */
//        public static final byte[] VERIFIER = "W_OK".getBytes();


// offsets
        public static final int OFFSET_CORRECTION = 0x1;
        public static final int OFFSET_SEPARATOR = 0x1;

    /** Offset of the protocol identifier. */
        public static final int OFFSET_TOKEN = 0x0;

        /** Offset of the custom data type identifier. */
        public static final int OFFSET_DATA_TYPE = 0x1;

        /** Offset of the custom data length. */
        public static final int OFFSET_DATA_LENGTH = 0x3;

        /** Offset of the custom data CRC. */
        public static final int OFFSET_CRC = 0x4;

        /** Position where custom data starts. */
        public static final int OFFSET_DATA = 0x8;

        /** Offset of the verification identifier from DATA. */
        public static final int OFFSET_VERIFIER = BLOCK_SIZE * (BLOCKS - 1);


    /**
     * BuildTagId generates a tag from data
     *
     * @param info    Byte array block with data
     * @param Offset  Correction offset to begin with
     * @return String
     */

    public static String BuildTagId(byte[] info, int Offset) {
        //00 0f -> número al revés ->  00 00 1b 03 01
        // e0:04:01:00:23:9d:b5:dc

        byte[] rsp = new byte[8];
        // para el voto siempre empieza con E0:04:01
        rsp[0] =  info[Offset + 7]; // E0 iCODE
        rsp[1] =  info[Offset + 6]; // 04 Philips
        rsp[2] =  info[Offset + 5]; // 01 SL2 ICS20
        rsp[3] =  info[Offset + 4]; // IC manufacturerer serial number
        rsp[4] =  info[Offset + 3];
        rsp[5] =  info[Offset + 2];
        rsp[6] =  info[Offset + 1];
        rsp[7] =  info[Offset    ];

        return ByteArrayToHexString(rsp);
    }


        /** Supported data types */
        public enum DataType {
            /** The data represents a vote. */
            VOTE(0x1),
            /** The data represents an MSA user information. */
            MSA_USER(0x2),
            /** The data represents user information related to the president
             * of a single location. */
            PRESIDENT(0x3),
            /** The data has re-counting information. */
            RECOUNTING(0x4),
            /** The data has opening information. */
            OPENING(0x5),
            /** The data is related to a DEMO version of VOT.AR. */
            DEMO(0x6);

            final int value;

            DataType(int value) {
                this.value = value;
            }

            public static DataType fromValue(int value) {
                for (DataType dataType : values()) {
                    if (dataType.value == value) {
                        return dataType;
                    }
                }
                throw new RuntimeException("Invalid data type value: " + value);
            }

            public static String toString(int value) {
               return DataType.toString(DataType.fromValue(value));
            }
            public static String toString(DataType value) {
                String rsp;
                switch(value) {
                    case VOTE:
                       rsp = "VOTO";
                       break;
                    case MSA_USER:
                        rsp = "MSA_USER";
                        break;
                    case PRESIDENT:
                        rsp = "Presidente de mesa";
                        break;
                    case RECOUNTING:
                        rsp = "Reconteo";
                        break;
                    case OPENING:
                        rsp = "Apertura";
                        break;
                    case DEMO:
                        rsp = "DEMO";
                        break;
                    default:
                        rsp = "Desconocido";
                }
                return rsp;
            }
        }

    /**
     * Utility class to convert a byte array to a hexadecimal string.
     *
     * @param bytes Bytes to convert
     * @return String, containing hexadecimal representation.
     */
    public static String ByteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String ByteArrayToString(byte[] bytes) {
        //final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] result = new char[bytes.length];
        for ( int j = 0; j < bytes.length; j++ ) {
            result[j] = (char)bytes[j];
        }
        return new String(result);
    }

    /**
     * Utility class to convert a hexadecimal string to a byte string.
     *
     * <p>Behavior with input strings containing non-hexadecimal characters is undefined.
     *
     * @param s String containing hexadecimal characters to convert
     * @return Byte array generated from input
     */
    public static byte[] HexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static int wordToInt(byte[] b)
    {
        return  (b[1] * 0xFF) + (b[0]) ;
    }

    public static int byteArrayToInt(byte[] b)
    {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static int byteArrayToIntBE(byte[] b)
    {
        return   b[0] & 0xFF |
                (b[1] & 0xFF) << 8 |
                (b[2] & 0xFF) << 16 |
                (b[3] & 0xFF) << 24;
    }


}
