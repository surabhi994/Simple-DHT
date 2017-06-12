package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Map;

import static java.security.AccessController.getContext;

public class SimpleDhtActivity extends Activity {
 Uri muri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(tv, getContentResolver()));

        final Button b1 = (Button) findViewById(R.id.button1);


/*        findViewById(R.id.button2).setOnClickListener(
                MatrixCursor resultCursor = contentResolver.query(uri, null,
                        "*", null, null);
        );*/



        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String key="";
                String val="";
                SharedPreferences sharedPref = getApplication().getSharedPreferences("Your Pref", Context.MODE_PRIVATE);
                Map<String,?> keys = sharedPref.getAll();


                Log.d("Length of LDUMP",Integer.toString(keys.size()));

                for(Map.Entry<String,?> entry : keys.entrySet()){
                    key= entry.getKey() ;
                    val=entry.getValue().toString();
                    TextView localTextView = (TextView) findViewById(R.id.textView1);
                    localTextView.append("\n" + key+ " " + val);
                }
                //TextView localTextView = (TextView) findViewById(R.id.textView1);
                //localTextView.append("\t" + msg);

            }
        });
        final Button b4= (Button) findViewById(R.id.button4);
        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              SimpleDhtProvider obj= new SimpleDhtProvider();
                Uri.Builder uriBuilder = new Uri.Builder();
                uriBuilder.authority("edu.buffalo.cse.cse486586.simpledht.provider");
                uriBuilder.scheme("content");
                muri = uriBuilder.build();
               int res= obj.delete(muri,"@",null);
                Log.d("delete button", String.valueOf(res));

            }

        });



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }

}