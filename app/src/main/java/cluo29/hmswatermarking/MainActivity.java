package cluo29.hmswatermarking;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;




public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //int 32 bit, one dimension
        //startService(new Intent(MainActivity.this, Procedure.class));

        //startService(new Intent(MainActivity.this, ThreeDoubleProcedure.class));

        //startService(new Intent(MainActivity.this, CharProcedure.class));

        //startService(new Intent(MainActivity.this, FloatProcedure.class));

        //float 32 bit, 3 dimensions
        startService(new Intent(MainActivity.this, ThreeFloatProcedure.class));

        //startService(new Intent(MainActivity.this, ThreeFloatProcedure2Bits.class));

        //encryption test

        //startService(new Intent(MainActivity.this, Encyption.class));

        //startService(new Intent(MainActivity.this, DeviceIDProcedure.class));

        //startService(new Intent(MainActivity.this, ThreeFloatCPUTest.class));

        //startService(new Intent(MainActivity.this, ThreeFloatComparison.class));

        //startService(new Intent(MainActivity.this, ThreeFloatComparison2Bits.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
