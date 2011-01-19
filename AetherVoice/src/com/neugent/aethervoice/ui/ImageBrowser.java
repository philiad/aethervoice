package com.neugent.aethervoice.ui;

import java.io.File;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;

import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.neugent.aethervoice.R;

public class ImageBrowser extends Activity{
	
	private BroadcastReceiver mReceiver;
	private Cursor imageCursor;
	private int image_column_index;
	private int count;
	private ProgressDialog mProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.aethervoice_grid);
		
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setMessage("Loading Images");
		mProgressDialog.setCancelable(false);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		
		mReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
					Toast.makeText(getApplicationContext(), "No external storage found.", Toast.LENGTH_SHORT).show();
		        	finish();
		        } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
		        	mProgressDialog.show();
		        	Log.d("AetherVoice", "MEDIA SCANNING STARTED");
		        	//TODO show progress dialog here
		        } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
		        	mProgressDialog.cancel();
		        	Log.d("AetherVoice", "MEDIA SCANNING FINISHED");
		        	//destroy progress dialog here
		        	initPhoneImages();
		        } else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
		        	Toast.makeText(getApplicationContext(), "No external storage found.", Toast.LENGTH_SHORT).show();
		        	finish();
		        }else if(action.equals(Intent.ACTION_MEDIA_MOUNTED)){
		        	Log.d("AetherVoice", "MEDIA SCANNING on MOUNTED EXTERNAL DATA SOURCE");
		        }
			}
		};
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addDataScheme("file");

        registerReceiver(mReceiver, intentFilter);
        
    	sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse 
				("file://"+ Environment.getExternalStorageDirectory())));
        
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse 
				("file://"+ "/sdcard")));
        
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse 
				("file://"+ "removalbe_usb")));
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(mReceiver);
	}
	
	private void initPhoneImages(){
//		String[] img = { 	MediaStore.Images.Thumbnails._ID ,
//							MediaStore.Images.Thumbnails.IMAGE_ID };
		
		final Uri uri = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;
		try{
			imageCursor = managedQuery(uri, null, null,	null, null);
			startManagingCursor(imageCursor);
			if(imageCursor.moveToFirst()){
				image_column_index = imageCursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails._ID);
				count = imageCursor.getCount();
				GridView gridView = (GridView) findViewById(R.id.gridViewImage);
				gridView.setAdapter(new ImageCursorAdapter(this, imageCursor));
				gridView.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						System.gc();
				        String[] proj = { MediaStore.Images.Media.DATA };
				        Cursor actualImageCursor = getContentResolver().query(uri, proj, null, null, null);
				        int actual_image_column_index = actualImageCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				        actualImageCursor.moveToPosition(position);
				        String i = actualImageCursor.getString(actual_image_column_index);
				        System.out.println("The image path is "+i);
				        try{
				        	File photoFile = new File(i);
				        	EditContactInfo.setProfilePhoto(getApplicationContext(), photoFile);
				        }catch (Exception e) {
				        	Toast.makeText(getApplicationContext(), "File not found", Toast.LENGTH_LONG).show();
				        }
				        
				        finish();
					}
				});
			}else{
				finish();
			}
		}catch(Exception e){ finish(); }
	}
	
	public class ImageCursorAdapter extends CursorAdapter{
		
		public ImageCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			try{
				int actual_image_column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		        String path = cursor.getString(actual_image_column_index);
				((ImageView) view).setImageDrawable(Drawable.createFromPath(path));
//				int id = imageCursor.getInt(image_column_index);
//				int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
//				String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
//				String path = cursor.getString(index);
//				long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Thumbnails.IMAGE_ID));
//				System.out.println("THE ID OF THE IMAGE IS "+id);
				
//				((ImageView) view).setImageURI(Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, ""+id));
			}catch(Exception e){ 
				((ImageView) view).setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.bg_no_prev));
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			ImageView imageView = new ImageView(context.getApplicationContext());
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
	        imageView.setLayoutParams(new GridView.LayoutParams(90, 90));
			return imageView;
		}
		
	}
	
	public class ImageAdapter extends BaseAdapter{
		private Context mContext;
		
		public ImageAdapter(Context context){
			mContext = context;
		}

		@Override
		public int getCount() {
			return count;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			System.gc();
            ImageView imageView = new ImageView(mContext.getApplicationContext());
            if (convertView == null) {
                imageCursor.moveToPosition(position);
                int id = imageCursor.getInt(image_column_index);
                imageView.setImageURI(Uri.withAppendedPath(
					MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, ""
					+ id));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(new GridView.LayoutParams(90, 90));
          }
          else {
        	  imageView = (ImageView) convertView;
          }
			return imageView;
		}
		
	}
}
