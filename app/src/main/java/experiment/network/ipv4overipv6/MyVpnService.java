package experiment.network.ipv4overipv6;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.net.VpnService.Builder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import android.os.Environment;
import java.net.Inet4Address;
import java.net.InetAddress;

public class MyVpnService extends VpnService
{
   // File file = Environment.getExternalStorageDirectory();

    public String ipv4Addr;
    public String[] DNSserver;
    public String sock;
    public void writePipe(File file,int arr) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        OutputStreamWriter out=new OutputStreamWriter(fileOutputStream);
        out.write(arr);

        out.close();
    }

    /*代码基本按照http://blog.csdn.net/Roland_Sun/article/details/46337171#reply所写*/

    @Override
    public void onCreate(){
        Log.e("create","here");
    }
    /*
    public void start(String addr, String[] DNS, String socks){
        ipv4Addr = addr;
        DNSserver = DNS;
        sock = socks;
        onStartCommand();
    }
    */
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        /*得到传来的内容*/
        Log.e("VPNservice","started");

        String str = "/data/data/experiment.network.ipv4overipv6/files/tmp";
        String path = str + "/VPNFIFO.txt";
        File FIFO = new File(path);

        ipv4Addr = intent.getStringExtra("ipv4Addr");
        DNSserver = intent.getStringArrayExtra("DNSserver");
        sock = intent.getStringExtra("socket");

        /*根据后台传来的信息启动VPN，把安卓虚接口描述符写入管道传到后台*/
        Builder builder=new Builder();
        builder.setMtu(1024);//即表示虚拟网络端口的最大传输单元，如果发送的包长度超过这个数字，则会被分包
        builder.addAddress(ipv4Addr, 32);//虚拟网络端口的 IP 地址
        builder.addRoute("0.0.0.0", 0);//将所有的 IP 包都通过 NAT 转发到虚拟端口上去
        builder.addDnsServer(DNSserver[0]);//该端口的 DNS 服务器地址

     //   builder.addDnsServer(DNSserver[1]);
   //     builder.addDnsServer(DNSserver[2]);
        builder.addSearchDomain("");
        builder.setSession("MyVpn");//要建立的 VPN 连接的名字
        ParcelFileDescriptor interFace =builder.establish();//tun0 虚拟网络接口就建立完成了
        int fileds=interFace.getFd();//通过 getFD()函数就能把 tun0 的文件描述符拿出来
    //    protect(Integer.parseInt(sock));//对socket进行保护，只有它不走虚接口。为了防止发送的数据包再被转到TUN虚拟网络设备上
        String des=fileds + "";
        Log.e("data",des);
        char[] array = des.toCharArray();
        try {
            writePipe(FIFO, fileds);//写入fifo文件
        } catch (IOException e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }
}
