package org.grothedev.snapsaver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.grothedev.snapsaver.R;

import android.os.Bundle;
import android.os.Environment;


import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	File extStorageFile = Environment.getExternalStorageDirectory(); 
	String extStoragePath = extStorageFile.getPath();
	File savedsnaps = new File(extStorageFile + "/savedsnaps");
	Button ready;
	private Timer timer;
	
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
				startTimers();
			}
		});
		
		timer = new Timer();
		
		
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
			rootText.setText("Your device appears to be rooted. Pictures and videos will be copied.");
		} else {
			rootText.setText("Your device appears to not be rooted. Only videos will be copied.");
		}
	}

	private void startTimers(){
		timer.schedule(new TimerTask(){ //after 15 seconds copy files. after another 10 seconds rename. 

			@Override
			public void run() {
				timerMethod();
			}
			
		}, 15000);
		timer.schedule(new TimerTask(){

			@Override
			public void run() {
				timerMethod2();
			}
			
		}, 25000);
		toastMessage("Open up SnapChat and wait for your snaps to load. There will be a message notifying you that your snaps have been saved", 4);
	}
	
	private void timerMethod(){
		this.runOnUiThread(timerTick);
	}
	private void timerMethod2(){
		this.runOnUiThread(timer2Tick);
	}
	
	private Runnable timerTick = new Runnable(){

		@Override
		public void run() {
			copyFiles();
		}
		
	};
	private Runnable timer2Tick = new Runnable(){

		@Override
		public void run() {
			rename();
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
			toastMessage("copying videos and pictures", 2);
			PrintStream stdin = new PrintStream(p.getOutputStream()); 
			stdin.println("mount -o remount,rw -t yaffs2 /dev/block/mtdblock3 /system");
			stdin.println("mkdir " + extStoragePath + "/savedsnaps");
			stdin.println("cp -r /data/data/com.snapchat.android/cache/received_image_snaps/* "
									+ extStoragePath + "/savedsnaps");
			stdin.println("cp -r /storage/sdcard0/Android/data/com.snapchat.android/cache/received_video_snaps/* "
					+ extStoragePath + "/savedsnaps");
			stdin.println("mount -o remount,ro -t yaffs2 /dev/block/mtdblock3 /system");
			
		} catch (IOException e) {
			toastMessage("copying videos", 2);
			copyVids();
		} //end root shell
		
		
		
	}
	
	private void copyVids() {
		File vidsDir = new File(extStoragePath + "/Android/data/com.snapchat.android/cache/received_video_snaps/");
		File[] vids = vidsDir.listFiles();
		
		if (vids != null){ //check if there are any vids to copy
			if (savedsnaps.exists()){ //check if savedsnaps folder exists
				for (int i = 0; i < vids.length; i++){
					try {
						copy(vids[i], new File(extStoragePath + "/savedsnaps/" + vids[i].getName()));
					} catch (IOException e) {
						
						e.printStackTrace();
					}
				}
			} else {
				savedsnaps.mkdirs();
				for (int i = 0; i < vids.length; i++){
					try {
						copy(vids[i], new File(extStoragePath + "/savedsnaps/" + vids[i].getName()));
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
	
	private void rename(){
		Random r = new Random(); //random int in filename to prevent overwriting
		File[] nomedia = savedsnaps.listFiles();
		if (nomedia != null){
			for (int i = 0; i < nomedia.length; i++) {
				
				if (nomedia[i].getPath().contains("jpg") && nomedia[i].getPath().contains("nomedia")){
					nomedia[i].renameTo(new File(savedsnaps, "" + i + "-" + r.nextInt(20) + ".jpg"));
					nomedia[i].delete();
				}
				if (nomedia[i].getPath().contains("mp4") && nomedia[i].getPath().contains("nomedia")){
					nomedia[i].renameTo(new File(savedsnaps, "" + i + "-" + r.nextInt(20) +".mp4"));
					nomedia[i].delete();
				}
			}
			toastMessage("snaps saved", 1);
		}
	}
	
	private void toastMessage(String string, int dur) {
		Toast toast = Toast.makeText(this, string, dur);
		toast.show();
	}
	
	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { // Inflate the
	 * menu; this adds items to the action bar if it is present.
	 * getMenuInflater().inflate(R.menu.main, menu); return true; }
	 */   // might do settings later

}
