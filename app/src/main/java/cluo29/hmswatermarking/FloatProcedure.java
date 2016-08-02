package cluo29.hmswatermarking;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Comet on 01/02/16.
 */

//This service receives intent and waters marking.

public class FloatProcedure extends Service implements SensorEventListener {

    public static final String ACTION_SENSOR_WATERMARKING = "ACTION_SENSOR_WATERMARKING";

    public static final String EXTRA_DATA = "data";

    private static SensorManager mSensorManager;
    private static Sensor mAccelerometer;


    //ready for next watering?
    private static boolean ready = true;

    //how many data readings are watered
    private static int water_count = 0;

    //how many container rows are used
    private static int container_count = 0;

    //start time of testing
    private static long start_time = 0;

    //last time of container
    private static long last_time=0;


    //last time of finished water
    private static long last_water_time=0;

    private static long intervalSum=0;

    //processing time
    private static long processingSum=0;

    //testing conditions: sensor type: accelerometer
    // duration (consistent to sensing app!) 300
    // sensing frequency:SENSOR_DELAY_NORMAL
    // Bits to use: 1

    //testing duration
    private static int duration = 300;

    //how many bits do we use in a container
    final static int BITS = 1;

    //how many bits of container data are needed?
    //32 bits float
    private static int bitsOfContainer = 32;

    //how is the watering progress? 0 means nothing done, 32 means done for one reading
    private static int embeddingComplete = 0;


    private static boolean testing = true;

    //binary data to embed
    private static String binaryData;

    private static List<Float> data_to_embed = new ArrayList<Float>();

    @Override
    public void onCreate() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SENSOR_WATERMARKING);
        registerReceiver(SensorReceiver, filter);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER&&(!ready||!data_to_embed.isEmpty())) //!ready means watermarking ongoing for one reading.
        {
            long startProcessingTime = System.currentTimeMillis();


            //Log.d("SENSORS10", " containers = " + container_count);

            if(start_time == 0)
            {
                start_time = System.currentTimeMillis();

                last_time = start_time;

                Log.d("SENSORS10", "watering starts at = " + start_time);
            }else
            {
                intervalSum = intervalSum - last_time + System.currentTimeMillis();

                last_time = System.currentTimeMillis();
            }

            if(System.currentTimeMillis() - start_time > duration * 1000 &&testing)
            {
                testing = false;

                Log.d("SENSORS10", "In " + duration +" seconds, watered readings = " + water_count);

                Log.d("SENSORS10", "In " + duration +" seconds, avg watering speed = " + (last_water_time - start_time)*1.0/ water_count);

                Log.d("SENSORS10", "In " + duration +" seconds, avg interval = " + intervalSum*1.0/container_count);

                Log.d("SENSORS10", "In " + duration +" seconds, avg processing time = " + processingSum*1.0/container_count);

                //Log.d("SENSORS10", "In " + duration +" seconds, containers = " + container_count);
            }

            float accelerometer_x = event.values[0];

            //start watering
            if(ready){
                //water a new
                ready = false;

                embeddingComplete = 0;

                float data = data_to_embed.get(0);

                data_to_embed.remove(0);

                binaryData = FloatToBinaryString(data);

                float result_watermarking = FloatWatermarking(binaryData, accelerometer_x, BITS, embeddingComplete);

                embeddingComplete = embeddingComplete + BITS;

                if(embeddingComplete == bitsOfContainer)
                {
                    ready = true;

                    water_count++;

                    last_water_time = System.currentTimeMillis();

                    //Log.d("SENSORS10", " watered readings = " + water_count);
                }
            }
            else
            {
                //continue watering previous
                float result_watermarking = FloatWatermarking(binaryData, accelerometer_x, BITS, embeddingComplete);

                embeddingComplete = embeddingComplete + BITS;

                if(embeddingComplete == bitsOfContainer)
                {
                    ready = true;

                    water_count++;

                    last_water_time = System.currentTimeMillis();

                    //Log.d("SENSORS10", " watered readings = " + water_count);
                }

            }


            container_count++;

            processingSum= processingSum - startProcessingTime + System.currentTimeMillis();

            //Log.d("SENSORS10", "processing time = "+ (System.currentTimeMillis() - startProcessingTime));

        }
    }

    //receive sensor data to embed and store in an arrayList
    private ContextReceiver SensorReceiver = new ContextReceiver();
    public class ContextReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_SENSOR_WATERMARKING))
            {
                float data = intent.getExtras().getFloat(EXTRA_DATA);

                data_to_embed.add(data);

            }
        }
    }

    private static String FloatToBinaryString(float data){ //float data to put in containers, 32-bit
        int intData = Float.floatToIntBits(data);
        return String.format("%32s", Integer.toBinaryString(intData)).replace(' ', '0');
    }


    //do watermarking for float, container is float, accelerometer
    private static float FloatWatermarking(String binaryData,  //String binaryData to put in containers
                                           float container,// the container, in our testing it is accelerometer
                                           int bitsToUse, //how many bits to use in a row of container, except the flag
                                           int embeddedBitsCounter) //counter of how many bits are embedded already. Remember to add bitsToUse afterwards!!!
    {
        //A float is 32-bit. So use an int to store its binary.
        int intContainer = Float.floatToIntBits(container);
        String binaryContainer = String.format("%32s", Integer.toBinaryString(intContainer)).replace(' ', '0');
        char[] charContainer = binaryContainer.toCharArray();

        //put flag bit to one. This means this row of container data is used for watermarking.
        charContainer[Float.SIZE-1] = '1';

        for (int i = 0; i < bitsToUse;i++){
            charContainer[Float.SIZE-2-i] = binaryData.charAt(embeddedBitsCounter);
            embeddedBitsCounter++;
        }
        float watermarkingResult;
        //if the container float number is negative, do some actions
        if(charContainer[0] == '1')
        {
            charContainer[0] = '0';
            binaryContainer = String.valueOf(charContainer);
            intContainer = Integer.parseInt(binaryContainer,2);
            watermarkingResult = -Float.intBitsToFloat(intContainer);
        }
        else
        {
            binaryContainer = String.valueOf(charContainer);
            intContainer = Integer.parseInt(binaryContainer,2);
            watermarkingResult = Float.intBitsToFloat(intContainer);
        }
        return watermarkingResult;
    }


    private static String DoubleToBinaryString(double data){ //double data to put in containers, 64-bit
        long longData = Double.doubleToLongBits(data);
        //A long is 64-bit
        return String.format("%64s", Long.toBinaryString(longData)).replace(' ', '0');
    }

    //do watermarking for float, container is float, accelerometer
    private static float DoubleWatermarking(String binaryData,  //data to put in containers
                                            float container,// the container, in our testing it is accelerometer
                                            int bitsToUse, //how many bits to use in a row of container, except the flag
                                            int embeddedBitsCounter) //counter of how many bits are embedded already. Remember to add bitsToUse afterwards!!!
    {
        //A float is 32-bit. So use an int to store its binary.
        int intContainer = Float.floatToIntBits(container);
        String binaryContainer = String.format("%32s", Integer.toBinaryString(intContainer)).replace(' ', '0');
        char[] charContainer = binaryContainer.toCharArray();

        //put flag bit to one. This means this row of container data is used for watermarking.
        charContainer[Float.SIZE-1] = '1';

        for (int i = 0; i < bitsToUse;i++){
            charContainer[Float.SIZE-2-i] = binaryData.charAt(embeddedBitsCounter);
            embeddedBitsCounter++;
        }
        float watermarkingResult;
        //if the container float number is negative, do some actions
        if(charContainer[0] == '1')
        {
            charContainer[0] = '0';
            binaryContainer = String.valueOf(charContainer);
            intContainer = Integer.parseInt(binaryContainer,2);
            watermarkingResult = -Float.intBitsToFloat(intContainer);
        }
        else
        {
            binaryContainer = String.valueOf(charContainer);
            intContainer = Integer.parseInt(binaryContainer,2);
            watermarkingResult = Float.intBitsToFloat(intContainer);
        }
        return watermarkingResult;
    }




    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {

    }

}

/*
In this example, the default data delay (SENSOR_DELAY_NORMAL) is specified when the registerListener() method is invoked.
 The data delay (or sampling rate) controls the interval at which sensor events are sent to your application
 via the onSensorChanged() callback method. The default data delay is suitable for monitoring typical screen orientation
 changes and uses a delay of 200,000 microseconds. You can specify other data delays,
  such as SENSOR_DELAY_GAME (20,000 microsecond delay), SENSOR_DELAY_UI (60,000 microsecond delay),
   or SENSOR_DELAY_FASTEST (0 microsecond delay). As of Android 3.0 (API Level 11) you can also
    specify the delay as an absolute value (in microseconds).

The delay that you specify is only a suggested delay. The Android system and other applications
 can alter this delay. As a best practice, you should specify the largest delay that you can
  because the system typically uses a smaller delay than the one you specify (that is, you should
  choose the slowest sampling rate that still meets the needs of your application). Using a larger
   delay imposes a lower load on the processor and therefore uses less power.

 */