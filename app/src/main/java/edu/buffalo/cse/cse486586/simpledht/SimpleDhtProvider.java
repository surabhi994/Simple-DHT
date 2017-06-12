package edu.buffalo.cse.cse486586.simpledht;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    String myPort = "";
    ArrayList<String> key_arr = new ArrayList<String>();
    ArrayList<String> value_arr = new ArrayList<String>();

    String node_id = "";
    String msg_rec[] = new String[5];
    HashMap<String, String> hm = new HashMap<String, String>();
    private ContentResolver mContentResolver;

    Uri muri;
    static final String TAG = SimpleDhtProvider.class.getSimpleName();


    static final int SERVER_PORT = 10000;
    String REMOTE_PORT = "11108";
    String transfer_msg;
    public static String successor;
    public static String pred;


    ArrayList<String> node_list = new ArrayList<String>();

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        String hash_key = "";

        SharedPreferences sharedPref = getContext().getSharedPreferences("Your Pref", Context.MODE_PRIVATE);
        try {
            hash_key = genHash(selection);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.d("selection", selection);
        Log.d("selection", hash_key);

        if ((pred.equals("null") && successor.equals("null")) || (node_list.size() == 1)) {// condition for checking if single node in system
            if (selection.equals("*") || selection.equals("@")) {

                sharedPref.edit().clear().commit();
            } else {

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.remove(selection);
                editor.commit();

                Log.d("delete", "key");

            }
            return 0;
        } else { // if more than 1 node in the system
            if (selection.equals("@")) // to delete all values in the avd
            {
                sharedPref.edit().clear().commit();
                return 0;

            } else { // delete a specific value in the avd


                Log.d("Deleting value", "delete" + selection);
                String hk = "";
                try {
                    hk = genHash(selection);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                int compare1 = pred.compareTo(hk);
                int compare2 = hk.compareTo(node_id);
                int compare3 = node_id.compareTo(pred);
                int compare4 = node_id.compareTo(successor);

                if (compare1 < 0 && compare2 <= 0) // condition to check if the key lies in the region of this avd
                {

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.remove(selection);
                    editor.commit();

                    Log.d("delete", "key");
                    return 0;


                } else if ((compare3 < 0 && compare4 < 0) && (compare1 < 0 || compare2 < 0)) // condition to check if key lies in the node with the smallest hash value
                {


                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.remove(selection);
                    editor.commit();

                    Log.d("delete", "key");
                    return 0;

                } else { // forwarding request to the node where the key lies
                    String rec = "";
                    String originating = myPort;


                    String port = hm.get(successor);

                    transfer_msg = "transferDelQuery" + ";" + port + ";" + successor + ";" + selection + ";" + originating;


                    Log.d("transfer del at query", transfer_msg);

                    try {
                        rec = new ClientTask3().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, transfer_msg, myPort).get();//port
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }


                    return 0;

                }

            }

        }
    }


    @Override
    public String getType(Uri uri) {

        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {


        String k = values.getAsString("key");
        String hash_key = "";


        String value = values.getAsString("value");


        try {
            hash_key = genHash(k);
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }


        Log.d("initial insert", k + hash_key);


        if ((pred.equals("null") && successor.equals("null")) || (node_list.size() == 1)) { // checking if onlyone node is present
            try {
                SharedPreferences sharedPref = getContext().getSharedPreferences("Your Pref", Context.MODE_PRIVATE);
                SharedPreferences.Editor ed = sharedPref.edit();
                ed.putString(k, value);
                ed.commit();
                Log.d("inserting", hash_key);
            } catch (Exception e) {
                Log.d("insert", "fail");
            }


        } else { // more than 1 node present in the system


            int compare1 = pred.compareTo(hash_key);
            Log.d("current_node", node_id);
            Log.d("pred of curr", pred);
            Log.d("succ of curr", successor);

            int compare2 = hash_key.compareTo(node_id);
            int compare3 = node_id.compareTo(pred);
            int compare4 = node_id.compareTo(successor);

            Log.d("ComparesValueInsert", Integer.toString(compare1) + ";" + Integer.toString(compare2));
            Log.d("ComparesValueInsert2", k + hash_key);

            if (compare1 < 0 && compare2 <= 0) { //if the current avd is where the key should be inserted

                try {

                    SharedPreferences sharedPref = getContext().getSharedPreferences("Your Pref", Context.MODE_PRIVATE);
                    SharedPreferences.Editor ed = sharedPref.edit();
                    ed.putString(k, value);
                    ed.commit();
                    Log.d("insertingKey", k);
                    Log.d("insertingHashedKey", hash_key);
                } catch (Exception e) {
                    Log.d("insert", "fail");
                }

            } else if ((compare3 < 0 && compare4 < 0) && (compare1 < 0 || compare2 < 0)) { // if the current avd is where the key should be inserted and is the avd with smallest hash value


                try {
                    SharedPreferences sharedPref = getContext().getSharedPreferences("Your Pref", Context.MODE_PRIVATE);
                    SharedPreferences.Editor ed = sharedPref.edit();
                    ed.putString(k, value);
                    ed.commit();

                    Log.d("insertingKey", k);
                    Log.d("insertingHashedKey", hash_key);
                    Log.d("inserting late", genHash(k));

                } catch (Exception e) {
                    Log.d("insert", "fail");
                }

            } else {//forwarding request to the avd where the key should be inserted
                String port = hm.get(successor);
                transfer_msg = "transfer" + ";" + port + ";" + successor + ";" + k + ";" + value;


                Log.d("transfer at insert", transfer_msg);

                new ClientTask3().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, transfer_msg, port);
            }


        }
        return uri;
    }

    @Override
    public boolean onCreate() {


        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);

        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        mContentResolver = getContext().getContentResolver();
        if (myPort.equals("11108")) {
            pred = "null";
            successor = "null";
            node_list.add("33d6357cfaaf0f72991b0ecd8c56da066613c089");
        }
        hm.put("33d6357cfaaf0f72991b0ecd8c56da066613c089", "11108");
        hm.put("208f7f72b198dadd244e61801abe1ec3a4857bc9", "11112");
        hm.put("abf0fd8db03e5ecb199a9b82929e9db79b909643", "11116");
        hm.put("c25ddd596aa7c81fa12378fa725f706d54325d12", "11120");
        hm.put("177ccecaec32c54b82d5aaafc18a2dadb753e3b1", "11124");
        try {
            node_id = genHash(portStr);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.d("my node id", node_id);
        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");

        }

        String msg = "new" + ";" + node_id + ";" + myPort + ";" + "" + ";" + "";

        if (!myPort.equals("11108"))
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

        return false;


    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Log.d("QUERYPRED", pred);
        Log.d("QUERYSUCC", successor);


        MatrixCursor cr = new MatrixCursor(new String[]{"key", "value"});


        String hash_key = "";

        SharedPreferences sharedPref = getContext().getSharedPreferences("Your Pref", Context.MODE_PRIVATE);
        try {
            hash_key = genHash(selection);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.d("selection", selection);
        Log.d("selection", hash_key);

        if ((pred.equals("null") && successor.equals("null")) || (node_list.size() == 1)) {
            if (selection.equals("*") || selection.equals("@")) {
                Map<String, ?> keys = sharedPref.getAll();

                for (Map.Entry<String, ?> entry : keys.entrySet()) {
                    Log.d("map values", entry.getKey() + ": " +
                            entry.getValue().toString());


                    MatrixCursor.RowBuilder builder = cr.newRow();
                    builder.add("key", entry.getKey());
                    builder.add("value", entry.getValue());
                }
            } else {
                String val = sharedPref.getString(selection, null);


                MatrixCursor.RowBuilder builder = cr.newRow();
                builder.add("key", selection);
                builder.add("value", val);
                Log.d("query", "key");
                Log.d(selection, val);
            }
            cr.setNotificationUri(getContext().getContentResolver(), uri);

            return cr;
        } else {


            if (selection.equals("*")) { // query all values in all avd
                Log.d("myport at starq", myPort);
                String getting = "";

                String origin = myPort;
                if (sortOrder != null) {
                    origin = sortOrder;
                }
                String port = hm.get(successor);
                Map<String, ?> keystar = sharedPref.getAll();

                for (Map.Entry<String, ?> entry : keystar.entrySet()) {

                    MatrixCursor.RowBuilder builder = cr.newRow();

                    builder.add("key", entry.getKey());
                    builder.add("value", entry.getValue());


                }
                if (!(port.equals(origin))) {


                    Log.d("starq", "succesor port" + port);
                    String send = "star" + ";" + port + ";" + key_arr + ";" + value_arr + ";" + origin;
                    Log.d("SendingTHis", send);
                    try {
                        getting = new ClientTask2().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, send, myPort).get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                    String rec[] = getting.split(";");
                    String key[] = rec[0].split(" ");
                    String val[] = rec[1].split(" ");

                    int l = key.length;

                    Log.d("final k", String.valueOf(l));

                    for (int i = 0; i < l; i++) {
                        key[i] = key[i].replace(",", "").replace("[", "").replace("]", "");
                        val[i] = val[i].replace(",", "").replace("[", "").replace("]", "");
                    }
                    for (int i = 0; i < l; i++) {
                        Log.d("key array", key[i]);
                        MatrixCursor.RowBuilder builder = cr.newRow();
                        builder.add("key", key[i]);
                        builder.add("value", val[i]);

                    }
                }

                cr.setNotificationUri(getContext().getContentResolver(), uri);
                return cr;
            }

            if (selection.equals("@")) //query all values in current avd
            {
                Map<String, ?> keys = sharedPref.getAll();

                for (Map.Entry<String, ?> entry : keys.entrySet()) {
                    Log.d("map values", entry.getKey() + ": " +
                            entry.getValue().toString());


                    MatrixCursor.RowBuilder builder = cr.newRow();
                    builder.add("key", entry.getKey());
                    builder.add("value", entry.getValue());

                }
                cr.setNotificationUri(getContext().getContentResolver(), uri);
                return cr;

            } else { // querying a specific key value
                Log.d("querying", "query3" + selection);
                String hk = "";
                try {
                    hk = genHash(selection);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                int compare1 = pred.compareTo(hk);
                int compare2 = hk.compareTo(node_id);
                int compare3 = node_id.compareTo(pred);
                int compare4 = node_id.compareTo(successor);

                if (compare1 < 0 && compare2 <= 0) {
                    Log.d("querying", "query3.1" + selection);
                    String val = sharedPref.getString(selection, null);
                    Log.d("VALRECD", val);
                    MatrixCursor.RowBuilder builder = cr.newRow();
                    builder.add("key", selection);
                    builder.add("value", val);
                    cr.setNotificationUri(getContext().getContentResolver(), uri);
                    Log.d("CursorMade", "Completed");
                    return cr;


                } else if ((compare3 < 0 && compare4 < 0) && (compare1 < 0 || compare2 < 0)) {
                    Log.d("HelloThere", "query3.2");
                    String val = sharedPref.getString(selection, null);
                    MatrixCursor.RowBuilder builder = cr.newRow();
                    builder.add("key", selection);
                    builder.add("value", val);
                    cr.setNotificationUri(getContext().getContentResolver(), uri);
                    return cr;

                } else {
                    String rec = "";
                    String originating = myPort;


                    String port = hm.get(successor);

                    transfer_msg = "transferQuery" + ";" + port + ";" + successor + ";" + selection + ";" + originating;


                    Log.d("transfer at query", transfer_msg);

                    try {
                        rec = new ClientTask3().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, transfer_msg, myPort).get();//port
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    String split[] = rec.split(";");
                    String k = split[0];
                    String v = split[1];

                    MatrixCursor.RowBuilder builder = cr.newRow();
                    builder.add("key", k);
                    builder.add("value", v);
                    cr.setNotificationUri(getContext().getContentResolver(), uri);
                    return cr;

                }
            }
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {


        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            String msg;

            String rec = "";
            int len = 0;
            int counter = 0;//no. of nodes
            String p1 = "";
            String p2 = "";
            String s1 = "";
            String s2 = "";
            int index1, index2;

            ServerSocket serverSocket = sockets[0];
            Log.d("ServerTask", "Started");


            while (true)

            {
                try {

                    Socket s = serverSocket.accept();


                    DataInputStream di = new DataInputStream(s.getInputStream());
                    DataOutputStream ack = new DataOutputStream(s.getOutputStream());


                    rec = di.readUTF();


                    msg_rec = rec.trim().split(";");
                    msg = msg_rec[1];
                    String type = msg_rec[0];
                    Log.d("ServerReceived", rec);
                    Log.d("ServerReceived", myPort);
                    if (type.equals("new")) {


                        ack.writeUTF("ACK");
                        s.close();
                        Log.d("At server", msg);


                        hm.put(msg, msg_rec[2]);
                        node_list.add(msg);
                        Log.d("list len", String.valueOf(node_list.size()));
                        counter++;


                        Collections.sort(node_list);


                        Log.d("arraylist", String.valueOf(node_list));


                        int index = node_list.indexOf(msg_rec[1]);


                        String tempPred, tempSuccessor;
                        len = node_list.size();
                        if (index == 0) {
                            tempPred = node_list.get((len - 1));
                            tempSuccessor = node_list.get((index + 1));//last node
                            index1 = len - 1;
                            index2 = index + 1;
                        } else if (index == (len - 1)) {
                            tempSuccessor = node_list.get((0));
                            tempPred = node_list.get(index - 1);
                            index1 = index - 1;
                            index2 = 0;
                        } else {
                            tempSuccessor = node_list.get((index + 1));
                            tempPred = node_list.get(index - 1);
                            index1 = index - 1;
                            index2 = index + 1;
                        }
                        Log.d(" new node_id", msg_rec[1]);
                        Log.d("new successor", tempSuccessor);
                        Log.d("new pred", tempPred);


                        if (index1 == 0) {
                            p1 = node_list.get((len - 1));
                            s1 = node_list.get((index1 + 1));//last node
                        } else if (index1 == (len - 1)) {
                            s1 = node_list.get((0));
                            p1 = node_list.get(index1 - 1);
                        } else {
                            s1 = node_list.get((index1 + 1));
                            p1 = node_list.get(index1 - 1);
                        }


                        Log.d("affected node 1", node_list.get(index1));
                        Log.d("affected_node1 succ", s1);
                        Log.d("affected_node1 pred", p1);

                        if (index2 == 0) {
                            p2 = node_list.get((len - 1));
                            s2 = node_list.get((index2 + 1));//last node
                        } else if (index2 == (len - 1)) {
                            s2 = node_list.get((0));
                            p2 = node_list.get(index2 - 1);
                        } else {
                            s2 = node_list.get((index2 + 1));
                            p2 = node_list.get(index2 - 1);
                        }

                        Log.d("affected node 2", node_list.get(index2));
                        Log.d("affected node2 succ", s2);
                        Log.d("affected node2 pred", p2);

                        String port1 = hm.get(msg_rec[1]);
                        String port2 = hm.get(node_list.get(index1));
                        String port3 = hm.get(node_list.get(index2));
                        String str1 = "coming" + ";" + port1 + ";" + msg + ";" + tempSuccessor + ";" + tempPred;
                        String str2 = "coming" + ";" + port2 + ";" + node_list.get(index1) + ";" + s1 + ";" + p1;
                        String str3 = "coming" + ";" + port3 + ";" + node_list.get(index2) + ";" + s2 + ";" + p2;

                        publishProgress(str1);
                        publishProgress(str2);
                        publishProgress(str3);

                    }


                    if (type.equals("list")) {
                        ack.writeUTF("ACK");
                        s.close();
                        pred = msg_rec[4];
                        successor = msg_rec[3];

                        Log.d("my port", myPort);
                        Log.d("FINALnode id", node_id);
                        Log.d("FINALpred", pred);
                        Log.d("FINALsucc", successor);


                    }


                    if (type.equals("transfer")) {
                        ack.writeUTF("ACK");
                        s.close();

                        ContentValues mcv = new ContentValues();
                        mcv.put("key", msg_rec[3]);
                        mcv.put("value", msg_rec[4]);

                        Uri.Builder uriBuilder = new Uri.Builder();
                        uriBuilder.authority("edu.buffalo.cse.cse486586.simpledht.provider");
                        uriBuilder.scheme("content");
                        muri = uriBuilder.build();


                    }
                    if (type.equals("transferQuery")) {
                        Log.d("in", "transferQuery");

                        String selection = msg_rec[3];
                        Uri.Builder uriBuilder = new Uri.Builder();
                        uriBuilder.authority("edu.buffalo.cse.cse486586.simpledht.provider");
                        uriBuilder.scheme("content");
                        muri = uriBuilder.build();
                        Cursor res = query(muri, null, selection, null, null);
                        res.moveToFirst();
                        Log.d("CursorRecd", res.getString(0));
                        int keyIndex = res.getColumnIndex("key");
                        int valueIndex = res.getColumnIndex("value");
                        String key = res.getString(keyIndex);
                        String v = res.getString(valueIndex);
                        ack.writeUTF(key + ";" + v);
                        s.close();


                    }
                    if (type.equals("transferDelQuery")) {
                        Log.d("in", "transferDelQuery");

                        String selection = msg_rec[3];
                        Uri.Builder uriBuilder = new Uri.Builder();
                        uriBuilder.authority("edu.buffalo.cse.cse486586.simpledht.provider");
                        uriBuilder.scheme("content");
                        muri = uriBuilder.build();
                        int res = delete(muri, selection, null);
                        Log.d("CursorRecd", "at server");


                        ack.writeUTF(String.valueOf(res));

                        s.close();


                    }
                    if (type.equals("star")) {
                        String selection = "*";


                        Uri.Builder uriBuilder = new Uri.Builder();
                        uriBuilder.authority("edu.buffalo.cse.cse486586.simpledht.provider");
                        uriBuilder.scheme("content");
                        muri = uriBuilder.build();
                        Cursor res = query(muri, null, selection, null, msg_rec[4]);

                        ArrayList<String> keylist = new ArrayList<String>();
                        ArrayList<String> vallist = new ArrayList<String>();

                        Log.d("cursor", "returned");
                        Log.d("res length", String.valueOf(res.getCount()));


                        for (res.moveToFirst(); !res.isAfterLast(); res.moveToNext()) {

                            Log.d("reaching", "surRes");
                            keylist.add(res.getString(res.getColumnIndex("key")));
                            vallist.add(res.getString(res.getColumnIndex("value")));
                            Log.d(res.getString(res.getColumnIndex("key")), res.getString(res.getColumnIndex("value")));
                        }
                        res.close();

                        ack.writeUTF(keylist + ";" + vallist);
                        s.close();


                    }


                } catch (Exception e) {

                }
            }
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String rec[] = strings[0].trim().split(";");
            String type = rec[0];


            if (type.equals("coming")) {

                String msg = "list" + ";" + rec[1] + ";" + rec[2] + ";" + rec[3] + ";" + rec[4];


                new ClientTask3().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, rec[1]);

            }
            if (type.equals("transfer")) {
                String p = rec[1];

                Log.d("transfer in progUpdate", p);
                new ClientTask3().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, strings[0], p);

            }
        }


    }


    private class ClientTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... msgs) {

            try {

                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT));


                String sending_msg = msgs[0];

                DataOutputStream d = new DataOutputStream(socket.getOutputStream());
                Log.d("At client", msgs[0]);
                d.writeUTF(sending_msg);
                d.flush();
                DataInputStream in = new DataInputStream(socket.getInputStream());
                String s = in.readUTF();
                if (s == "ACK") {
                    socket.close();
                }

            } catch (IOException e) {
                pred = "null";
                successor = "null";
                e.printStackTrace();
            }

            return null;

        }
    }

    private class ClientTask2 extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... msgs) {
            Log.d("client2", "reached");
            try {

                String rec[] = msgs[0].trim().split(";");


                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(rec[1]));


                String sending_msg = msgs[0];

                DataOutputStream d = new DataOutputStream(socket.getOutputStream());

                d.writeUTF(sending_msg);
                d.flush();
                DataInputStream in = new DataInputStream(socket.getInputStream());
                String s = in.readUTF();//reads msg from server
                if (s.equals("ACK")) {
                    socket.close(); // closes socket
                } else {
                    socket.close();

                    return s;
                }


            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;

        }
    }

    private class ClientTask3 extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... msgs) {
            try {
                String rec[] = msgs[0].trim().split(";");


                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(rec[1]));


                String sending_msg = msgs[0];

                DataOutputStream d = new DataOutputStream(socket.getOutputStream());
                Log.d("At client3", msgs[0]);
                d.writeUTF(sending_msg);
                d.flush();
                DataInputStream in = new DataInputStream(socket.getInputStream());
                String s = in.readUTF();
                if (s.equals("ACK")) {
                    socket.close();
                } else {
                    socket.close();

                    return s;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;

        }
    }
}



