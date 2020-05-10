package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
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
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {
    //**********************************************************************************************
    private static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final Integer[] REMOTE_PORTS = new Integer[]{11108, 11112, 11116, 11120, 11124};
    private static final Integer SERVER_PORT = 10000;
    private static Uri cpURI;
    private Integer msgCount = 0;
    ContentValues valuesToInsert = new ContentValues();
    //**********************************************************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver())
        );


        //******************************************************************************************

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger1.provider");
        uriBuilder.scheme("content");
        cpURI = uriBuilder.build();

        //Copied from PA1
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket" + e.toString());
            return;
        }
        //******************************************************************************************


        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.d("one More---",myPort);
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        //******************************************************************************************
        findViewById(R.id.button4).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Pa2a1", "send button is clicked");
                EditText editText = (EditText) findViewById(R.id.editText1);
                String msgToSend = editText.getText().toString();
                editText.setText("");
                Log.d("Pa2a1", "Got message after clicking send button :" + msgToSend);

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
            }
        });
        //******************************************************************************************
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    //**********************************************************************************************

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     *
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     *
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        Socket s;

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            Log.d("SERVERpa2a1", "Inside doInBackgroud fo server task");
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            try {
                //serverSocket.setReuseAddress(true);
                while (true) {
                    s = serverSocket.accept();


                    //https://stackoverflow.com/questions/14639003/must-server-client-have-reverse-sequence-of-claiming-objectoutputstream-obje
                    ObjectInputStream incomingStream = new ObjectInputStream(s.getInputStream());
                    String str = incomingStream.readUTF();
                    ObjectOutputStream ougoingStream = new ObjectOutputStream(s.getOutputStream());
                    ougoingStream.flush();

                    if (str == null)
                        break;
                    Log.d("SERVERPa2a1", "Message received in server is :" + str);
                    publishProgress(str);

                    s.close();
                    incomingStream.close();
                    ougoingStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Unable to receive/pass the message" + e.getMessage());
                e.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(String... strings) {
            Log.d("SERVERpa2a1", "Inside onProgressUpdate fo server task");
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.setTextColor(Color.RED);
            remoteTextView.append(strReceived + "\n");
            Log.d("SERVERpa2a1", "Message received in server onProgressUpdate is :" + strReceived);
            //***************************************************************************************
            //saving to database
            valuesToInsert.put("key", msgCount++);
            valuesToInsert.put("value", strReceived);
            getContentResolver().insert(cpURI, valuesToInsert);
            //***************************************************************************************
            //return;
            Log.d("SERVERpa2a1", "Existing onProgressUpdate fo server task");
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            Log.d("CLIENTpa2a1", "Inside doInBackgroud fo client task");
            //Socket socket;

            String msgToSend = msgs[0];

            for (int remotePort : REMOTE_PORTS) {
                //Log.d("pa2a1", "size of remotePort is  : " + REMOTE_PORTS.length);
                Log.d("CLIENTpa2a1", "Entering remotePort loop of : " + remotePort);

                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), remotePort);
                    /*
                     * TODO: Fill in your client code that sends out a message.
                     */
                    try {
                        //https://stackoverflow.com/questions/14639003/must-server-client-have-reverse-sequence-of-claiming-objectoutputstream-obje
                        ObjectOutputStream outGoingStream = new ObjectOutputStream(socket.getOutputStream());
                        outGoingStream.writeUTF(msgToSend);
                        Log.d("CLIENTPa2a1", "Message to flush from client is :" + msgToSend);
                        outGoingStream.flush();
                        ObjectInputStream incomingStream = new ObjectInputStream(socket.getInputStream());
                        outGoingStream.close();
                        socket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to sends out a message" + e.getMessage());
                    }
                    Log.d("CLIENTpa2a1", "Closing socket");
                    socket.close();
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }
                Log.d("CLIENTpa2a1", "Existing remotePort loop of : " + remotePort);
            }

            Log.d("CLIENTpa2a1", "Existing doInBackgroud fo client task");
            return null;
        }
    }
    //**********************************************************************************************

}
