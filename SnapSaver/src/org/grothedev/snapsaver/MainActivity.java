package org.grothedev.snapsaver;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Timer;
import java.util.TimerTask;

import org.grothedev.snapserver.R;

import android.os.Bundle;
import android.os.Environment;


import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	File extStorageFile = Environment.getExternalStorageDirectory(); 
	String extStoragePath = extStorageFile.getPath();
	File savedsnaps = new File(extStorageFile + "/savedsnaps");
	Button ready;
	private Timer timer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
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
			
			PrintStream stdin = new PrintStream(p.getOutputStream()); 
			stdin.println("mount -o remount,rw -t yaffs2 /dev/block/mtdblock3 /system");
			stdin.println("mkdir " + extStoragePath + "/savedsnaps");
			stdin.println("cp -r /data/data/com.snapchat.android/cache/received_image_snaps/* "
									+ extStoragePath + "/savedsnaps");
			stdin.println("cp -r /storage/sdcard0/Android/data/com.snapchat.android/cache/received_video_snaps/* "
					+ extStoragePath + "/savedsnaps");
			stdin.println("mount -o remount,ro -t yaffs2 /dev/block/mtdblock3 /system");
			
		} catch (IOException e) {
			e.printStackTrace();
		} //end root shell
		
		
		
	}
	
	private void rename(){
		File[] nomedia = savedsnaps.listFiles();
		if (nomedia != null){
			for (int i = 0; i < nomedia.length; i++) {
				Log.d(nomedia[i].getPath(), "");
				if (nomedia[i].getPath().contains("jpg")){
					nomedia[i].renameTo(new File(savedsnaps, "" + i + ".jpg"));
					nomedia[i].delete();
				}
				if (nomedia[i].getPath().contains("mp4")){
					nomedia[i].renameTo(new File(savedsnaps, "" + i + ".mp4"));
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
