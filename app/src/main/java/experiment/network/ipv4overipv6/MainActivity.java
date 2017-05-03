package experiment.network.ipv4overipv6;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.TextView;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;


public class MainActivity extends AppCompatActivity {
    private static String host_ip;// the host ip
    private static String host_mac;// the host mac
    private final String server_ip = "2402:f000:1:4417::900";
    private final int server_port = 5678;
    Socket socket;
    Thread network_thread;

    TextView test;

    Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        init();
        checkNetworkState();
        start_network_thread();
        network_thread.start();

        Intent intent = VpnService.prepare(this);
        if (intent!=null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    public void link_to_server()
    {
        try {
            socket = new Socket(server_ip,server_port);
            Log.e("success","connected to server");
        }catch(Exception e){
            Log.e("error","connection failed");
            e.printStackTrace();
        }

    }

    public void init(){//do some initialization to variables
        test = new TextView(this);
        test = (TextView) findViewById(R.id.tet);
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch(msg.arg1){
                    case R.id.tet:
                        test.setText(host_ip);
                }
            }
        };
    }

    public void checkNetworkState(){ // check the state of network of the phone
        if (!isNetworkConnected(this)) {
            while(true){
                Log.e("error", "no network");
                try {
                    Thread.currentThread().sleep(2000);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                Toast.makeText(this,"no network", Toast.LENGTH_SHORT);
                if(isNetworkConnected(this)){
                    Toast.makeText(this,"network ready", Toast.LENGTH_SHORT);
                    break;
                }
            }
        }
    }

    public void start_network_thread(){
        network_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                host_ip = getIpv6Address();
                if(host_ip!=null) {
                    Message msg = new Message();
                    msg.arg1 = R.id.tet;
                    handler.sendMessage(msg);
                }
                link_to_server();
            }
        });
    }


    public boolean isNetworkConnected(Context context) {// judge whether the network is connected
        if (context!=null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo!=null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    public String getIpv6Address(){ // get the ipv6 address of the phone
        String str = "";

        try
        {
            InetAddress ipv6_address = Inet6Address.getLocalHost();
            String hostname = ipv6_address.getHostName();
            if(ipv6_address!=null)
            str += ipv6_address.getHostAddress() + "this ipv6 address\n";
            Log.e("ipv6address",str);
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
            {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                    {
                        str += inetAddress.getHostAddress().toString() + "\n";
                    }
                }
            }
            return str;
        }
        catch (Exception ex)
        {
            Log.e("Wifi", ex.toString());
        }
        return null;
    }


    protected void onActivityResult(int request, int result, Intent data) {
        if (result==RESULT_OK) {
            Intent intent = new Intent(this, MyVpnService.class);


            startService(intent);
        }
    }

    public native String stringFromJNI();

    static {
        System.loadLibrary("hellojni");
    }
}











