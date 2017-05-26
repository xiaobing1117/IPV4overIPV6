package experiment.network.ipv4overipv6;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import experiment.network.ipv4overipv6.MyVpnService;
import android.os.Bundle;

import android.util.Log;
import android.util.Xml;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {
    private static String host_ip;// the host ip
    private static String host_mac;// the host mac
    private final String server_ip = "2402:f000:1:4417::900";
    private final int server_port = 5678;
    private String envi_dir;
    Socket socket;
    Thread network_thread;

    MyVpnService myVpnService;

    Handler handler;

    /*wifi related*/
    WifiManager wifiManager;
    WifiInfo wifiinfo;

    final int UPDATE_INFO = 1;
    final int NETWORK_CONDITION = 2;
    final int BAD_LINK = 3;
    final int LINKED = 4;
    final int TIME_UPDATE = 5;
    final int BUFSIZE = 1024;
    final int NETWORK2 = 6;
    final int NETWORK1 = 7;
    public File enviroment_dir;//目前的路径
    public File FIFO;//IP管道
    public File FIFO2;//流量管道

    int flag = 0;//用于定时器
    int time = 0;
    public byte buf[];

    String pipe_message;
    String ipv4Addr;
    String DNSserver[];
    String sock;
    String ipv6Addr;

    int total_time;//运行时长
    int upload_speed;//上传速度
    int download_speed;//下载速度
    int total_upload_size;//上传总流量
    int total_download_size;//下载总流量
    int total_upload_packages;//上传总包数
    int total_download_packages;//下载总包数

    String local_ipv6 = "";
    TextView text_ipv4Addr;
    TextView text_ipv6Addr;
    TextView text_total_time;
    TextView text_upload_speed;
    TextView text_download_speed;
    TextView text_total_upload_size;
    TextView text_total_upload_packages;
    TextView text_total_download_size;
    TextView text_total_download_packages;
    TextView network_condition;

    public DecimalFormat df;

    Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        try{
            createFIFO();
        }catch(Exception e){
            e.printStackTrace();
        }
        init();

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch(msg.arg1){
                    case UPDATE_INFO:

                        text_upload_speed.setText(Double.toString(upload_speed));
                        text_download_speed.setText(Double.toString(download_speed));
                        text_total_upload_size.setText(Double.toString(total_upload_size));
                        text_total_download_size.setText(Double.toString(total_download_size));
                        text_total_upload_packages.setText(Double.toString(total_upload_packages));
                        text_total_download_packages.setText(Double.toString(total_download_packages));

                        break;
                    case NETWORK_CONDITION:
                        switch(msg.arg2) {
                            case BAD_LINK:
                                network_condition.setText("连接失败，请检查网络");
                                break;
                            case LINKED:
                                network_condition.setText("已连接");
                                break;
                        }
                        break;
                    case TIME_UPDATE:
                        time ++;
                        text_total_time.setText(Integer.toString(time));
                        break;
                    case NETWORK2:
                        text_ipv6Addr.setText(local_ipv6);
                        break;
                    case NETWORK1:
                        text_ipv4Addr.setText(ipv4Addr);
                        break;
                }
            }
        };


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                checkNetworkState();
                Log.e("start connection","here");
                setConnection(envi_dir);
            }
        });

        thread.start();
        timer();
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                local_ipv6 = getIpv6Address();
                Message message = new Message();
                message.arg1 = NETWORK2;
                handler.sendMessage(message);
                try {
                    //write_pipe(FIFO, str);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
       thread2.start();
    }


    public void init(){//do some initialization to variables


        wifiManager=(WifiManager)getSystemService(Context.WIFI_SERVICE);
        wifiinfo = wifiManager.getConnectionInfo();
        buf = new byte[BUFSIZE];
        enviroment_dir = Environment.getExternalStorageDirectory();
        Log.e("path",Environment.getDataDirectory().getAbsolutePath().toString());
        envi_dir = enviroment_dir.getAbsolutePath();
        Log.e("env_dir",envi_dir);
        String path1 = "/data/data/experiment.network.ipv4overipv6/files/tmp/IPFIFO.txt";
        String path2 = "/data/data/experiment.network.ipv4overipv6/files/tmp/FLOWFIFO.txt";
        FIFO = new File(path1);
        FIFO2 = new File(path2);
        ipv4Addr = new String();
        DNSserver = new String[3];
        sock = new String();

        text_download_speed = (TextView)findViewById(R.id.download_speed);
        text_upload_speed = (TextView)findViewById(R.id.upload_speed);
        text_total_download_size = (TextView)findViewById(R.id.total_download_size);
        text_total_upload_size = (TextView)findViewById(R.id.total_upload_size);
        text_total_download_packages = (TextView)findViewById(R.id.total_download_packages);
        text_total_upload_packages = (TextView)findViewById(R.id.total_upload_packages);
        text_total_time = (TextView)findViewById(R.id.total_time);
        text_ipv4Addr= (TextView)findViewById(R.id.ipv4Addr);
        text_ipv6Addr= (TextView)findViewById(R.id.ipv6Addr);

        network_condition= (TextView)findViewById(R.id.network_condition);
        intent = VpnService.prepare(this);

        DecimalFormat df=new DecimalFormat("#.0");

    }

    public void checkNetworkState(){ // check the state of network of the phone
        int times = 0;
        if (!isNetworkConnected(this)) {
            while(true){
                times ++;
                Log.e("times",Integer.toString(times));
                if(times > 5){//好久没有连接，那么放弃
                    Message message = new Message();
                    message.arg1 = NETWORK_CONDITION;
                    message.arg2 = BAD_LINK;
                    handler.sendMessage(message);
                    break;
                }
                Log.e("error", "no network");
                try {
                    Thread.currentThread().sleep(2000);
                }
                catch(Exception e){
                    e.printStackTrace();
                }

                if(isNetworkConnected(this)){
                    Message message = new Message();
                    message.arg1 = NETWORK_CONDITION;
                    message.arg2 = LINKED;
                    handler.sendMessage(message);

                    break;
                }
            }

        }
        else{
            Message message = new Message();
            message.arg1 = NETWORK_CONDITION;
            message.arg2 = LINKED;
            handler.sendMessage(message);
        }
    }


    public boolean isNetworkConnected(Context context) {// judge whether the network is connected\
        Log.e("checknetwork","networkcond");
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
            //Log.e("ipv6address",str);
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
            {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet6Address)
                    {
                        String addr = inetAddress.getHostAddress().toString();
                        Log.e("ipv6address2",addr);
                        if(addr.charAt(0) != 'f')
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
            Log.e("ipv6address",str);
            return str;
        }
        catch (Exception ex)
        {
            Log.e("Wifi", ex.toString());
        }
        Log.e("ipv6Address","no ipv6Address");
        return null;
    }


    protected void onActivityResult(int request, int result, Intent data) {
        if (result==RESULT_OK) {
           Intent intent  = new Intent(this,MyVpnService.class);
            intent.putExtra("ipv4Addr",ipv4Addr);
            intent.putExtra("DNSserver",DNSserver);
            intent.putExtra("socket",sock);
            startService(intent);
        }
    }

    /*读取管道信息*/
    public int read_pipe(File file) throws Exception{
        if(!file.exists()){
            Log.e("FIFO","not exists");
        }
        buf = null;
        buf = new byte[BUFSIZE];
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream in = new BufferedInputStream(fileInputStream);

        int length = in.read(buf);//读取管道
        //Log.e("here",buf.toString());
        //Log.e("lenght",Integer.toString(length));
        in.close();
        return length;
    }

    public void write_pipe(File file,String str) throws Exception{
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        BufferedOutputStream out = new BufferedOutputStream(fileOutputStream);
        out.write(str.getBytes());//读取管道
        //Log.e("buf",Byte.toString(buf[1]));
       // pipe_message = new String(buf, "GB2312");
        out.close();
    }


    /*定时器主要功能为定时刷新显示界面，显示流量信息*/
    /*
    1）开启定时器之前，创建一个读取IP信息管道的全局标志位flag，默认置0；
    2）开始读取管道，首先读取IP信息管道，判断是否有后台传送来的IP等信息；
	3）假如没有，下次循环继续读取；
	4）有IP信息，就启用安卓VPN服务(此部分在第五章附录部分有详细解释)；
	5）把获取到的安卓虚接口描述符写入管道传到后台；
	6）把flag置1，下次循环不再读取该IP信息管道；
	7）读取流量信息管道；
    8）从管道读取后台传来的实时流量信息；
	9）把流量信息进行格式转换；
	10）显示到界面；
	11）界面显示的信息有运行时长、上传和下载速度、上传总流量和包数、下载总流量和包数、下联虚接口V4地址、上联物理接口IPV6地址。
    */

    public void timer(){
        Timer timer = new Timer();//定时器
        Log.e("timer","start");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //Log.e("flag",Integer.toString(flag));
                /*如果flag为0，即要读取IP信息管道*/
                Message mes = new Message();
                mes.arg1 = TIME_UPDATE;
                handler.sendMessage(mes);
                if(flag == 0){
                    try{
                        int length = read_pipe(FIFO);
                        if(length == -1){
                            //Log.e("read_pipe","no message");
                        }
                        else{
                           // Log.e("read_pipe",Integer.toString(length));
                            flag = 1;
                            String message = "";
                            for(int i=0;i<100;++i){
                               char m = (char)buf[i];
                                message += m;
                            }
                            Log.e("message",message);
                            ipv4Addr = message.split(" ")[0];

                            DNSserver[0] = message.split(" ")[2];
                            DNSserver[1] = message.split(" ")[3];
                            DNSserver[2] = message.split(" ")[4];
                            sock = message.split(" ")[1];
                            Log.e("ipv4addr & DNS",ipv4Addr+" "+DNSserver+" "+sock);
                            /*开启安卓VPN服务*/

                            if (intent!=null) {
                                startActivityForResult(intent,0);
                            }
                            else {
                                onActivityResult(0, RESULT_OK, null);
                            }

                            Message m = new Message();
                            m.arg1 = NETWORK1;
                            handler.sendMessage(m);
                        }
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }

                }
                /*如果flag = 1，读取流量信息*/
                else{
                    try {

                        if(read_pipe(FIFO2)!= -1) {
                            String pipe_message = "";
                            for(int i=0;i<100;++i){
                                char m = (char)buf[i];
                                pipe_message += m;
                            }
                            Log.e("flowpipe",pipe_message);
                            total_upload_size = (Integer.parseInt(pipe_message.split(" ")[0]) );

                            upload_speed = Integer.parseInt(pipe_message.split(" ")[1])  ;
                         ;
                            total_download_size =  Integer.parseInt(pipe_message.split(" ")[2]) ;

                            download_speed = Integer.parseInt(pipe_message.split(" ")[3]) ;

                            total_upload_packages = Integer.parseInt(pipe_message.split(" ")[4]);
                            total_download_packages = Integer.parseInt(pipe_message.split(" ")[5]);
                            Log.e("totaldownloadpackages",Integer.toString(total_download_packages));
                        /*需要告知handler*/
                            Message message = new Message();
                            message.arg1 = UPDATE_INFO;
                            handler.sendMessage(message);
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        },1000,1000);
    }


    public void createFIFO() throws Exception{
        String path = "/data/data/experiment.network.ipv4overipv6/files/tmp/";
        File dir = new File(path);
        if(!dir.exists()){
            dir.mkdirs();
        }
        String filename = "IPFIFO.txt";
        File file = new File(dir,filename);
        if(file.exists()){
            file.delete();
        }
        file.createNewFile();
        filename = "FLOWFIFO.txt";
        file = new File(dir,filename);
        if(file.exists()){
            file.delete();
        }
        file.createNewFile();
        filename = "VPNFIFO.txt";
        file = new File(dir,filename);
        if(file.exists()){
            file.delete();
        }
        file.createNewFile();
    }

    public native int setConnection(String dir);

    static {;
        System.loadLibrary("setConnection");

    }
}











