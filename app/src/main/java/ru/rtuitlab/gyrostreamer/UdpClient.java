package ru.rtuitlab.gyrostreamer;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class UdpClient
{
    private AsyncTask<Void, Void, Void> asyncClient;
    private InetAddress mAddress;
    private int mPort;

    public UdpClient(String address, int port) throws UnknownHostException {
        mAddress = InetAddress.getByName(address);
        mPort = port;
    }

    @SuppressLint("StaticFieldLeak")
    public void send(final ByteBuffer buffer)
    {
        asyncClient = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params)
            {
                DatagramSocket ds = null;

                try
                {
                    ds = new DatagramSocket();

                    DatagramPacket dp;
                    byte[] arr = buffer.array();
                    dp = new DatagramPacket(arr, arr.length, mAddress, mPort);

                    //ds.setBroadcast(true);
                    ds.send(dp);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    if (ds != null)
                    {
                        ds.close();
                    }
                }
                return null;
            }

            protected void onPostExecute(Void result)
            {
                super.onPostExecute(result);
            }
        };

        asyncClient.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}