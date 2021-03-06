
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
 * Created by Comet on 01/02/16.
 */

//testing conditions: sensor type: accelerometer
// duration (consistent to sensing app!) 300
// sensing frequency: from 3 SENSOR_DELAY_NORMAL to 0 SENSOR_DELAY_FASTEST
// Bits to use: 1


//This service receives intent and waters marking.

public class ThreeFloatCPUTest extends Service implements SensorEventListener {

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

    //private static long intervalSum=0;

    //processing time
    //private static long processingSum=0;



    //testing duration
    private static long duration = 300;

    //how many bits do we use in a container
    final static int BITS = 1;

    //how many bits of container data are needed?
    //32 bits float
    private static int bitsOfContainer = 32;

    //how is the watering progress? 0 means nothing done, 32 means done for one reading
    private static int embeddingComplete = 0;

    private static boolean testing = true;

    //binary data to embed
    private static String binaryData_x;

    //binary data to embed
    private static String binaryData_y;

    //binary data to embed
    private static String binaryData_z;


    private static List<ContentValues> data_to_embed = new ArrayList<ContentValues>();

    //processing times
    private static List<Long> processing_times = new ArrayList<Long>();

    //intervals
    private static List<Long> intervals = new ArrayList<Long>();

    private static int block_size = 32;

    //1 sec to Nano Sec
    public static int secToNanoSec = 1000000000;

    //1 ms to Nano Sec
    public static int msToNanoSec = 1000000;

    //testing condition label
    public static String label;

    @Override
    public void onCreate() {

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
        final int Embed_DELAY = 0;

        label="acc "+SENSOR_DELAY + " mag "+Embed_DELAY;

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

            //Log.d("SENSORS11", " processing");

            float accelerometer_x = event.values[0];
            float accelerometer_y = event.values[1];
            float accelerometer_z = event.values[2];

            //start watering
            if(ready){
                //water a new
                ready = false;

                embeddingComplete = 0;

                ContentValues data = data_to_embed.get(0);

                data_to_embed.remove(0);

                //no encryption

                //binaryData_x = FloatToBinaryString(data.getAsFloat("float_x"));

                //binaryData_y = FloatToBinaryString(data.getAsFloat("float_y"));

                //binaryData_z = FloatToBinaryString(data.getAsFloat("float_z"));

                //encryption

                binaryData_x = Encryption(data.getAsFloat("float_x"));

                binaryData_y = Encryption(data.getAsFloat("float_y"));

                binaryData_z = Encryption(data.getAsFloat("float_z"));

                float result_watermarking_x = FloatWatermarking(binaryData_x, accelerometer_x, BITS, embeddingComplete);

                float result_watermarking_y = FloatWatermarking(binaryData_y, accelerometer_y, BITS, embeddingComplete);

                float result_watermarking_z = FloatWatermarking(binaryData_z, accelerometer_z, BITS, embeddingComplete);

                embeddingComplete = embeddingComplete + BITS;

                if(embeddingComplete == bitsOfContainer)
                {
                    ready = true;

                    //Log.d("SENSORS10", " watered readings = " + water_count);
                }
            }
            else
            {
                //continue watering previous

                float result_watermarking_x = FloatWatermarking(binaryData_x, accelerometer_x, BITS, embeddingComplete);

                float result_watermarking_y = FloatWatermarking(binaryData_y, accelerometer_y, BITS, embeddingComplete);

                float result_watermarking_z = FloatWatermarking(binaryData_z, accelerometer_z, BITS, embeddingComplete);

                embeddingComplete = embeddingComplete + BITS;

                if(embeddingComplete == bitsOfContainer)
                {
                    ready = true;

                    //Log.d("SENSORS10", " watered readings = " + water_count);
                }

            }

        }
    }

    //receive sensor data to embed and store in an arrayList
    private ContextReceiver SensorReceiver = new ContextReceiver();
    public class ContextReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_SENSOR_WATERMARKING))
            {
                ContentValues data = (ContentValues) intent.getParcelableExtra(EXTRA_DATA);

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

    public static String Encryption(float Data) { //binaryData for encryption

        String encrypted_binary_string="";

        byte[] input_string = ByteBuffer.allocate(4).putFloat(Data).array();

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

            if(encrypted_bits_counter == 0) {

                key_index = 0;

            }
            else if(encrypted_bits_counter == 96){ //after 32*3, next float to do

                key_index = 4;

            }
            else if(encrypted_bits_counter == 192){ //after 32*3*2, next float to do

                key_index = 8;
            }
            else if(encrypted_bits_counter == 288){ //after 32*3*3, next float to do

                key_index = 12;
            }

            for (int i = 0; i < input_string.length; i++) {

                encrypted_string[i] = (byte) ((byte) input_string[i] ^ key_stream[i + key_index]);
            }



            encrypted_bits_counter += 32;

            if(encrypted_bits_counter == 384) //128*3,   32*4*3
            {
                encrypted_bits_counter = 0;
            }

            for (int i = 0; i < 4; i++){
                String temp = String.format("%8s", Integer.toBinaryString(encrypted_string[i] & 0xFF)).replace(' ', '0');
                encrypted_binary_string = encrypted_binary_string +temp;
            }

        } catch ( ShortBufferException e){
            Log.d("SENSORS10", "bad encry");
            //Log.d("SENSORS10", ""+e);
        }
        //Log.d("SENSORS10", "good encry");

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