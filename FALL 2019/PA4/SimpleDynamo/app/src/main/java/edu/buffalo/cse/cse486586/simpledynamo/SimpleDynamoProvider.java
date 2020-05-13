package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

public class SimpleDynamoProvider extends ContentProvider {
    private static final String TAG = SimpleDynamoProvider.class.getSimpleName();

    private SQLiteDatabase writeIntoDB1, readFromDB1;
    private KeyValueContract.KeyValueDbHelper dbHelper1;

    SQLiteDatabase writeIntoDB2, readFromDB2;
    KeyValueContract.KeyValueDbHelper dbHelper2;


    private final String KEY_VALUE_SEPARATOR = "/";
    private final String CURSOR_SEPARATOR = "#";

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    private static final String STAR_SYMBOL = "*";
    private static final String AT_SYMBOL = "@";

    private static final Integer SERVER_PORT = 10000;
    private static String myPort = "";
    private static String predecessor = "";
    private static String successor = "";
    private static String successor2 = "";
    private static String myHash = "";
    private static TreeMap<String, String> tm = new TreeMap<String, String>();
//    final Uri CONTENT_PROVIDER_URI = Uri.parse("content://edu.buffalo.cse.cse486586.simpledynamo.provider");
    private static HashMap<String, String> buffer = new HashMap<String, String>();

    List<String> queryResults = null;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.d(TAG, "Inside delete function with selection parameter : " + selection);

        if (selection.equals(STAR_SYMBOL)) {
            Log.d(TAG, "Inside * if");
            writeIntoDB1.delete(KeyValueContract.KVEntry.TABLE_NAME, null, null);

            Message m = new Message();
            m.setCurrentStatus("DELETE");
            List<String> templist1 = sendToClient(m);
        }
        if (selection.equals(AT_SYMBOL)) {
            Log.d(TAG, "Inside @ if");
            writeIntoDB1.delete(KeyValueContract.KVEntry.TABLE_NAME, null, null);

            Message m = new Message();
            m.setCurrentStatus("DELETE");
            List<String> templist2 = sendToClient(m);
        } else {
            writeIntoDB1.delete(KeyValueContract.KVEntry.TABLE_NAME, null, null);

            Message m = new Message();
            m.setCurrentStatus("DELETE");
            List<String> templist3 = sendToClient(m);
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "Inside Insert function content key is : " + values.get("key").toString() + " and value is :" + values.get("value"));

        try {
            String insertHash = genHash(values.get(KEY_FIELD).toString());
            String destPort = whichAVDBelongs(insertHash);
            String destPortSucc1 = getSucc1(destPort);
            String destPortSucc2 = getSucc2(destPort);
            Log.d(TAG, "This hash belongs to-- :" + destPort + " its succ1 is :" + destPortSucc1 + " and succ2 is :" + destPortSucc2);
            if (destPort.equals(myPort)) {
                Log.d(TAG, "Inside insert first IF");
                Message m = new Message();
                m.setCurrentStatus("INSERT");
                m.setCvK(values.get("key").toString());
                m.setCvV(values.get("value").toString());
                List<String> tempList = new ArrayList<String>();
                tempList.add(destPort);
                tempList.add(destPortSucc1);
                tempList.add(destPortSucc2);
                m.setPorts(tempList);
                Log.d(TAG, "Sending to client");
                List<String> templist1 = sendToClient(m);
            } else {
                Log.d(TAG, "Inside insert else");
                Message m = new Message();
                m.setDestinationPort(destPort);
                Log.d(TAG, "Sending insert to avd: " + m.getDestinationPort());
                m.setCurrentStatus("INSERT");
                m.setCvK(values.get("key").toString());
                m.setCvV(values.get("value").toString());
                List<String> tempList = new ArrayList<String>();
                tempList.add(destPort);
                tempList.add(destPortSucc1);
                tempList.add(destPortSucc2);
                m.setPorts(tempList);
                Log.d(TAG, "Sending to client");
                List<String> templist2 = sendToClient(m);
            }
        } catch (Exception e) {
            Log.d(TAG, "Inside insert exception " + e.getMessage());
            e.printStackTrace();
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "AVD ONCREATE CALLED");
        Log.d(TAG, "Initializing dbHelper object");
        dbHelper1 = new KeyValueContract.KeyValueDbHelper(this.getContext());
        readFromDB1 = dbHelper1.getReadableDatabase();
        writeIntoDB1 = dbHelper1.getWritableDatabase();
        buffer.clear();

        dbHelper2 = new KeyValueContract.KeyValueDbHelper(this.getContext());
        readFromDB2 = dbHelper2.getReadableDatabase();
        writeIntoDB2 = dbHelper2.getWritableDatabase();

        try {
            TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            myPort = String.valueOf((Integer.parseInt(portStr)));
            tm.put(genHash("5554"), "5554");
            tm.put(genHash("5556"), "5556");
            tm.put(genHash("5558"), "5558");
            tm.put(genHash("5560"), "5560");
            tm.put(genHash("5562"), "5562");
            myHash = genHash(myPort);
            Log.d(TAG, "this avd is :" + myPort + " and hash is :" + myHash);
            successor = getSucc1(myPort);
            successor2 = getSucc2(myPort);
            predecessor = getPred1(myPort);
            Log.d(TAG, "my port : " + myPort + " successor is : " + successor + " predecessor is :" + predecessor + " second successor is :" + successor2);


            Log.d(TAG, "Starting serverTask");
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
//            Log.d(TAG, "CALLING UPDATE FROM ONCREATE");
//            updateContentValue();
            updateContentValue1();

        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket" + e.toString());
            return false;
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "Inside oncreate error of NoSuchAlgorithmException" + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Log.d(TAG, "Inside query function with selection parameter : " + selection);
        queryResults = new ArrayList<String>();
        Cursor retCursor = null;

        if (selection.equals(STAR_SYMBOL)) {
            Log.d(TAG, "Querying @ for * at this AVD :" + myPort);
            Cursor cursor = query(AT_SYMBOL);
            Log.d(TAG, "Returned from query function with @ for * query");
            String cursorToStr = cursorToString(cursor);
            Log.d(TAG, "This avd string cursor is " + myPort + "--" + cursorToStr);
            queryResults.add(cursorToStr);
            Message m = new Message();
            m.setCurrentStatus("QUERY_ALL");
            m.getCursorList().clear();
            m.setCursorList(queryResults);
            List<String> templist1 = sendToClient(m);
            Log.d(TAG, "client returned list size is (ALL) ---------------- :" + templist1.size());
            Log.d(TAG, "client returned list (ALL) :" + templist1.toString());

            retCursor = stringToCursor(templist1);
            Log.d(TAG, "Returning cursor and has first element :" + retCursor.moveToFirst());
            return retCursor;
        } else if (selection.equals(AT_SYMBOL)) {
            Log.d(TAG, "Querying @ at this AVD :" + myPort);
            retCursor = query(selection);
            Log.d(TAG, "Returned from query function with @ query :" + retCursor.moveToFirst());
            return retCursor;
        } else {
            //queryResultSingle = new ArrayList<String>();
            try {

                if (isrightAVD(genHash(selection))) {
                    Log.d(TAG, "This key belong to this AVD :" + myPort);
                    retCursor = query(selection);
                    Log.d(TAG, "Returned from query function with output of this selection: " + selection);
                    Log.d(TAG, "cursor has first element :" + retCursor.moveToFirst());
                    return retCursor;
                } else {
                    Log.d(TAG, "Inside else, transfering query to target avd");
                    Message m = new Message();
                    m.setCurrentStatus("QUERY_SINGLE");
                    m.setMsg(selection);
                    m.setDestinationPort(whichAVDBelongs(genHash(selection)));
                    Log.d(TAG, "Sending for query to avd- : " + m.getDestinationPort());
                    List<String> tempList = new ArrayList<String>();
                    tempList.add(m.getDestinationPort());
                    tempList.add(getSucc1(m.getDestinationPort()));
                    tempList.add(getSucc2(m.getDestinationPort()));
                    m.setPorts(tempList);
                    List<String> templist2 = sendToClient(m);
                    Log.d(TAG, "Client returned list size is (SINGLE)---------------- :" + templist2.size());
                    Log.d(TAG, "Client returned list is (SINGLE) :" + templist2.toString());
                    retCursor = stringToCursor(templist2);
                    Log.d(TAG, "Returning cursor and has first element :" + retCursor.moveToFirst());
                    return retCursor;
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            return retCursor;

        }
    }

    synchronized public Cursor query(String queryKey) {
        Log.d(TAG, "Inside query with single parameter :" + queryKey);

        if (queryKey.equals(AT_SYMBOL)) {
            Log.d(TAG, "Inside first If(@)");
            return readFromDB1.query(KeyValueContract.KVEntry.TABLE_NAME, new String[]{KEY_FIELD, VALUE_FIELD}, null,
                    null, null, null, null);
        } else {
            Log.d(TAG, "Inside else with other than @: " + queryKey);
            String selection = KEY_FIELD + " = ?";
            return readFromDB1.query(KeyValueContract.KVEntry.TABLE_NAME, new String[]{KEY_FIELD, VALUE_FIELD}, selection,
                    new String[]{queryKey}, null, null, null);
        }
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    synchronized public void updateContentValue() {
        //queryResultDouble = new ArrayList<String>();
        Log.d(TAG, "Inside UpdateContentValue Function");
        HashMap<String, String> hm = new HashMap<String, String>();

        Message m = new Message();
        m.setCurrentStatus("CAME_FROM_FAIL");
        List<String> tempList = new ArrayList<String>();
        tempList.add(predecessor);
        tempList.add(successor);
        tempList.add(getSucc2(myPort));
        tempList.add(getPred2(myPort));
        m.setPorts(tempList);
        List<String> templist1 = sendToClient(m);
        if (templist1 != null) {

//            Log.d(TAG, "Client returned list is (UpdateContentValue) " + templist1.toString());
            Log.d(TAG,"BACK in updateContentValue");
            Cursor updates = stringToCursor(templist1);
            Log.d(TAG, "Inside updateValue, updates counts are:" + updates.getCount());
            if (updates.moveToFirst()) {
                do {
                    hm.put(updates.getString(updates.getColumnIndex(KEY_FIELD)), updates.getString(updates.getColumnIndex(VALUE_FIELD)));
                } while (updates.moveToNext());
                Log.d(TAG, "Hash map is : " + hm.toString());
            }
            for (Map.Entry entry : hm.entrySet()) {
                String key = (String) entry.getKey();
                try {
                    if (whichAVDBelongs(genHash(key)).equals(myPort) || whichAVDBelongs(genHash(key)).equals(predecessor) || whichAVDBelongs(genHash(key)).equals(getPred2(myPort))) {
                        ContentValues cv = new ContentValues();
//                        Log.d(TAG, "Inside for if, msg missed");
                        cv.put(KEY_FIELD, key);
                        cv.put(VALUE_FIELD, hm.get(key));
//                        Log.d(TAG, "Inserting value in side UpdateContentValue:" + cv.toString());
                        writeIntoDB1.insertWithOnConflict(KeyValueContract.KVEntry.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                    }
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "Existing updateContentValue function");
        }
    }

    synchronized public void updateContentValue1() {
        Message m = new Message();
        m.setCurrentStatus("BUFFER");
        List<String> temp = sendToClient(m);
    }

    public void updatingContentValue() {
        Log.d(TAG,"Inside UPDATE TEMP FUNCTION");
        if (buffer.size() != 0) {
            Log.d(TAG,"Buffer is : "+buffer.toString());
            for (Map.Entry<String, String> e : buffer.entrySet()) {

                ContentValues cv = new ContentValues();
                Log.d(TAG, "key value are :" + e.getKey() + " " + e.getValue());
                cv.put(KEY_FIELD, e.getKey());
                cv.put(VALUE_FIELD, e.getValue());
                Log.d(TAG, "Inserting value inside UpdateContentValue:");
                writeIntoDB1.insertWithOnConflict(KeyValueContract.KVEntry.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }
        buffer.clear();
    }

    synchronized private String genHash(String input) throws NoSuchAlgorithmException {
//        Log.d(TAG, "Inside genHash function with input : " + input);
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
//        Log.d(TAG, "Exiting genHash function");
        return formatter.toString();
    }

    //Convert Cursor Object to String so that we can pass through Message in string format.
    public String cursorToString(Cursor cursor) {
        Log.d(TAG, "Inside stringToCursor function");
        if (cursor.moveToFirst()) {
            String curToStr = "";
            int keyColumnNo = cursor.getColumnIndex(KEY_FIELD);
            int valueColumnNo = cursor.getColumnIndex(VALUE_FIELD);
            do {
                curToStr += cursor.getString(keyColumnNo) + KEY_VALUE_SEPARATOR + cursor.getString(valueColumnNo) + CURSOR_SEPARATOR;
            } while (cursor.moveToNext());
            //Using subString because last CURSOR_SEPARATOR is extra, so removing that extra seperator
            curToStr = curToStr.substring(0, curToStr.length() - 1);
            Log.d(TAG, "Ending cursorToString function with output : " + curToStr);
            return curToStr;
        }

        return null;
    }

    //Convert received string to cursor so that we can return cursor object in query function
    public Cursor stringToCursor(List<String> results) {
        Log.d(TAG, "Inside stringToCursor function with input :");// + results.toString());
        MatrixCursor finalCursor = new MatrixCursor(new String[]{KEY_FIELD, VALUE_FIELD});

        for (String result : results) {
            if (result != null) {
                String cursorEntries[] = result.split(CURSOR_SEPARATOR);
                for (int i = 0; i < cursorEntries.length; i++) {
                    String entry[] = cursorEntries[i].split(KEY_VALUE_SEPARATOR);
                    finalCursor.newRow().add(KEY_FIELD, entry[0])
                            .add(VALUE_FIELD, entry[1]);
                }
            }
        }
        Log.d(TAG, "Ending stringToCursor function");
        return finalCursor;
    }

    //This function tell weather given hash belong to this AVD or not
    public boolean isrightAVD(String insertHash) throws NoSuchAlgorithmException {
        Log.d(TAG, "Inside isRightAVD function, checking for hash : " + insertHash);
        if (predecessor.equals(successor) && predecessor.equals(myPort))
            return true;
        if ((genHash(predecessor).compareTo(insertHash) < 0 && myHash.compareTo(insertHash) >= 0) || (myHash.compareTo(genHash(predecessor)) < 0 && (insertHash.compareTo(genHash(predecessor)) > 0 || insertHash.compareTo(myHash) < 0))) {
            return true;
        }
        return false;
    }

    //Call client task
    private List<String> sendToClient(Message toSend) {
        try {
            return new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, toSend).get();
        } catch (InterruptedException e) {
            Log.d(TAG, "Inside sendToClient Exception");
            e.printStackTrace();
        } catch (ExecutionException e) {
            Log.d(TAG, "Inside sendToClient Exception");
            e.printStackTrace();
        }
        return null;
    }

    //Tell which AVD this hash belongs
    private String whichAVDBelongs(String givenHash) {
//        Log.d(TAG, "Inside whichAVDBelong function for hash :" + givenHash);
        String p = "";
        if (tm.higherEntry(givenHash) == null) {
//            Log.d(TAG, "Higher value is null");
            p = tm.firstEntry().getValue();
//            Log.d(TAG, "This hash belongs to :" + p);
            return p;

        } else {
//            Log.d(TAG, "Higher value is not null");
            p = tm.higherEntry(givenHash).getValue();
//            Log.d(TAG, "This hash belongs to :" + p);
            return p;
        }
    }


    private String getSucc1(String givenKey) {
        String succ1 = "";
        try {
            if (tm.higherEntry(genHash(givenKey)) != null) {
                succ1 = tm.higherEntry(genHash(givenKey)).getValue();
            } else {
                succ1 = tm.firstEntry().getValue();
            }

        } catch (NoSuchAlgorithmException nsae) {
            Log.d(TAG, "Error inside getSucc1 function " + nsae.getMessage());
            nsae.printStackTrace();
        }
        return succ1;
    }

    private String getSucc2(String givenKey) {
        String succ1 = getSucc1(givenKey);
        String succ2 = "";
        try {
            if (tm.higherEntry(genHash(succ1)) != null) {
                succ2 = tm.higherEntry(genHash(succ1)).getValue();
            } else {
                succ2 = tm.firstEntry().getValue();
            }

        } catch (NoSuchAlgorithmException nsae) {
            Log.d(TAG, "Error inside getSucc2 function " + nsae.getMessage());
            nsae.printStackTrace();
        }
        return succ2;
    }

    private String getPred1(String givenKey) {
        String pred1 = "";
        try {
            if (tm.lowerEntry(genHash(givenKey)) != null) {
                pred1 = tm.lowerEntry(genHash(givenKey)).getValue();
            } else {
                pred1 = tm.lastEntry().getValue();
            }

        } catch (NoSuchAlgorithmException nsae) {
            Log.d(TAG, "Error inside getPred1 function " + nsae.getMessage());
            nsae.printStackTrace();
        }
        return pred1;
    }

    private String getPred2(String givenKey) {
        String pred1 = getPred1(givenKey);
        String pred2 = "";
        try {
            if (tm.lowerEntry(genHash(pred1)) != null) {
                pred2 = tm.lowerEntry(genHash(pred1)).getValue();
            } else {
                pred2 = tm.lastEntry().getValue();
            }

        } catch (NoSuchAlgorithmException nsae) {
            Log.d(TAG, "Error inside getPred2 function " + nsae.getMessage());
            nsae.printStackTrace();
        }
        return pred2;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        Socket s;
        Message receivedObj = null;


        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            Log.d(TAG, "Inside doInBackgroud of server task");

            ServerSocket serverSocket = sockets[0];

            ObjectInputStream incomingStream = null;
            ObjectOutputStream outgoingStream = null;
            try {
                serverSocket.setReuseAddress(true);
                while (true) {

                    Log.d(TAG, "Inside server Starting");
                    s = serverSocket.accept();
                    if (s != null) {
                        incomingStream = new ObjectInputStream(s.getInputStream());
                        outgoingStream = new ObjectOutputStream(s.getOutputStream());

                        receivedObj = (Message) (incomingStream.readObject());
                        Log.d(TAG, "Message Received in server :" + receivedObj.toString());

                        //Enter in this IF when message is to inserted
                        if (receivedObj.getCurrentStatus().equals("INSERT")) {
                            Log.d(TAG, "Inside SERVER first IF(INSERT)");
                            ContentValues cv = new ContentValues();
                            Log.d(TAG, "Inserting key value at loc - " + myPort + " key-" + receivedObj.getCvK() + " value-" + receivedObj.getCvV());
                            cv.put(KEY_FIELD, receivedObj.getCvK());
                            cv.put(VALUE_FIELD, receivedObj.getCvV());
                            Log.d(TAG, "Inserting Forwarded Message to :" + myPort);
                            writeIntoDB2.insertWithOnConflict(KeyValueContract.KVEntry.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                            outgoingStream.writeObject("Insert received");
                            outgoingStream.flush();
                        }
                        //Enter in this IF when predecessor sent a delete request
                        if (receivedObj.getCurrentStatus().equals("DELETE")) {
                            Log.d(TAG, "Inside SERVER second IF(DELETE)");
                            //delete(CONTENT_PROVIDER_URI, AT_SYMBOL, null);
                            writeIntoDB2.delete(KeyValueContract.KVEntry.TABLE_NAME, null, null);
                            outgoingStream.writeObject("delete received");
                            outgoingStream.flush();
                        }

                        //Enter in this IF when this AVD ALL content Value is requested
                        if (receivedObj.getCurrentStatus().equals("QUERY_ALL")) {
                            Log.d(TAG, "Inside SERVER third IF(QUERY)");
                            Cursor c = readFromDB2.query(KeyValueContract.KVEntry.TABLE_NAME, new String[]{KEY_FIELD, VALUE_FIELD}, null,
                                    null, null, null, null);
                            String cstr = cursorToString(c);
                            Log.d(TAG, "Server This avd string cursor is " + myPort + "---" + cstr);
                            List<String> temp = receivedObj.getCursorList();
                            temp.add(cstr);
                            receivedObj.setCursorList(temp);
                            Log.d(TAG, "Sending back with attached cursor " + receivedObj);
                            outgoingStream.writeObject(receivedObj);
                            outgoingStream.flush();
                        }

                        //Enter in this IF when this AVD particular key is requested
                        if (receivedObj.getCurrentStatus().equals("QUERY_SINGLE")) {
                            Log.d(TAG, "Inside SERVER fourth IF(QUERY_SINGLE) for :" + receivedObj.getMsg());
                            if (isrightAVD(genHash(receivedObj.getMsg())) || getSucc1(whichAVDBelongs(genHash(receivedObj.getMsg()))).equals(myPort)) {


                                Log.d(TAG, "Inside first if of QUERY_SINGLE");
                                String selection = KEY_FIELD + " = ?";
                                Cursor c = readFromDB2.query(KeyValueContract.KVEntry.TABLE_NAME, new String[]{KEY_FIELD, VALUE_FIELD}, selection,
                                        new String[]{receivedObj.getMsg()}, null, null, null);
                                String cstr = cursorToString(c);
                                if (cstr == null) {
                                    Log.d(TAG, "cstr is null(if)");
                                    updateContentValue1();
                                    Cursor c2 = readFromDB2.query(KeyValueContract.KVEntry.TABLE_NAME, new String[]{KEY_FIELD, VALUE_FIELD}, selection,
                                            new String[]{receivedObj.getMsg()}, null, null, null);
                                    String cstr2 = cursorToString(c2);
                                    Log.d(TAG, "Server This avd string cursor is " + myPort + "---" + cstr);
                                    List<String> tempList = new ArrayList<String>();
                                    tempList.add(cstr2);
                                    receivedObj.setCursorList(tempList);
                                    Log.d(TAG, "Sending back with attached cursor " + receivedObj);
                                    outgoingStream.writeObject(receivedObj);
                                    outgoingStream.flush();
                                } else {
                                    Log.d(TAG, "cstr is not null(else)");
                                    Log.d(TAG, "Server This avd string cursor is " + myPort + "---" + cstr);
                                    List<String> tempList = new ArrayList<String>();
                                    tempList.add(cstr);
                                    receivedObj.setCursorList(tempList);
                                    Log.d(TAG, "Sending back with attached cursor " + receivedObj);
                                    outgoingStream.writeObject(receivedObj);
                                    outgoingStream.flush();
                                }
                            } else {
                                Log.d(TAG, "Inside else of QUERY_SINGLE");
                                receivedObj.setMsg("NOT THIS");
                                outgoingStream.writeObject(receivedObj);
                                outgoingStream.flush();
                            }
                        }
                        //Fail AVD wants to update its content value
                        if (receivedObj.getCurrentStatus().equals("CAME_FROM_FAIL")) {
                            Log.d(TAG, "Inside SERVER fifth IF(CAME_FROM_FAIL)");
                            Cursor c = readFromDB2.query(KeyValueContract.KVEntry.TABLE_NAME, new String[]{KEY_FIELD, VALUE_FIELD}, null,
                                    null, null, null, null);
                            String cstr = cursorToString(c);
                            Log.d(TAG, "Inside Server, This avd string cursor is " + myPort + "---" + cstr);
                            List<String> temp = receivedObj.getCursorList();
                            temp.add(cstr);
                            receivedObj.setCursorList(temp);
                            Log.d(TAG, "Sending back with attached cursor " + receivedObj);
                            outgoingStream.writeObject(receivedObj);
                            outgoingStream.flush();
                        }
                        if (receivedObj.getCurrentStatus().equals("BUFFER")) {
                            Log.d(TAG,"Inside Server Buffer");
                            Message m = new Message();
                            Log.d(TAG,"Current Buffer is : "+buffer.toString());
                            m.setHm(buffer);
                            Log.d(TAG,"Current buffer after setting : "+m.getHm().toString());
                            Log.d(TAG, "Sending back with attached buffer " + m.toString());
                            outgoingStream.writeObject(m);
                            outgoingStream.flush();
                            buffer.clear();

                        }
                    }

                }
            } catch (Exception e) {
                Log.e(TAG, "Inside server Eexception--- :" + s.getPort());
                e.printStackTrace();
            } finally {
                try {
                    if (s != null)
                        s.close();
                    if (outgoingStream != null)
                        outgoingStream.close();
                    if (incomingStream != null)
                        incomingStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return null;
        }
    }

    private class ClientTask extends AsyncTask<Message, Void, List<String>> {

        List<String> toReturn = new ArrayList<String>();

        @Override
        protected List<String> doInBackground(Message... msgs) {
            Log.d(TAG, "Inside doInBackgroud fo client task");
            Socket socket = null;

            String receivedStr = "";
            ObjectOutputStream outGoingStream = null;
            ObjectInputStream incomingStream = null;
            Message msgToSend = msgs[0];
            Log.d(TAG, "Message to send from client: " + msgToSend.toString());

            try {
                //When AVD wants to forward insert to successor
                if (msgToSend.getCurrentStatus().equals("INSERT")) {
                    Log.d(TAG, "(IF1)Inside doInBackground client for sending INSERT message to Three nodes");


                    for (String str : msgToSend.getPorts()) {
                        try {
                            Log.d(TAG, "sending to :" + str);
                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(str) * 2);
                            outGoingStream = new ObjectOutputStream(socket.getOutputStream());
                            incomingStream = new ObjectInputStream(socket.getInputStream());
                            outGoingStream.writeObject(msgToSend);
                            outGoingStream.flush();

                            receivedStr = (String) (incomingStream.readObject());
                            Log.d(TAG, "Message received from :" + str + " is :" + receivedStr);

                        } catch (Exception ex) {
                            Log.d(TAG, "Error during INSERT :" + ex.getMessage());
                            ex.printStackTrace();
                            Log.d(TAG, "Inside insert exception, Inserting in buffer" + msgToSend.getCvK() + " " + msgToSend.getCvV());
                            buffer.put(msgToSend.getCvK(), msgToSend.getCvV());

                        }
                    }
                }
                //When AVD wants to delete content value of other AVD
                if (msgToSend.getCurrentStatus().equals("DELETE")) {
                    Log.d(TAG, "(IF2)Inside doInBackground client for sending DELETE message to all active node");
                    for (Map.Entry<String, String> entry : tm.entrySet()) {
                        try {

                            if (entry.getValue().equals(myPort))
                                continue;
                            Log.d(TAG, "Sending Update Message to :" + entry.getValue());
                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(entry.getValue()) * 2);
                            outGoingStream = new ObjectOutputStream(socket.getOutputStream());
                            incomingStream = new ObjectInputStream(socket.getInputStream());
                            outGoingStream.writeObject(msgToSend);
                            outGoingStream.flush();

                            receivedStr = (String) (incomingStream.readObject());
                            Log.d(TAG, "Message received By" + entry.getValue() + " - " + receivedStr);
                        } catch (Exception ex) {
                            Log.d(TAG, "Error during DELETE :" + ex.getMessage());
                            ex.printStackTrace();

                        }
                    }
                }
                //When AVD wants to get content value from other AVD
                if (msgToSend.getCurrentStatus().equals("QUERY_ALL")) {
                    Log.d(TAG, "(IF3)Inside doInBackground client for sending QUERY_ALL message to all active node");
                    for (Map.Entry<String, String> entry : tm.entrySet()) {
                        try {
                            if (entry.getValue().equals(myPort))
                                continue;
                            Log.d(TAG, "Sending Query Message to QUERY_ALL :" + entry.getValue());
                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(entry.getValue()) * 2);
                            outGoingStream = new ObjectOutputStream(socket.getOutputStream());
                            incomingStream = new ObjectInputStream(socket.getInputStream());
                            outGoingStream.writeObject(msgToSend);
                            outGoingStream.flush();

                            Message receivedMessage = (Message) (incomingStream.readObject());
                            Log.d(TAG, "Message received By :" + entry.getValue() + " - " + receivedMessage);
                            msgToSend.getCursorList().clear();
                            msgToSend.setCursorList(receivedMessage.getCursorList());
                            toReturn = msgToSend.getCursorList();

                        } catch (Exception ex) {
                            Log.d(TAG, "Error during QUERY_ALL :" + ex.getMessage());
                            ex.printStackTrace();

                        }
                    }
                    Log.d(TAG, "Returning back to query to print this queryALL");
                }
                //When AVD wants to get content value of a particular key
                if (msgToSend.getCurrentStatus().equals("QUERY_SINGLE")) {
                    Log.d(TAG, "(IF4)Inside doInBackground client for sending QUERY_SINGLE message to target node");
                    try {
                        Log.d(TAG, "Sending Query Message to QUERY_SINGLE :" + msgToSend.getDestinationPort());
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgToSend.getDestinationPort()) * 2);
                        outGoingStream = new ObjectOutputStream(socket.getOutputStream());
                        incomingStream = new ObjectInputStream(socket.getInputStream());
                        outGoingStream.writeObject(msgToSend);
                        outGoingStream.flush();

                        Message receivedMessage = (Message) (incomingStream.readObject());
                        Log.d(TAG, "Message received By :" + msgToSend.getDestinationPort() + " - " + receivedMessage);
                        if (!receivedMessage.getMsg().equals("NOT THIS")) {
                            toReturn = receivedMessage.getCursorList();
                        }
                        Log.d(TAG, "Exiting Client IF4");
                    } catch (Exception ex) {
                        Log.d(TAG, "Inside QUERY_SINGLE exception :" + ex.getMessage());
                        if (socket != null)
                            socket.close();
                        Log.d(TAG, "As avd is down, now querying from its successor" + msgToSend.getPorts().get(1));
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgToSend.getPorts().get(1)) * 2);
                        outGoingStream = new ObjectOutputStream(socket.getOutputStream());
                        incomingStream = new ObjectInputStream(socket.getInputStream());
                        outGoingStream.writeObject(msgToSend);
                        outGoingStream.flush();

                        Message receivedMessage = (Message) (incomingStream.readObject());
                        Log.d(TAG, "Message received By(exception) :" + msgToSend.getDestinationPort() + " - " + receivedMessage);
                        if (!receivedMessage.getMsg().equals("NOT THIS")) {
                            toReturn = receivedMessage.getCursorList();
                        }
                        Log.d(TAG, "Exiting Client IF4 catch");
                    }

                }
                if (msgToSend.getCurrentStatus().equals("CAME_FROM_FAIL")) {
                    Log.d(TAG, "(IF5)Inside doInBackground client for sending CAME_FROM_FAIL message to pred and succ");
                    for (String str : msgToSend.getPorts()) {
                        try {
                            if (str.equals(myPort))
                                continue;
                            Log.d(TAG, "Sending Query Message to CAME_FROM_FAIL :" + str);
                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(str) * 2);
                            outGoingStream = new ObjectOutputStream(socket.getOutputStream());
                            incomingStream = new ObjectInputStream(socket.getInputStream());
                            outGoingStream.writeObject(msgToSend);
                            outGoingStream.flush();

                            Message receivedMessage = (Message) (incomingStream.readObject());
                            Log.d(TAG, "Message received By :" + str + " - " + receivedMessage);
                            msgToSend.getCursorList().clear();
                            msgToSend.setCursorList(receivedMessage.getCursorList());
                            toReturn = msgToSend.getCursorList();
                        } catch (Exception ex) {
                            Log.d(TAG, "Error during CAME_FROM_FAIL :" + ex.getMessage());
                            ex.printStackTrace();
                        }
                    }
                    Log.d(TAG, "Returning back to updateContentValue to print this CAME_FROM_FAIL ");
                }
                if (msgToSend.getCurrentStatus().equals("BUFFER")) {
                    Log.d(TAG,"Inside Buffer if client");
                    for (Map.Entry<String, String> entry : tm.entrySet()) {
                        try {

                            if (entry.getValue().equals(myPort))
                                continue;
                            Log.d(TAG, "Sending Update Message to :" + entry.getValue());
                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(entry.getValue()) * 2);
                            outGoingStream = new ObjectOutputStream(socket.getOutputStream());
                            incomingStream = new ObjectInputStream(socket.getInputStream());
                            outGoingStream.writeObject(msgToSend);
                            msgToSend.getHm().clear();
                            outGoingStream.flush();
                            Message receivedObj = (Message) (incomingStream.readObject());
                            Log.d(TAG,"Received object in  buffer is :"+receivedObj.toString());
                            if (receivedObj.getHm().size() != 0) {
                                for (Map.Entry<String, String> e : receivedObj.getHm().entrySet()) {
                                    Log.d(TAG,"Putting key value :"+e.getKey()+" "+e.getValue());
                                    buffer.put(e.getKey(), e.getValue());
                                }
                            }
                            Log.d(TAG, "Message received from :" + entry.getValue() + " is :" + receivedStr);
                        } catch (Exception ex) {
                            Log.d(TAG, "Inside client buffer exception" + ex.getMessage());
                            ex.printStackTrace();
                        }
                    }
                    Log.d(TAG,"NEXT LINE IS UPDATE TEMP FUNCTION with buffer : "+buffer.toString());
                    updatingContentValue();
                }

            } catch (Exception e) {
                Log.e(TAG, "Inside ClientTask Exception--- " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    if (socket != null)
                        socket.close();
                    if (outGoingStream != null)
                        outGoingStream.close();
                    if (incomingStream != null)
                        incomingStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            Log.d("CLIENTpa2a1", "Existing doInBackgroud fo client task");
            Log.d(TAG, "Returning list from client");
            return toReturn;
        }
    }
}