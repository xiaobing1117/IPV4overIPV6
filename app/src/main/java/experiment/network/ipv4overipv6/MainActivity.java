package experiment.network.ipv4overipv6;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //System.loadLibrary("hellojni");
        TextView t = new TextView(this);
        setContentView(R.layout.activity_main);
        t = (TextView)findViewById(R.id.tet);
        t.setText(stringFromJNI());

    }


    public native String stringFromJNI();
    static{
        System.loadLibrary("hellojni");
    }

}
