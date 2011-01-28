package com.neugent.aethervoice.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.ui.Receiver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.Activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;

import android.content.Context;

import android.content.DialogInterface;
import android.database.Cursor;

import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.telephony.PhoneNumberUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import com.neugent.aethervoice.R;

import com.neugent.aethervoice.xml.parse.*;
import com.neugent.aethervoice.log.*;


public class PldtDirectory extends Activity {
	
	private ListView directoryList;
	
//	private DirectoryListAdapter directoryListAdapter;
	
	private Button previouPageButton;
	
	private Button nextPageButton;
	
	private Button clearListButton;
	
	private Button searchButton;
	
	private TextView currentPage;
	
	private static ArrayList<DirectoryDataset> parsedDirectoryDataSet = new ArrayList<DirectoryDataset>();
	
	private DirectoryListAdapter directoryListAdapter;
	
	private EditText directory_search;
	
	private ConnectivityManager connMgr;
	
	private android.net.NetworkInfo wifi;
	
	private static ErrorAlert mErrorAlert;
	
	private int pageNumber;
	
	private int pageSize;
	
	private int totalRecords;
	
	private ProgressDialog progressD;
	
	private boolean search_now = false;
	
	protected final int SHOW_PROGRESS = 1;	
	protected final int HIDE_PROGRESS = 2;
	protected final int NO_DATA		  = 3;
	protected final int NO_CONNECTION = 4;
	protected final int CANCEL		  = 5;
	
	private Context mContext;
	
    Handler mHandler = new Handler() {
      	 @Override
           public void handleMessage(Message msg) {
      		 	if(msg.what == SHOW_PROGRESS){   			 	
      		 		if (progressD  == null) {
      		 			System.out.println("call dialog >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                   		progressD = ProgressDialog.show(mContext, null, "Searching...");
                   		progressD.setCancelable(true);
      		 		}
      		 		
      		 		//return;               	          	
                  
      		 	} else if(msg.what == HIDE_PROGRESS){
      		 		if (progressD != null) {
      		 			progressD.dismiss();
      		 			progressD = null;
      		 		}
      		 		directoryListAdapter.notifyDataSetChanged();
      		 		//directoryList.setAdapter(directoryListAdapter);
      		 		currentPage.setVisibility(View.VISIBLE);
      		 		currentPage.setText(pageNumber+" of "+getTotalPageSearch()+" pages");
      		 		
      		 		if (search_now) {
      		 			previouPageButton.setEnabled(false);
    					nextPageButton.setEnabled(true);
    					previouPageButton.setBackgroundResource(R.drawable.btn_prev_f3);
    					nextPageButton.setBackgroundResource(R.drawable.btn_next_bg);
    					
    					search_now = false;
    					
      		 		} else {
      		 			if (pageNumber <= getTotalPageSearch() && pageNumber != 0) {
							previouPageButton.setEnabled(true);
							nextPageButton.setEnabled(true);
							previouPageButton.setBackgroundResource(R.drawable.btn_prev_bg);
							nextPageButton.setBackgroundResource(R.drawable.btn_next_bg);
      		 			} 
      		 			if (pageNumber == 1) {
      		 				previouPageButton.setEnabled(false);
							previouPageButton.setBackgroundResource(R.drawable.btn_prev_f3);	
      		 			} else if (pageNumber == getTotalPageSearch()) {
							nextPageButton.setEnabled(false);
							nextPageButton.setBackgroundResource(R.drawable.btn_next_f3);
      		 			}
      		 		}	
      		 		
      		 		System.out.println("directoryList count >>>>>>>>>>>>>>>>>>>>>>>>>>>"+directoryList.getCount());				
      		 		

      		 	} else if(msg.what == NO_DATA){
      		 		if (progressD != null) {
      		 			progressD.dismiss();
      		 			progressD = null;
      		 		}
      		 		PldtDirectory.mErrorAlert.showErrorDialog("Directory Error", "Type another keyword!");		

      		 	} else if(msg.what == NO_CONNECTION){
      		 		if (progressD != null) {
      		 			progressD.dismiss();
      		 			progressD = null;
      		 		}
      		 		PldtDirectory.mErrorAlert.showErrorDialog("Directory Error", "Connection Problem! Check internet connection.");

      		 	} else if(msg.what == CANCEL){
      		 		if (progressD != null) {
      		 			progressD.dismiss();
      		 			progressD = null;
      		 		}
      		 		
      		 		if (loadThread != null) {
      		 			loadThread.interrupt();
      		 			loadThread = null;
      		 		}      		 		

      		 	} 
           
      	 	}
       };
       
       private Thread loadThread;
       
       private Runnable getParsedData = new Runnable(){
   		@Override 
   		public void run() {
   				mHandler.sendEmptyMessage(SHOW_PROGRESS); 
					if (wifi.isConnected() && AetherVoice.dialer.checkInternetConnection()) { 
						System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Search");
						
						if (parseDirectoryService(directory_search.getText().toString(), pageNumber, pageSize)) {
							if (parsedDirectoryDataSet != null && parsedDirectoryDataSet.size() >= 0) {
								//directoryListAdapter = new DirectoryListAdapter(getApplicationContext());
								
								//directoryListAdapter.notifyDataSetChanged();
								
								mHandler.sendEmptyMessage(HIDE_PROGRESS);
								
							} else {
								mHandler.sendEmptyMessage(NO_DATA); 
							 
							}
						} else {
							mHandler.sendEmptyMessage(NO_CONNECTION); 					 
						 
						}	
					} else {
						mHandler.sendEmptyMessage(NO_CONNECTION); 	
					}
				 
   		}
       };
	
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pldt_directory);
        mContext = this;
        AetherVoice.setIsFinishing(false);
        
        _FakeX509TrustManager.allowAllSSL();
        
        initView();
	}	
	
	@Override
	protected void onResume() {
		super.onResume();		
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> onResume");
		if (Receiver.call_state != UserAgent.UA_STATE_IDLE)
			Receiver.moveTop();
		
		AetherVoice.setIsFinishing(false);
	}
	
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {		
		if (loadThread != null ) {
			mHandler.sendEmptyMessage(CANCEL); 
		}
		
		return false;
	}
	
	private void initView(){		
		pageNumber = 1;
		pageSize = 10;
		
		search_now = false;
		
		mErrorAlert = new ErrorAlert(this);	

		connMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
    	wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		
    	currentPage = (TextView)findViewById(R.id.page);
		directory_search = (EditText)findViewById(R.id.directory_search_box);
		searchButton = (Button)findViewById(R.id.search_btn);
		directoryList = (ListView)findViewById(R.id.directory_list_numbers);
		previouPageButton = (Button)findViewById(R.id.directory_btn_prev);
		nextPageButton  = (Button)findViewById(R.id.directory_btn_next);		
		clearListButton = (Button)findViewById(R.id.directory_btn_clear);
		
		currentPage.setVisibility(View.INVISIBLE);
		previouPageButton.setEnabled(false);
		nextPageButton.setEnabled(false);
		previouPageButton.setBackgroundResource(R.drawable.btn_prev_f3);
		nextPageButton.setBackgroundResource(R.drawable.btn_next_f3);
		
		clearListButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(directoryListAdapter != null) 
					if (directoryListAdapter.getCount()>0)
					getClearListDialog().show();

			}
			
		});
		
		previouPageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (parsedDirectoryDataSet != null) {
					if (parsedDirectoryDataSet.size() >= 0) {
						
						if (directory_search.getText().length() != 0) {						
							if ((pageNumber - 1) <= getTotalPageSearch() && ((pageNumber - 1) != 0)) {
								--pageNumber;
								loadThread = new Thread(getParsedData);
								loadThread.start();	
								
//								previouPageButton.setEnabled(true);
//								nextPageButton.setEnabled(true);
//								previouPageButton.setBackgroundResource(R.drawable.btn_prev_bg);
//								nextPageButton.setBackgroundResource(R.drawable.btn_next_bg);
//								
//								currentPage.setText(pageNumber+" of "+getTotalPageSearch()+" pages");
//								
//								if (pageNumber == 1) {
//									previouPageButton.setEnabled(false);
//									previouPageButton.setBackgroundResource(R.drawable.btn_prev_f3);	
//								}
							} else {
								previouPageButton.setEnabled(false);
								previouPageButton.setBackgroundResource(R.drawable.btn_prev_f3);								
							}
					
						} else {
							PldtDirectory.mErrorAlert.showErrorDialog("Directory Error", "No Keyword found. Please enter and try again!");
						}	
					}
				}

			}
			
		});
		
		nextPageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (parsedDirectoryDataSet != null) {
					if (parsedDirectoryDataSet.size() >= 0) {
						
						if (directory_search.getText().length() != 0) {						
							if ((pageNumber + 1) <= getTotalPageSearch()) {
								++pageNumber;
								loadThread = new Thread(getParsedData);
								loadThread.start();	
								
//								previouPageButton.setEnabled(true);
//								nextPageButton.setEnabled(true);
//								previouPageButton.setBackgroundResource(R.drawable.btn_prev_bg);
//								nextPageButton.setBackgroundResource(R.drawable.btn_next_bg);
//								
//								currentPage.setText(pageNumber+" of "+getTotalPageSearch()+" pages");
//								
//								if (pageNumber == getTotalPageSearch()) {
//									nextPageButton.setEnabled(false);
//									nextPageButton.setBackgroundResource(R.drawable.btn_next_f3);
//								}
							} else {
								nextPageButton.setEnabled(false);
								nextPageButton.setBackgroundResource(R.drawable.btn_next_f3);
							}
						} else {
							PldtDirectory.mErrorAlert.showErrorDialog("Directory Error", "No Keyword found. Please enter and try again!");
						}	
					}
				}
				
			}
			
		});
				
		
		searchButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {				
				if (directory_search.getText().length() != 0) {
					pageNumber = 1;
					loadThread = new Thread(getParsedData);
					loadThread.start();	
					
					search_now = true;
					
//					previouPageButton.setEnabled(false);
//					nextPageButton.setEnabled(true);
//					previouPageButton.setBackgroundResource(R.drawable.btn_prev_f3);
//					nextPageButton.setBackgroundResource(R.drawable.btn_next_bg);
					
				} else {
					PldtDirectory.mErrorAlert.showErrorDialog("Directory Error", "No Keyword found. Please enter and try again!");
				}			
			} 
			 
		 });

		
		directory_search.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				
				if(actionId == EditorInfo.IME_ACTION_DONE){
					if (directory_search.getText().length() != 0) {
						pageNumber = 1;
						loadThread = new Thread(getParsedData);
						loadThread.start();	
						
						search_now = true;
						
//						previouPageButton.setEnabled(false);
//						nextPageButton.setEnabled(true);
//						previouPageButton.setBackgroundResource(R.drawable.btn_prev_f3);
//						nextPageButton.setBackgroundResource(R.drawable.btn_next_bg);
						
					} else {
						PldtDirectory.mErrorAlert.showErrorDialog("Directory Error", "No Keyword found. Please enter and try again!");
					}	
					
				} 
				return false;
			}
		});
		
		directoryListAdapter = new DirectoryListAdapter(mContext);
		directoryList.setAdapter(directoryListAdapter);
		
//		directoryList.setOnTouchListener(new OnTouchListener() {
//
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
//				// TODO Auto-generated method stub
//				System.out.println("touch>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");				
//				return false;
//			}
//			
//		});
		
		
		directoryList.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(final AdapterView<?> arg0, final View v,
							final int position, final long id) {
		        		System.out.println("Click Record>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

		        		String data3 = PhoneNumberUtils.extractNetworkPortion(parsedDirectoryDataSet.get(position).getTelephone());
		    			data3 = PhoneNumberUtils.formatNumber(data3);
		        		
						String data = "Company Name: "+parsedDirectoryDataSet.get(position).getCompanyName()+"\n"
						             +"SubCategory: "+parsedDirectoryDataSet.get(position).getSubCategory()+"\n"
						             +"Full Address: "+parsedDirectoryDataSet.get(position).getAddress()+"\n"
						             +"City: "+parsedDirectoryDataSet.get(position).getCity()+"\n"
						             +"Telephone: "+data3+"\n";
			
						PldtDirectory.mErrorAlert.showErrorDialog("View", data);
					}
		});

		
	}
	


	

	private int getTotalPageSearch() {
		int remainder = 0;
		
		if (totalRecords % 10 > 1) {
			remainder = 1;
		}
		
		return ((totalRecords / 10) + (remainder));
		
	}
	
	
    private AlertDialog getClearListDialog() {
		final AlertDialog.Builder clearDialog = new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Clear Results?")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog,
				 int whichButton) {
					if (parsedDirectoryDataSet != null) {
						parsedDirectoryDataSet.clear();
						directoryListAdapter.notifyDataSetChanged();
						directory_search.setText("");
						
						previouPageButton.setEnabled(false);
						nextPageButton.setEnabled(false);
						previouPageButton.setBackgroundResource(R.drawable.btn_prev_f3);
						nextPageButton.setBackgroundResource(R.drawable.btn_next_f3);
						
						currentPage.setVisibility(View.INVISIBLE);
					}	
					}
				})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog,
						int whichButton) {
				}
			});
		return clearDialog.create();
	}

	
    private boolean parseDirectoryService(String keyword, int pageNumber, int pageSize) {

        try {       	
        	
            /* Create a URL we want to load some xml-data from. */
            URL url = new URL("http://www.phoenixph.com/TelpadDialerService/TelpadService/DirectoryService/Search/"+keyword.replace(" ", "%20").trim()+"/"+pageNumber+"/"+pageSize);

            /* Get a SAXParser from the SAXPArserFactory. */
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();

            /* Get the XMLReader of the SAXParser we created. */
            XMLReader xr = sp.getXMLReader();
            /* Create a new ContentHandler and apply it to the XML-Reader*/
            
            DirectoryHandler myExampleHandler = new DirectoryHandler();
            xr.setContentHandler(myExampleHandler);
           
            /* Parse the xml-data from our URL. */
            xr.parse(new InputSource(url.openStream()));
            /* Parsing has finished. */
            
            //parsedDirectoryDataSet.clear();
            
            parsedDirectoryDataSet = myExampleHandler.getParsedData();
           
            
            System.out.println("Total Records >>>>>>>>>>>>>>>>> "+myExampleHandler.getTotalRecords());
            
            totalRecords = Integer.parseInt(myExampleHandler.getTotalRecords().toString());
            
            for (int x=0; x<parsedDirectoryDataSet.size(); x++) {
                System.out.println("Category: "+parsedDirectoryDataSet.get(x).getCategory());
                System.out.println("City: "+parsedDirectoryDataSet.get(x).getCity());
                System.out.println("CompanyName: "+parsedDirectoryDataSet.get(x).getCompanyName());
                System.out.println("FullAddress: "+parsedDirectoryDataSet.get(x).getAddress());
                System.out.println("Id: "+parsedDirectoryDataSet.get(x).getId());                    
                System.out.println("ListingCode: "+parsedDirectoryDataSet.get(x).getListingCode());
                System.out.println("SubCategory: "+parsedDirectoryDataSet.get(x).getSubCategory());
                System.out.println("Telephone: "+parsedDirectoryDataSet.get(x).getTelephone());                              
                
            }
            
            return true;
           
        } catch (Exception e) {
            /* Display any Error to the GUI. */
            System.out.println("Error: " + e.getMessage());
            return false;
        }
  
    }
	

	private static class DirectoryListAdapter extends BaseAdapter{
		
		private Context context;
		private LayoutInflater mInflater;

        public DirectoryListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
            this.context = context;
        }
		
		public int getCount() {
			return parsedDirectoryDataSet.size();
				
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
			if(convertView == null){
				convertView = mInflater.inflate(R.layout.directory_entry, null);
				holder = new ViewHolder();
				holder.txt_name = (TextView) convertView.findViewById(R.id.directory_contact_name);
				holder.txt_number = (TextView) convertView.findViewById(R.id.directory_contact_number);
				holder.txt_address = (TextView) convertView.findViewById(R.id.directory_contact_address);				
				holder.img = (Button) convertView.findViewById(R.id.directory_call_button);
				
                convertView.setTag(holder);
			}else {
				 holder = (ViewHolder) convertView.getTag();
			}

			holder.txt_name.setText(parsedDirectoryDataSet.get(position).getCompanyName());
			holder.txt_address.setText(parsedDirectoryDataSet.get(position).getAddress());
			
			
			String data3 = PhoneNumberUtils.extractNetworkPortion(parsedDirectoryDataSet.get(position).getTelephone());
			data3 = PhoneNumberUtils.formatNumber(data3);
			holder.txt_number.setText( PhoneNumberUtils.formatNumber(data3));
			
			holder.img.setOnClickListener(getClickListener(context, holder.img, position));
			
			//convertView.setOnClickListener(getRecordClickListener(context, position));
						
			return convertView;
		}
	}
	
	static class ViewHolder{
		TextView txt_name;
		TextView txt_number;
		TextView txt_address;		
		Button img;
	}	

	private static OnClickListener getClickListener(final Context context, final Button img, final int position) {
        return new View.OnClickListener(){ 
                public void onClick(View v){
                	String data3 = PhoneNumberUtils.extractNetworkPortion(parsedDirectoryDataSet.get(position).getTelephone());
        			data3 = PhoneNumberUtils.formatNumber(data3);
                	if (!Dialer.isVoip) {
                		System.out.println("Click to call>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            			AetherVoice.dialer.dialPSTN(PhoneNumberUtils.formatNumber(data3));
                	} else {
                		if (SettingsWindow.isRegistered)
                			AetherVoice.dialer.dial(data3, context);
                		else 
                			Toast.makeText(context, R.string.toast_register, Toast.LENGTH_SHORT).show();
                		//Toast.makeText(context, "Please dock the tablet to proceed the call", Toast.LENGTH_LONG).show();
                	}
                	
                }	 
        };
	}
	
	private static OnClickListener getRecordClickListener(final Context context, final int position) {
        return new View.OnClickListener(){ 
                public void onClick(View v){
                		System.out.println("Click Record>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

                		String data3 = PhoneNumberUtils.extractNetworkPortion(parsedDirectoryDataSet.get(position).getTelephone());
            			data3 = PhoneNumberUtils.formatNumber(data3);
                		
						String data = "Company Name: "+parsedDirectoryDataSet.get(position).getCompanyName()+"\n"
						             +"SubCategory: "+parsedDirectoryDataSet.get(position).getSubCategory()+"\n"
						             +"Full Address: "+parsedDirectoryDataSet.get(position).getAddress()+"\n"
						             +"City: "+parsedDirectoryDataSet.get(position).getCity()+"\n"
						             +"Telephone: "+data3+"\n";
			
						PldtDirectory.mErrorAlert.showErrorDialog("View", data);
                }	 
        };
	}
	
}
