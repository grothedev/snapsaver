package org.grothedev.snapsaver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.grothedev.snapsaver.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;


import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	//this code is really messy. i plan to clean it up eventually
	
	File extStorageFile = Environment.getExternalStorageDirectory(); 
	String extStoragePath = extStorageFile.getPath();
	File savedsnaps = new File(extStorageFile + "/savedsnaps");
	Button ready;
	CheckBox checkPics;
	CheckBox checkVids;
	CheckBox checkStories;
	private Timer timer;
	boolean savePics, saveVids, saveStories;
	
	boolean root;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		checkRoot(); 
		
		ready = new Button(this); //this button will start the timer
		ready = (Button)findViewById(R.id.button1);
		ready.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				savePics = checkPics.isChecked();
				saveVids = checkVids.isChecked();
				saveStories = false;
				
				copyFiles();
				
			}
		});
		
		timer = new Timer();
		
		
		
		//set up checkboxes
		checkPics = new CheckBox(this);
		checkPics = (CheckBox) findViewById(R.id.checkPics);
		checkVids = new CheckBox(this);
		checkVids = (CheckBox) findViewById(R.id.checkVids);
		checkStories = new CheckBox(this);
		checkStories = (CheckBox) findViewById(R.id.checkStories);
		checkStories.setClickable(false); //until i figure out how to decrypt stories
	}

	private void checkRoot() {
		Process a;
		try {
			a = Runtime.getRuntime().exec("su");
			root = true;
		} catch (IOException e) {
			root = false;
		} 
		
		TextView rootText = (TextView) findViewById(R.id.checkRoot);
		if (root){
			rootText.setText("Your device appears to be rooted. You have the option to save pictures, videos, and stories.");
			
		} else {
			rootText.setText("Your device appears to not be rooted. You will only be able to save videos and stories.");
			checkPics.setClickable(false);
		}
	}

	private void startTimer(int time){
		timer.schedule(new TimerTask(){

			@Override
			public void run() {
				timerMethod();
			}
			
		}, time);
	}
	
	private void timerMethod(){
		this.runOnUiThread(timerTick);
	}
	private Runnable timerTick = new Runnable(){
		@Override
		public void run() {
			renameAndDecrypt();
			toastMessage("now starting decryption", 1);
		}
	};

	@Override
	public void onBackPressed(){
		System.exit(0);
	}
	
	
	private void copyFiles(){
		Process p; //root shell here, thanks to reddit user wchill
		
		try {
			p = Runtime.getRuntime().exec("su");
			toastMessage("copying snaps", 2);
			PrintStream stdin = new PrintStream(p.getOutputStream()); 
			stdin.println("mount -o remount,rw -t yaffs2 /dev/block/mtdblock3 /system");
			stdin.println("mkdir " + extStoragePath + "/savedsnaps");
			stdin.println("mkdir " + extStoragePath + "/savedsnaps/pics");
			stdin.println("mkdir " + extStoragePath + "/savedsnaps/vids");
			stdin.println("mkdir " + extStoragePath + "/savedsnaps/stories");
			stdin.println("mkdir " + extStoragePath + "/savedsnaps/stories/pics");
			stdin.println("mkdir " + extStoragePath + "/savedsnaps/stories/vids");
			stdin.println("chmod -R 777 /data/data/com.snapchat.android/cache/received_image_snaps ");
			
			if (savePics){
				stdin.println("cp -r /data/data/com.snapchat.android/cache/received_image_snaps/* "
									+ extStoragePath + "/savedsnaps/pics");
			}
			
			if (saveVids){
				stdin.println("cp -r /storage/sdcard0/Android/data/com.snapchat.android/cache/received_video_snaps/* "
						+ extStoragePath + "/savedsnaps/vids");
			}
			
			if (saveStories){
				stdin.println("cp -r /storage/sdcard0/Android/data/com.snapchat.android/cache/stories/received/image/* "
						+ extStoragePath + "/savedsnaps/stories/pics");
				
				stdin.println("cp -r /storage/sdcard0/Android/data/com.snapchat.android/cache/stories/received/video/* "
						+ extStoragePath + "/savedsnaps/stories/vids");
			}
			
			stdin.println("mount -o remount,ro -t yaffs2 /dev/block/mtdblock3 /system");
			
			
		} catch (IOException e) {
			
			if (saveVids){
				toastMessage("copying snaps", 2);
				copyVids();
			}
			
			if (saveStories){
				toastMessage("copying snaps", 2);
				copyStories();
			}
			
			
		} //end root shell
		
		if (saveVids){
			startTimer(new File(extStorageFile, "Android/data/com.snapchat.android/cache/received_video_snaps").list().length * 1500 + 2000); //1000 ms for each video so they are done copying by the time they are decrypted
		} else startTimer(8000);
		
	}
	
	private void copyVids() {
		File vidsDir = new File(extStoragePath + "/Android/data/com.snapchat.android/cache/received_video_snaps/");
		File[] vids = vidsDir.listFiles();
		
		File savedVidsDir = new File(extStorageFile + "/savedsnaps/vids");
		
		if (vids != null){ //check if there are any vids to copy
			if (savedVidsDir.exists()){ //check if savedVidsDir folder exists
				for (int i = 0; i < vids.length; i++){
					try {
						copy(vids[i], new File(extStoragePath + "/savedsnaps/vids/" + vids[i].getName()));
					} catch (IOException e) {
						
						e.printStackTrace();
					}
				}
			} else {
				savedVidsDir.mkdirs();
				for (int i = 0; i < vids.length; i++){
					try {
						copy(vids[i], new File(extStoragePath + "/savedsnaps/vids/" + vids[i].getName()));
					} catch (IOException e) {
						
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void copyStories() {
		File storiesDirImg = new File(extStoragePath + "/Android/data/com.snapchat.android/cache/stories/received/image/");
		File storiesDirVid = new File(extStoragePath + "/Android/data/com.snapchat.android/cache/stories/received/video/");
		File[] images = storiesDirImg.listFiles();
		File[] videos = storiesDirVid.listFiles();
		
		File savedVidsDir = new File(extStorageFile + "/savedsnaps/stories/vids/");
		File savedPicsDir = new File(extStorageFile + "/savedsnaps/stories/pics/");
		
		if (images != null){
			if (savedPicsDir.exists()){ 
				for (int i = 0; i < images.length; i++){
					try {
						copy(images[i], new File(extStoragePath + "/savedsnaps/stories/pics/" + images[i].getName()));
					} catch (IOException e) {
						
						e.printStackTrace();
					}
				}
			} else {
				savedPicsDir.mkdirs();
				for (int i = 0; i < images.length; i++){
					try {
						copy(images[i], new File(extStoragePath + "/savedsnaps/stories/pics/" + images[i].getName()));
					} catch (IOException e) {
						
						e.printStackTrace();
					}
				}
			}
		}
		
		if (videos != null){ 
			if (savedVidsDir.exists()){ 
				for (int i = 0; i < images.length; i++){
					try {
						copy(videos[i], new File(extStoragePath + "/savedsnaps/stories/vids/" + images[i].getName()));
					} catch (IOException e) {
						
						e.printStackTrace();
					}
				}
			} else {
				savedVidsDir.mkdirs();
				for (int i = 0; i < images.length; i++){
					try {
						copy(videos[i], new File(extStoragePath + "/savedsnaps/stories/vids/" + images[i].getName()));
					} catch (IOException e) {
						
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void copy(File src, File dst) throws IOException { //method taken from http://stackoverflow.com/questions/9292954/how-to-make-a-copy-of-a-file-in-android
	    InputStream in = new FileInputStream(src);
	    OutputStream out = new FileOutputStream(dst);

	    // Transfer bytes from in to out
	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = in.read(buf)) > 0) {
	        out.write(buf, 0, len);
	    }
	    in.close();
	    out.close();
	}
	
	private void renameAndDecrypt(){
		Random r = new Random(); //random int in filename to prevent overwriting
		File[] pics = new File(savedsnaps, "pics").listFiles();
		File[] vids = new File(savedsnaps, "vids").listFiles();
		File[] storiesPics = new File(savedsnaps, "stories/pics").listFiles();
		File[] storiesVids = new File(savedsnaps, "stories/vids").listFiles();
		
		if (savePics){
			removeNoMediaAndDecrypt(pics, false);
		}
		if (saveVids){
			removeNoMediaAndDecrypt(vids, false);
		}
		if (saveStories){
			removeNoMediaAndDecrypt(storiesPics, true);
			removeNoMediaAndDecrypt(storiesVids, true);
		}
		
		
		
		toastMessage("snaps saved", 1);
	}
	
	public void removeNoMediaAndDecrypt(File[] f, boolean story){//removes ".nomedia" from all files within directory f
		if (f != null){ 
			for (int i = 0; i < f.length; i++) {
				String fileName = f[i].getName();
				if (fileName.contains("nomedia")){
					String newFileName = fileName.substring(0, fileName.length() - 8);
					
					File encryptedFile = new File(f[i].getParent(), newFileName);
					
					f[i].renameTo(encryptedFile); 
					f[i].delete();
					
					Log.d(encryptedFile.toString(), encryptedFile.getPath());
					
					
				}
			}
			
		}
	}
	
	
	
	public void decrypt(File f){
		try {
			
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(2, new SecretKeySpec("M02cnQ51Ji97vwT4".getBytes(), "AES")); //encryption key found in snapchat source 
			//cipher.init(2, new SecretKeySpec("1234567891123456".getBytes(), "AES"));
			
			byte[] fBytes = (byte[]) fileToBytes(f);
			
			byte[] unencryptedBytes = cipher.doFinal(fBytes);
			
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
			out.write(unencryptedBytes);
			out.flush();
			out.close();
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void decrypt(File[] f){
		for (int i = 0; i < f.length; i++){
			decrypt(f);
		}
	}
	
	public byte[] fileToBytes(File f){ //method taken from http://stackoverflow.com/questions/10039672/android-how-to-read-file-in-bytes
		
		byte[] bytes = new byte[ (int)f.length() ];
		
		try {
			BufferedInputStream buf = new BufferedInputStream(new FileInputStream(f));
			buf.read(bytes, 0, bytes.length);
			buf.close();
			return bytes;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private void toastMessage(String string, int dur) {
		Toast toast = Toast.makeText(this, string, dur);
		toast.show();
	}
	
	public void addMediaToGallery(File f) {
	    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	   
	    Uri contentUri = Uri.fromFile(f);
	    mediaScanIntent.setData(contentUri);
	    this.sendBroadcast(mediaScanIntent);
	}
	
	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { // Inflate the
	 * menu; this adds items to the action bar if it is present.
	 * getMenuInflater().inflate(R.menu.main, menu); return true; }
	 */   // might do settings later

}
