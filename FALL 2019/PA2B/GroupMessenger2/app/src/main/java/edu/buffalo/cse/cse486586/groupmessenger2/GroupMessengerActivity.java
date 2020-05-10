package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {

    static int failedPort = 0;      //Store the port which is failed
    static int receivedFailedPortStatus = 0;
    private static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final Integer[] REMOTE_PORTS = new Integer[]{11108, 11112, 11116, 11120, 11124};
    private static final Integer SERVER_PORT = 10000;
    private static Uri cpURI;
    private Integer msgCount = 0;   //Used inside content provider
    static int privateCount = 1;        // Private count of each AVD
    ContentValues valuesToInsert = new ContentValues();
    static HashMap<Integer, Integer> pNo;
    static String myPort;           // Store port no of this AVD

    /*
    PriorityBlockingQueue is synchronized PriorityQueue
    It will sort the Message object on the basis of proposedX and proposedY
     */
    static PriorityBlockingQueue<Message> pq = new PriorityBlockingQueue<Message>(25, new Comparator<Message>() {
        @Override
        public int compare(Message lhs, Message rhs) {
            if (lhs.getProposedX() == rhs.getProposedX())
                return (lhs.getProposedY() - rhs.getProposedY());
            else
                return (lhs.getProposedX() - rhs.getProposedX());
        }

    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver())
        );

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
        uriBuilder.scheme("content");
        cpURI = uriBuilder.build();

        //AVD number corresponding to each port
        pNo = new HashMap<Integer, Integer>();
        pNo.put(5554, 1);       //5554
        pNo.put(5556, 2);       //5556
        pNo.put(5558, 3);       //5558
        pNo.put(5560, 4);       //5560
        pNo.put(5562, 5);       //5562

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr)));
        Log.d("Inside Oncreate", "Here My Port is " + myPort + " and here is this processNo :" + pNo.get(Integer.parseInt(myPort)));
        //privateCount = 50 * Integer.parseInt(myPort);
        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket" + e.toString());
            return;
        }

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        findViewById(R.id.button4).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "send button is clicked");
                EditText editText = (EditText) findViewById(R.id.editText1);
                String msgToSend = editText.getText().toString();
                editText.setText("");
                Log.d("Inside clickListener", "Got message after clicking send button :" + msgToSend);

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /*
    Delete from PriorityQueue by matching the msg
     */
    public void deleteFromPQusingMsg(String msg) {
        Iterator value = pq.iterator();
        while (value.hasNext()) {
            Message m = (Message) value.next();
            if (m.getMessage().equals(msg)) {
                Log.d(TAG, "Deleting Object using message :" + m.toString());
                value.remove();
            }
        }
    }

    /*
    Delete from PriorityQueue by matching the port no.
     */
    public void deleteFromPQusingPort(int port) {
        Iterator value = pq.iterator();
        while (value.hasNext()) {
            Message m = (Message) value.next();
            if (port != 0 && m.getSourcePort() == port && !m.isFinal()) {
                Log.d(TAG, "Deleting failedPort Object :" + m.toString());
                value.remove();
            }
        }
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        Socket s;
        Message receivedObj = null;

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            Log.d(TAG, "Inside doInBackgroud fo server task");
            ServerSocket serverSocket = sockets[0];

            ObjectInputStream incomingStream = null;
            ObjectOutputStream outgoingStream = null;
            try {
                serverSocket.setReuseAddress(true);
                while (true) {

                    Log.d(TAG, "Inside server Starting");
                    s = serverSocket.accept();

                    incomingStream = new ObjectInputStream(s.getInputStream());
                    outgoingStream = new ObjectOutputStream(s.getOutputStream());

                    receivedObj = (Message) (incomingStream.readObject());//readUTF();
                    Log.d("Server while loop", "Received msg in server of" + myPort + ":" + receivedObj.toString());

                    deleteFromPQusingPort(receivedObj.getFailedPort());
                    //Here it will enter in if block when AVD have to propose the sequence and it will
                    // enter in else when there is final message received
                    if(!receivedObj.getMessage().equals("FailedInfo")) {
                        if (!receivedObj.isFinal()) {
                            Log.d("in serverDoInBackGround", "Inside proposal if-------");
                            receivedObj.setProposedX(privateCount);
                            receivedObj.setProposedY(pNo.get(Integer.parseInt(myPort)));
                            privateCount++;

                            outgoingStream.writeObject(receivedObj);
                            outgoingStream.flush();
                            pq.add(receivedObj);
                            Log.d(TAG, "Adding proposal object to port " + myPort);
                        } else {
                            if (receivedObj.getProposedX() >= privateCount)
                                privateCount = receivedObj.getProposedX() + 1;
                            deleteFromPQusingMsg(receivedObj.getMessage());

                            Log.d(TAG, "Adding message object to priority queue : " + receivedObj.toString());
                            pq.add(receivedObj);
                            outgoingStream.writeObject(failedPort);
                            outgoingStream.flush();
                        }
                    }
                    else{
                        outgoingStream.writeObject(failedPort);
                        outgoingStream.flush();
                    }
                    publishMessage();

                }
            } catch (Exception e) {
                Log.e(TAG, "Inside server IOEexception :" + s.getPort());
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

        //It will publish Message by checking the priority queue and its final status
        public void publishMessage() {
            Log.d("publishMessage", "PQ size is :" + pq.size() + " PQ peek object is :" + pq.peek().toString());
            if (pq.size() != 0 && pq.peek().isFinal()) {
                while (pq.size() != 0 && pq.peek().isFinal()) {
                    Message fromQueue = pq.poll();
                    Log.d(TAG, "Message passed to publishProgress method" + fromQueue.toString());
                    publishProgress(fromQueue.getMessage());
                }
            }
        }

        protected void onProgressUpdate(String... strings) {
            Log.d(TAG, "Inside onProgressUpdate fo server task");
            /*
             * The following code displays what is received in doInBackground().
             */
            try {
                String strReceived = strings[0].trim();
                TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                remoteTextView.setTextIsSelectable(true);
                remoteTextView.setFocusable(true);
                remoteTextView.setFocusableInTouchMode(true);
                remoteTextView.append(strReceived);
                remoteTextView.append("\n");

                Log.d("server OnProgressUpdate", "Message received in server onProgressUpdate is :" + strReceived);
                //saving to database

                valuesToInsert.put("key", msgCount++);
                valuesToInsert.put("value", strReceived);
                getContentResolver().insert(cpURI, valuesToInsert);

                Log.d(TAG, "Existing onProgressUpdate fo server task");
            } catch (Exception ex) {
                Log.e(TAG, "Inside onProgressUpdateError" + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {


        @Override
        protected Void doInBackground(String... msgs) {

            Socket socket = null;

            Log.d(TAG, "Inside doInBackgroud fo client task");
            //Creating Message object
            String msgToSend = msgs[0];
            Message msg = new Message();
            msg.setMessage(msgToSend);
            Log.d("CLIENT doInBackground", "My port is : " + myPort);
            msg.setSourcePort(Integer.parseInt(myPort));
            msg.setSourceX(privateCount);
            privateCount++;
            msg.setSourceY(pNo.get(Integer.parseInt(myPort)));
            msg.setProposedX(msg.getSourceX());
            msg.setProposedY(msg.getSourceY());
            msg.setFailedPort(failedPort);


            ObjectOutputStream outGoingStream = null;
            ObjectInputStream incomingStream = null;
            //HashMap to store the proposed value and finding the max
            HashMap<Integer, Integer> hm = new HashMap<Integer, Integer>();
            //First for loop to ask for proposal from all AVD
            for (int remotePort : REMOTE_PORTS) {
                Log.d("CLIENT for Loop1", "Entering remotePort loop of : " + remotePort / 2);
                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), remotePort);
                    outGoingStream = new ObjectOutputStream(socket.getOutputStream());
                    incomingStream = new ObjectInputStream(socket.getInputStream());
                    //socket.setSoTimeout(100);

                    //https://stackoverflow.com/questions/14639003/must-server-client-have-reverse-sequence-of-claiming-objectoutputstream-obje
                    outGoingStream.writeObject(msg);
                    Log.d("CLIENT for Loop1", "Message to flush from client is :" + msg.getMessage());
                    outGoingStream.flush();


                    if (incomingStream != null) {
                        Message receivedObj = (Message) (incomingStream.readObject());
                        hm.put(receivedObj.getProposedY(), receivedObj.getProposedX());
                    } else {
                        Log.d(TAG, "RECEIVED NULL VALUE FROM PROPOSED PROCESS");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "ClientTask socket IOException in first loop" + e.getMessage());
                    Log.e(TAG, "Inside first for loop exception");
                    Log.d(TAG, "********************************This avd is shut down : " + remotePort / 2);
                    failedPort = remotePort / 2;
                    msg.setFailedPort(failedPort);
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
                Log.d("CLIENT for Loop1", "Existing first remotePort loop of : " + remotePort / 2);
            }

            //Finding Max from all the proposed sequences
            Map.Entry<Integer, Integer> maxEntry = null;
            for (Map.Entry<Integer, Integer> i : hm.entrySet()) {
                if (maxEntry == null || i.getValue().compareTo(maxEntry.getValue()) > 0) {
                    maxEntry = i;
                }
                if (maxEntry == null || i.getValue().compareTo(maxEntry.getValue()) == 0) {
                    if (i.getKey().compareTo(maxEntry.getKey()) > 0)
                        maxEntry = i;
                }
            }


            msg.setFinal(true);
            msg.setProposedX(maxEntry.getValue());
            msg.setProposedY(maxEntry.getKey());
//            privateCount = maxEntry.getValue();
//            privateCount++;

            //Second for loop for telling all AVD the final proposed sequence
            for (int remotePort : REMOTE_PORTS) {
                Log.d("CLIENT for Loop2", "Entering remotePort loop of : " + remotePort / 2);
                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), remotePort);
                    outGoingStream = new ObjectOutputStream(socket.getOutputStream());
                    incomingStream = new ObjectInputStream(socket.getInputStream());

                    outGoingStream.writeObject(msg);
                    Log.d("CLIENT for Loop2", "Message to flush from client is :" + msg.getMessage());
                    Log.d(TAG, "Flushing to port : " + remotePort / 2);
                    outGoingStream.flush();

                    receivedFailedPortStatus = (Integer) incomingStream.readObject();
                    Log.d(TAG, "Client 2 loop received :" + receivedFailedPortStatus);

                } catch (Exception e) {
                    Log.e(TAG, "ClientTask socket IOException in second loop" + e.getMessage());
                    Log.e(TAG, "Inside second for loop exception");
                    failedPort = remotePort / 2;
                    msg.setFailedPort(failedPort);
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

                Log.d("CCLIENT for Loop2", "Existing second remotePort loop of : " + remotePort / 2);
            }

            if (failedPort != 0 && receivedFailedPortStatus == 0) {
                Message tempM = new Message();
                tempM.setMessage("FailedInfo");
                tempM.setFailedPort(failedPort);
                for (int remotePort : REMOTE_PORTS) {
                    Log.d("CLIENT for Loop2", "Entering remotePort loop of : " + remotePort / 2);
                    if (remotePort != Integer.parseInt(myPort) && remotePort != failedPort) {
                        try {
                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), remotePort);
                            outGoingStream = new ObjectOutputStream(socket.getOutputStream());
                            incomingStream = new ObjectInputStream(socket.getInputStream());

                            outGoingStream.writeObject(tempM);
                            Log.d("CLIENT for Loop2", "Message to flush from client is :" + msg.getMessage());
                            Log.d(TAG, "Flushing to port : " + remotePort / 2);
                            outGoingStream.flush();

                            Object ack =  incomingStream.readObject();

                        } catch (Exception e) {
                            Log.e(TAG, "ClientTask socket IOException in third loop" + e.getMessage());
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
                    }

                    Log.d("CCLIENT for Loop2", "Existing second remotePort loop of : " + remotePort / 2);
                }
            }

            Log.d("CLIENTpa2a1", "Existing doInBackgroud fo client task");
            return null;
        }
    }

}
