package com.neugent.aethervoice.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.neugent.aethervoice.R;

/**
 * Loads images from SD card. 
 * 
 * 
 *
 */
public class ImageGallery extends Activity {
    
    /**
     * Grid view holding the images.
     */
    private GridView sdcardImages;
    /**
     * Image adapter for the grid view.
     */
    private ImageAdapter imageAdapter;
    /**
     * Display used for getting the width of the screen. 
     */
    private Display display;
    
    //File[] imageList;
    
    private static final String sUSB_folder = "/removable_usb";
    
    protected final int END_PROGRESS = 9;	
    
    protected final int ERROR_PROGRESS = 8;
    
    ArrayList<String> imageList;
    
    private Handler mHandler = new Handler() {
      	 @Override
           public void handleMessage(Message msg) {
      		 	if(msg.what == END_PROGRESS){   			 	
      		 		Toast.makeText(getApplicationContext(), "No Images Found!", Toast.LENGTH_LONG).show();
               		finish();
               	} else if (msg.what == ERROR_PROGRESS){
      		 		Toast.makeText(getApplicationContext(), "Unreadable SDcard!", Toast.LENGTH_LONG).show();
               		finish();
               	}
      	    }
    };

    /**
     * Creates the content view, sets up the grid, the adapter, and the click listener.
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        // Request progress bar
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.aethervoice_grid);

		imageList = new ArrayList<String>();
		
        display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        setupViews();
        setProgressBarIndeterminateVisibility(true); 
        loadImages();
    }

    /**
     * Free up bitmap related resources.
     */
    protected void onDestroy() {
        super.onDestroy();
        final GridView grid = sdcardImages;
        final int count = grid.getChildCount();
        ImageView v = null;
        for (int i = 0; i < count; i++) {
            v = (ImageView) grid.getChildAt(i);
            ((BitmapDrawable) v.getDrawable()).setCallback(null);
        }
    }
    /**
     * Setup the grid view.
     */
    private void setupViews() {
        sdcardImages = (GridView) findViewById(R.id.gridViewImage);
        sdcardImages.setNumColumns(4);
        sdcardImages.setClipToPadding(false);
        sdcardImages.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				System.gc();
	        	File photoFile = new File(imageList.get(position));
	        	EditContactInfo.setProfilePhoto(getApplicationContext(), photoFile);
		        finish();
			}
		});
        imageAdapter = new ImageAdapter(getApplicationContext()); 
        sdcardImages.setAdapter(imageAdapter);
    }
    /**
     * Load images.
     */
    private void loadImages() {		
        final Object data = getLastNonConfigurationInstance();
        if (data == null) {
            new LoadImagesFromSDCard().execute();
        } else {
            final LoadedImage[] photos = (LoadedImage[]) data;
            if (photos.length == 0) {
                new LoadImagesFromSDCard().execute();
            }
            for (LoadedImage photo : photos) {
                addImage(photo);
            }
        }
    }
    /**
     * Add image(s) to the grid view adapter.
     * 
     * @param value Array of LoadedImages references
     */
    private void addImage(LoadedImage... value) {
        for (LoadedImage image : value) {
            imageAdapter.addPhoto(image);
            imageAdapter.notifyDataSetChanged();
        }
    }
    
    /**
     * Save bitmap images into a list and return that list. 
     * 
     * @see android.app.Activity#onRetainNonConfigurationInstance()
     */
//    @Override
//    public Object onRetainNonConfigurationInstance() {
//        final GridView grid = sdcardImages;
//        final int count = grid.getChildCount();
//        final LoadedImage[] list = new LoadedImage[count];
//
//        for (int i = 0; i < count; i++) {
//            final ImageView v = (ImageView) grid.getChildAt(i);
//            list[i] = new LoadedImage(((BitmapDrawable) v.getDrawable()).getBitmap());
//        }
//
//        return list;
//    }
    
    
   
    /**
     * Async task for loading the images from the SD card. 
     * 
     * @author Mihai Fonoage
     *
     */
    class LoadImagesFromSDCard extends AsyncTask<Object, LoadedImage, Object> {
        
        /**
         * Load images from SD Card in the background, and display each image on the screen. 
         *  
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Object doInBackground(Object... params) {

        	Bitmap bitmap = null;
        	Bitmap newBitmap = null;
        	Uri uri = null; 
        	
        	File f_sdcard = new File("/sdcard");

            File f_usb = new File(sUSB_folder);
            
            if ((!(f_sdcard.canRead() && f_sdcard.isDirectory())) && (!(f_usb.canRead() && f_usb.isDirectory()))) {
            	mHandler.sendEmptyMessage(END_PROGRESS);
            } 
            

            if (!(f_sdcard.canRead() && f_sdcard.isDirectory())) {
            	//mHandler.sendEmptyMessage(END_PROGRESS);
            }  else {         
        		
            	// Search sdcard/image catalog picture file , As the gallery source ,
            	//File sdcard=new File(Environment.getExternalStorageDirectory().toString());
            	//Log.d(TAG,"Gallery1.sdcard."+sdcard.getName());          	 	
            	
            	traverse(f_sdcard);
            }
            
            if (!(f_usb.canRead() && f_usb.isDirectory())) {
            	//mHandler.sendEmptyMessage(END_PROGRESS);
            } else {
            	traverse(f_usb); 
            }
            
            try {
            	for(int i=0 ; i<imageList.size(); i++){
            		File file=new File(imageList.get(i));
            		
            		final int idealWidth = 186;
            		final int idealHeight = 199;
            		final BitmapFactory.Options bfo = new BitmapFactory.Options();
            		bfo.inJustDecodeBounds = true;
            		BitmapFactory.decodeFile(file.getAbsolutePath(), bfo);
            		final int imageWidth = bfo.outWidth;
            		final int imageHeight = bfo.outHeight;
            		if (imageWidth > idealWidth || imageHeight > idealHeight) {
            			final int imageWidthRatio = (int) Math.floor(imageWidth
            					/ idealWidth);
            			final int imageHeightRatio = (int) Math.floor(imageHeight
            					/ idealHeight);
            			bfo.inSampleSize = imageWidthRatio > imageHeightRatio ? imageHeightRatio
            					: imageWidthRatio;
            		}
            		
            		bfo.inJustDecodeBounds = false;
            		bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), bfo);
            		
//            		bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());       		
            		

            		if (bitmap != null) {
            			newBitmap = Bitmap.createScaledBitmap(bitmap, 70, 70, true);
            			bitmap.recycle();
            			if (newBitmap != null) {
            				publishProgress(new LoadedImage(newBitmap));
            			}
            		}
            	} 
            } catch (Exception e) {
            	e.printStackTrace();            	
            	mHandler.sendEmptyMessage(ERROR_PROGRESS);
            }
       	
        	return null;
        }
        
        /**
         * Add a new LoadedImage in the images grid.
         *
         * @param value The image.
         */
        @Override
        public void onProgressUpdate(LoadedImage... value) {
            addImage(value);
        }
        /**
         * Set the visibility of the progress bar to false.
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Object result) {
            setProgressBarIndeterminateVisibility(false);
        }
    }
    
    private static int computeSampleSize(BitmapFactory.Options options, int target) {
        int w = options.outWidth;
        int h = options.outHeight;

        int candidateW = w / target;
        int candidateH = h / target;
        int candidate = Math.min(candidateW, candidateH);

        if (candidate == 0)
            return 1;

        if (candidate > 1) {
            if ((w > target) && (w / candidate) < target)
                candidate += 1;
        }

        if (candidate > 1) {
            if ((h > target) && (h / candidate) < target)
                candidate += 1;
        }
        	
        return candidate;
    }
    
    private void traverse(File dir) {   	
        if (dir.isDirectory()) {
            File str[] = dir.listFiles();
            
            if (str.length > 0) {            	
                for( int i = 0; i<=str.length-1; i++){
                	
                	File f=new File(str[i].toString());
                	
                	if (f.getName().equalsIgnoreCase(".android_secure")) {
                		continue;
                	}
                	
                	if (f.getName().equalsIgnoreCase(".thumbnails")) {
                		continue;
                	}
                	
                	
                	if(f.isDirectory()){
             
                		System.out.println(str[i] + " is a directory>>>>>>>>>>>>>>>>>>>>>>>>>>");
                		traverse(f);
            
                    } else if (f.isFile()){             	
                    	
           
                    	System.out.println(f.getName() + " <<<<<<<<<<<<<<<<<<<<<<<<<<<is a file");
           				
                    	String ex=f.getName().substring(f.getName().lastIndexOf(".")+1,f.getName().length()).toLowerCase();
           				
           				if(ex==null||ex.length()==0){
        				
           				}else{
           					if(ex.equals("jpg")||ex.equals("bmp")||ex.equals("png")||ex.equals("gif")){
           						imageList.add(f.getPath());
           					}
        					
           				}
            
                    }	
                } 
            } else {
            	System.out.println(">>>>>>>>>>>>>>>>>>>> no images");
            }
            
          

        }
    }
    
    class ImageFilter implements FilenameFilter {

    	@Override public boolean accept(File dir, String name) {
    		return name.endsWith(".jpg") || name.endsWith(".JPG") ||
            	   name.endsWith(".png") || name.endsWith(".PNG") ||
                   name.endsWith(".bmp") || name.endsWith(".BMP");
    	}                         
    }

    /**
     * Adapter for our image files. 
     * 
     * @author Mihai Fonoage
     *
     */
    class ImageAdapter extends BaseAdapter {

        private Context mContext; 
        private ArrayList<LoadedImage> photos = new ArrayList<LoadedImage>();

        public ImageAdapter(Context context) { 
            mContext = context; 
        } 

        public void addPhoto(LoadedImage photo) { 
            photos.add(photo); 
        } 

        public int getCount() { 
            return photos.size(); 
        } 

        public Object getItem(int position) { 
            return photos.get(position); 
        } 

        public long getItemId(int position) { 
            return position; 
        } 

        public View getView(int position, View convertView, ViewGroup parent) { 
            final ImageView imageView; 
            if (convertView == null) { 
                imageView = new ImageView(mContext); 
            } else { 
                imageView = (ImageView) convertView; 
            } 
            imageView.setLayoutParams(new GridView.LayoutParams(80, 80));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setPadding(8, 8, 8, 8);
            imageView.setImageBitmap(photos.get(position).getBitmap());
            return imageView; 
        } 
    }

    /**
     * A LoadedImage contains the Bitmap loaded for the image.
     */
    private static class LoadedImage {
        Bitmap mBitmap;

        LoadedImage(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }
    }

}