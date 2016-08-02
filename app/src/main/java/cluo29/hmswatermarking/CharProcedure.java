package cluo29.hmswatermarking;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Comet on 04/02/16.
 */
public class CharProcedure extends Service implements SensorEventListener {

    public static final String ACTION_SENSOR_WATERMARKING = "ACTION_SENSOR_WATERMARKING";

    public static final String EXTRA_DATA = "data";

    private static SensorManager mSensorManager;
    private static Sensor mAccelerometer;

    //ready for next watering?
    private static boolean ready = true;

    //how many data readings are watered
    private static int water_count = 0;

    //how many container rows are used, this var is not used
    private static int container_count = 0;

    //start time of testing
    private static long start_time = 0;

    //last time of container
    private static long last_time=0;


    //last time of finished water
    private static long last_water_time=0;

    //private static long intervalSum=0;

    //processing time
    //private static long processingSum=0;

    //testing conditions: sensor type: accelerometer
    // duration (consistent to sensing app!) 300
    // sensing frequency:SENSOR_DELAY_NORMAL
    // Bits to use: 1

    //testing duration
    private static long duration = 300;

    //how many bits do we use in a container
    final static int BITS = 1;

    //how many bits of container data are needed?
    //8 for char byte
    private static int bitsOfContainer = 8;

    //how is the watering progress? 0 means nothing done, 8 means done for one reading
    private static int embeddingComplete = 0;

    private static boolean testing = true;

    //binary data to embed
    private static String binaryData;

    private static List<Character> data_to_embed = new ArrayList<Character>();

    //processing times
    private static List<Long> processing_times = new ArrayList<Long>();

    //intervals
    private static List<Long> intervals = new ArrayList<Long>();

    //private static int block_size = 32;

    //1 sec to Nano Sec
    public static int secToNanoSec = 1000000000;

    //1 ms to Nano Sec
    public static int msToNanoSec = 1000000;

    //testing condition label
    public static String label;

    @Override
    public void onCreate() {

        //must do this!!!
        initialization_encryption();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        /** get sensor data as fast as possible */
        //public static final int SENSOR_DELAY_FASTEST = 0;
        /** rate suitable for games */
        //public static final int SENSOR_DELAY_GAME = 1;
        /** rate suitable for the user interface  */
        //public static final int SENSOR_DELAY_UI = 2;
        /** rate (default) suitable for screen orientation changes */
        //public static final int SENSOR_DELAY_NORMAL = 3;
        //from 3 to 0 in testing

        //this app from 3 to 0 in testing
        final int SENSOR_DELAY = 0;

        //the other app, mag from 0 to 3 in testing
        final int Embed_DELAY = 3;

        label="acc "+SENSOR_DELAY + " char "+Embed_DELAY;

        mSensorManager.registerListener(this, mAccelerometer, SENSOR_DELAY);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SENSOR_WATERMARKING);
        registerReceiver(SensorReceiver, filter);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER&&(!ready||!data_to_embed.isEmpty())) //!ready means watermarking ongoing for one reading.
        {
            long startProcessingTime = System.nanoTime();

            //Log.d("SENSORS10", " containers = " + container_count);

            if(start_time == 0)
            {
                start_time = System.nanoTime();

                //the last time we got a sensor reading.
                last_time = start_time;

                Log.d("SENSORS11", "watering starts at = " + start_time);
            }else
            {
                long interval = System.nanoTime() - last_time;

                intervals.add(interval);

                //intervalSum = intervalSum + interval;

                //the last time we got a sensor reading.
                last_time = System.nanoTime();
            }

            if((System.nanoTime() - start_time > duration * secToNanoSec) &&testing)
            {

                testing = false;

                Log.d("SENSORS11", "In " + duration +" seconds, watered readings = " + water_count);

                Log.d("SENSORS11", "In " + duration +" seconds, (RECORD THIS ) avg watering speed (ms) = " + ((last_water_time - start_time)*1.0/ water_count)/msToNanoSec);

                //Log.d("SENSORS10", "In " + duration +" seconds, avg  interval = " + intervalSum*1.0/container_count);

                //Log.d("SENSORS10", "In " + duration +" seconds, avg processing time = " + processingSum*1.0/container_count);

                //Log.d("SENSORS10", "In " + duration +" seconds, containers = " + container_count);

                stats();

                for (int i =0; i<processing_times.size();i++)
                {
                    ContentValues data = new ContentValues();
                    data.put(Provider.Unlock_Monitor_Data.TIMESTAMP, i+1);
                    data.put(Provider.Unlock_Monitor_Data.INTERVAL, -1);
                    data.put(Provider.Unlock_Monitor_Data.PROCESSING, processing_times.get(i));
                    data.put(Provider.Unlock_Monitor_Data.LABEL, label+" pro");
                    getContentResolver().insert(Provider.Unlock_Monitor_Data.CONTENT_URI, data);
                }

                for (int i =0; i<intervals.size();i++)
                {
                    ContentValues data = new ContentValues();
                    data.put(Provider.Unlock_Monitor_Data.TIMESTAMP, i+1);
                    data.put(Provider.Unlock_Monitor_Data.INTERVAL, intervals.get(i));
                    data.put(Provider.Unlock_Monitor_Data.PROCESSING, -1);
                    data.put(Provider.Unlock_Monitor_Data.LABEL, label+" int");
                    getContentResolver().insert(Provider.Unlock_Monitor_Data.CONTENT_URI, data);
                }
                Log.d("SENSORS11", "insert success");

                mSensorManager.unregisterListener(this, mAccelerometer);

            }

            float accelerometer_x = event.values[0];

            //start watering
            if(ready){
                //water a new
                ready = false;

                embeddingComplete = 0;

                char data = data_to_embed.get(0);

                data_to_embed.remove(0);

                binaryData = Encryption(data);

                float result_watermarking = CharWatermarking(binaryData, accelerometer_x, BITS, embeddingComplete);

                embeddingComplete = embeddingComplete + BITS;

                if(embeddingComplete == bitsOfContainer)
                {
                    ready = true;

                    water_count++;

                    last_water_time = System.nanoTime();

                    //Log.d("SENSORS10", " watered readings = " + water_count);
                }
            }
            else
            {
                //continue watering previous
                float result_watermarking = CharWatermarking(binaryData, accelerometer_x, BITS, embeddingComplete);

                embeddingComplete = embeddingComplete + BITS;

                if(embeddingComplete == bitsOfContainer)
                {
                    ready = true;

                    water_count++;

                    last_water_time = System.nanoTime();

                    //Log.d("SENSORS10", " watered readings = " + water_count);
                }

            }

            container_count++;

            long processing_time = System.nanoTime() - startProcessingTime;

            processing_times.add(processing_time);

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
                char data = intent.getExtras().getChar(EXTRA_DATA);

                data_to_embed.add(data);

            }
        }
    }

    private static String CharToBinaryString(char data){ //char (as ascii byte) to put in containers, 8-bit
        int digitChar = data;
        byte byteChar = (byte)digitChar;
        //A byte is 8-bit
        return String.format("%8s", Integer.toBinaryString(byteChar)).replace(' ', '0');
    }


    //do watermarking for char (as ascii byte), container is float, accelerometer
    private static float CharWatermarking(String binaryData,  //data to put in containers
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

//enc related vars

    public static byte[] key = new byte[16];
    public static byte[] iv = new byte[12];
    public static byte[] tag = new byte[16];
    public static Cipher cipher;
    public static Cipher cipher_for_stream;
    public static int encrypted_bits_counter = 0;
    public static byte[] zero_string = new byte[16];
    public static byte[] key_stream = new byte[16];

    public static void initialization_encryption(){
        key = "h8dsnb32husFG3AS".getBytes();
        iv = "L48Vj2a8F8ab".getBytes();

        try {

            // Initialization of the cipher with the key, IV, 128 bit authentication tag size
            // and encryption mode
            cipher = Cipher.getInstance("AES/GCM/NoPadding");

            // Create a new cipher that will be used for getting the key stream. GCM mode should work
            // as stream cipher but the java implementation handles the data in blocks
            // which is not suitable for our scenario. Therefore, we add the key stream to our data ourselves.
            cipher_for_stream = Cipher.getInstance("AES/GCM/NoPadding");

            // Have the same key and the same IV for both of the ciphers, otherwise won't work!
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(tag.length*Byte.SIZE, iv));
            cipher_for_stream.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(tag.length*Byte.SIZE, iv));

        } catch ( InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException |
                InvalidAlgorithmParameterException e){
            //Log.d("ENCRYPTION", "naive encryption");
        }

    }

    public static byte[] finalization_encryption(){
        // Do final application of the first cipher to get authentication tag

        try {
            byte[] final_part = new byte[cipher.getOutputSize(0)];
            cipher.doFinal(final_part,0);

            // Get the tag from the output
            for (int i = 0; i < tag.length; i++) {
                tag[i] = final_part[final_part.length-tag.length+i];
            }

            // So, in this case encrypted_string and the tag would be embedded

            //System.out.println("Encrypted length and final part length: "+encrypted_binary_string.length()/8+" "+final_part.length);
        } catch (  IllegalBlockSizeException | ShortBufferException |
                BadPaddingException e){
            Log.d("SENSORS10", "bad encry");
        }
        return tag;
    }

    //index of key stream to use
    public static int key_index;

    public static String Encryption(char Data) { //binaryData for encryption

        String encrypted_binary_string="";

        byte[] input_string = new byte[1];

        input_string[0] = (byte)Data;

        try {

            //Log.d("SENSORS10", "ENCRYPTION");

            if(encrypted_bits_counter == 0)
            {

                cipher_for_stream.update(zero_string, 0, 16, key_stream, 0);

            }

            // Encrypted data will be stored here
            byte[] encrypted_string = new byte[input_string.length];

            // This won't be used, since we will be manually adding the key stream

            byte[] throw_out = new byte[16];
            int l = cipher.update(input_string, 0, input_string.length, throw_out, 0);

            //Log.d("SENSORS10", "Lengths of input and output of cipher: " + input_string.length + " " + l);

            // Do manual addition of the key stream
            //debug here , chu

            key_index = encrypted_bits_counter/8;

            for (int i = 0; i < input_string.length; i++) {

                encrypted_string[i] = (byte) ((byte) input_string[i] ^ key_stream[i + key_index]);
            }

            encrypted_bits_counter += 8;

            if(encrypted_bits_counter == 128) //128,   32*4
            {
                encrypted_bits_counter = 0;
            }

            for (int i = 0; i < 1; i++){
                String temp = String.format("%8s", Integer.toBinaryString(encrypted_string[i] & 0xFF)).replace(' ', '0');
                encrypted_binary_string = encrypted_binary_string +temp;
            }


        } catch ( ShortBufferException e){
            //Log.d("SENSORS11", "bad encry");
            //Log.d("SENSORS10", ""+e);
        }

        //Log.d("SENSORS11", "good encry");

        return encrypted_binary_string;

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

    public static double meanTime(List<Long> numbers){
        double result=0;

        double sum = 0;

        for (int i =0; i<numbers.size();i++)
        {
            sum+=numbers.get(i);
        }

        result = sum/numbers.size();

        Log.d("SENSORS11", "mean (ms) = " + result/msToNanoSec);

        return result;
    }


    public static long maxTime(List<Long> numbers){
        long max=-1;

        for (int i =0; i<numbers.size();i++)
        {
            if(max<numbers.get(i))
            {
                max = numbers.get(i);
            }
        }

        Log.d("SENSORS11", "max (ms) = " + max/msToNanoSec);

        return max;
    }

    public static long minTime(List<Long> numbers){
        long min=numbers.get(0);

        for (int i =0; i<numbers.size();i++)
        {
            if(min>numbers.get(i))
            {
                min = numbers.get(i);
            }
        }

        Log.d("SENSORS11", "min (ms) = " + min/msToNanoSec);

        return min;
    }

    public static double stdTime(List<Long> numbers){
        double result=0;
        double mean = meanTime(numbers);

        double sum = 0;  //sum of (x_i - u)^2

        for (int i =0; i<numbers.size();i++)
        {
            sum+=  Math.pow((numbers.get(i) - mean),2);
        }

        result=Math.sqrt(sum/numbers.size());

        Log.d("SENSORS11", "stdTime (ms) = " + result/msToNanoSec);

        return result;
    }

    public static void stats (){

        //processing times
        //private static List<Long> processing_times = new ArrayList<Long>();

        //intervals
        //private static List<Long> intervals = new ArrayList<Long>();

        Log.d("SENSORS11", "Interval Stat: ");

        meanTime(intervals);

        maxTime(intervals);

        minTime(intervals);

        stdTime(intervals);

        Log.d("SENSORS11", "Processing Time Stat: ");

        meanTime(processing_times);

        maxTime(processing_times);

        minTime(processing_times);

        stdTime(processing_times);

        //if this works, don't need sums

        //Log.d("SENSORS11", "In " + duration +" seconds, avg interval = " + intervalSum*1.0/container_count);

        //Log.d("SENSORS11", "In " + duration +" seconds, avg processing time = " + processingSum*1.0/container_count);
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