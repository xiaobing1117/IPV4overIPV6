package experiment.network.ipv4overipv6;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //System.loadLibrary("hellojni");
        TextView t = new TextView(this);
        setContentView(R.layout.activity_main);
        t = (TextView) findViewById(R.id.tet);
        t.setText(stringFromJNI());
        if (!isNetworkConnected(this)){
            Log.e("error","no network");
        }
        else
            Log.e("error","has network");
        Intent intent = VpnService.prepare(this);
        if(intent != null){
            startActivityForResult(intent,0);
        }
        else{
            onActivityResult(0,RESULT_OK,null);
        }
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

    public void setipv6socket(){
        final String ipv6_str = "2402:f000:1:4417::900";

    }

    protected void onActivityResult(int request,int result, Intent data){
        if(result==RESULT_OK){
            Intent intent = new Intent(this,MyVpnService.class);



            startService(intent);
        }
    }
    public native String stringFromJNI();
    static{
        System.loadLibrary("hellojni");
    }

}
