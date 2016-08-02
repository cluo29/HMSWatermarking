package cluo29.hmswatermarking;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
//import javax.crypto.Cipher.*;

/**
 *
 * @author afylakis
 */
public class Encyption extends Service {


    @Override
    public void onCreate() {

        try {
            test();
        } catch ( InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException |
                InvalidAlgorithmParameterException | IllegalBlockSizeException | ShortBufferException |
                BadPaddingException e){
            Log.d("SENSORS10", "bad encry");
        }

    }

    public static void test() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, ShortBufferException, BadPaddingException {
        // TODO Auto-generated method stub
        Log.d("SENSORS10", "start encry");

        // We are using only 128 bit keys here for testing. By default, at least Oracle java does
        int testingmode = 1;
        // not seem to allow stronger encryption by default (for some strange reason?).
        byte[] key = new byte[16];
        byte[] iv = new byte[12];

        // Authentication tag will be saved here
        byte[] tag = new byte[16];

        // Just some testing key and IV
        // Note that in practice these have to be generated securely (e.g. with SecureRandom)
        key = "h8dsnb32husFG3AS".getBytes();
        iv = "L48Vj2a8F8ab".getBytes();

        // Encrypt mode, 2 = decrypt
        int mode = 1;


        // Initialization of the cipher with the key, IV, 128 bit authentication tag size
        // and encryption mode
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(tag.length*Byte.SIZE, iv));

        // Encrypting parts, note that the Cipher.update() method handles the data in chunks
        // based on the tag size (?) Therefore, we will be operating in 16*8 bit chunks.
        byte[] input_string = new byte[16] ;
        long startProcessingTime = System.currentTimeMillis();
        if (testingmode ==1){
            //String encry
            //String input = "000000000000000a"
            //input_string = input.getBytes(Charset.forName("UTF-8"));

            /*
            //Int encry
            int input =1;
            byte[] temp1 = new byte[12];
            byte[] temp2 = ByteBuffer.allocate(4).putInt(input).array();
            System.arraycopy(temp1, 0, input_string, 0, temp1.length);
            System.arraycopy(temp2, 0, input_string, temp1.length, temp2.length);
            */

            /*
            //float encry
            float input = 1.5f;
            byte[] temp1 = new byte[12];
            byte[] temp2 = ByteBuffer.allocate(4).putFloat(input).array();
            System.arraycopy(temp1, 0, input_string, 0, temp1.length);
            System.arraycopy(temp2, 0, input_string, temp1.length, temp2.length);
            */


            /*
            //char encry
            char input = 'a';
            byte[] temp1 = new byte[14];
            byte[] temp2 = ByteBuffer.allocate(2).putChar(input).array();
            System.arraycopy(temp1, 0, input_string, 0, temp1.length);
            System.arraycopy(temp2, 0, input_string, temp1.length, temp2.length);

            //A byte is 8-bit
            Log.d("SENSORS10", " char =  " + String.format("%8s", Integer.toBinaryString(temp2[0] & 0xFF)).replace(' ', '0'));

            Log.d("SENSORS10", " char =  " + String.format("%8s", Integer.toBinaryString(temp2[1] & 0xFF)).replace(' ', '0'));
            */




        }
        else{
            //16B
            String bigstring = "01101000011010000110100001101000011010000110100001101000011010000110100001101000011010000110100001101000011010000110100001101000";
            //String bigstring = "01101000";
            input_string = new BigInteger(bigstring, 2).toByteArray();
        }


        Log.d("SENSORS10", "input = "+ input_string);

        // Put the data in here
        byte[] encrypted_string = new byte[input_string.length];

        int l = cipher.update(input_string, 0, input_string.length, encrypted_string, 0);

        Log.d("SENSORS10", "input size = " + input_string.length + " " + l);

        // Do final application to get authentication tag
        cipher.doFinal(tag, 0);

        Log.d("SENSORS10", "time used = " + (System.currentTimeMillis() - startProcessingTime));

        Log.d("SENSORS10", "tag = "+tag);



        // DECRYPTING PART BEGINGS
        int offset;
        cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(tag.length*Byte.SIZE, iv));

        byte[] decrypted_string = new byte[input_string.length];
        offset = cipher.update(encrypted_string, 0, encrypted_string.length, decrypted_string, 0);

        cipher.update(tag, 0, tag.length, decrypted_string, offset);
        cipher.doFinal(decrypted_string, offset);



        if(testingmode == 1){
            String outputstr = new String(decrypted_string, Charset.forName("UTF-8"));
            Log.d("SENSORS10", "outputstr = " + outputstr);
            for(int i=0; i<16; i++) {
                String s1 = String.format("%8s", Integer.toBinaryString(decrypted_string[i] & 0xFF)).replace(' ', '0');
                Log.d("SENSORS10", "outputstr = " + s1);
            }
        }
        else{
            String decrypted_binarystring = "";
            for (int i = 0; i < 16; i++){
                String temp = String.format("%8s", Integer.toBinaryString(decrypted_string[0] & 0xFF)).replace(' ', '0');
                decrypted_binarystring = decrypted_binarystring +temp;
            }
            System.out.println(decrypted_binarystring);
        }



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