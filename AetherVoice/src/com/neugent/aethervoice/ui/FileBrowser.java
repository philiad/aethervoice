package com.neugent.aethervoice.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.neugent.aethervoice.R;

public class FileBrowser extends Activity implements OnItemClickListener {

	private enum DISPLAYMODE {
		ABSOLUTE, RELATIVE;
	}

	private GridView gridView;
	private TextView fb_path;

	private static List<IconifiedText> folderList = new ArrayList<IconifiedText>();
	private static List<IconifiedText> imageList = new ArrayList<IconifiedText>();
	private static List<IconifiedText> webTextList = new ArrayList<IconifiedText>();
	private static List<IconifiedText> packedList = new ArrayList<IconifiedText>();
	private static List<IconifiedText> audioList = new ArrayList<IconifiedText>();
	private static List<IconifiedText> videoList = new ArrayList<IconifiedText>();
	private static List<IconifiedText> unknownList = new ArrayList<IconifiedText>();
	protected final int SUCCESS_RETURN_CODE = 1;
	protected final int SUCCESS_SET_FOLDER = 2;
	protected final int SUCCESS_BACK_CODE = 3;
	protected static final int FILEBROWSER_ACTION_REQUEST = 1;

	private final DISPLAYMODE displayMode = DISPLAYMODE.RELATIVE;
	private File currentDirectory = Environment.getExternalStorageDirectory();

	private static String currentPath;
	boolean bColapsed = false;
	private File clickedFile;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.filebrowser);
		FileBrowser.currentPath = Environment.getExternalStorageDirectory()
				.getPath()
				+ "/";
		createUI();
		browseTo(currentDirectory);
	}

	private void createUI() {
		gridView = (GridView) findViewById(R.id.gridView);
		fb_path = (TextView) findViewById(R.id.fb_path);

		fb_path.setText(currentDirectory.getName());
		gridView.setAdapter(new EfficientAdapter(this));
		gridView.setOnItemClickListener(this);

	}

	private void browseTo(final File aDirectory) {
		if (aDirectory.isDirectory()) {
			FileBrowser.folderList.clear();
			currentDirectory = aDirectory;
			fb_path.setText(FileBrowser.currentPath);
			storeSubFolders(aDirectory.listFiles());
		}
	}

	private void storeSubFolders(final File[] files) {
		if (files == null){
			 //Toast.makeText(this, getResources().getString(R.string.toast_no_storage), Toast.LENGTH_SHORT).show();
			return;
		}
		Drawable currentIcon = null;
		for (final File currentFile : files)
			if (currentFile.isDirectory() && !currentFile.isHidden()) {
				currentIcon = getResources().getDrawable(
						R.drawable.fbrowser_folder);
				FileBrowser.folderList.add(new IconifiedText(currentFile
						.getName(), currentIcon));
			} else {
				final String fileName = currentFile.getName();
				if (checkEndsWithInStringArray(fileName, getResources()
						.getStringArray(R.array.fileEndingImage))) {
					currentIcon = getResources().getDrawable(
							R.drawable.fbrowser_image);
					FileBrowser.imageList.add(new IconifiedText(currentFile
							.getName(), currentIcon));

				}
			}

		sortList(FileBrowser.folderList.size(), FileBrowser.folderList);
		sortList(FileBrowser.imageList.size(), FileBrowser.imageList);
		sortList(FileBrowser.webTextList.size(), FileBrowser.webTextList);
		sortList(FileBrowser.packedList.size(), FileBrowser.packedList);
		sortList(FileBrowser.audioList.size(), FileBrowser.audioList);
		sortList(FileBrowser.videoList.size(), FileBrowser.videoList);
		sortList(FileBrowser.unknownList.size(), FileBrowser.unknownList);

		for (int i = 0; i < 6; i++) {
			List<IconifiedText> tempList = new ArrayList<IconifiedText>();

			switch (i) {
			case 0: {
				tempList = FileBrowser.audioList;
			}
				break;
			case 1: {
				tempList = FileBrowser.videoList;
			}
				break;
			case 2: {
				tempList = FileBrowser.imageList;
			}
				break;
			case 3: {
				tempList = FileBrowser.webTextList;
			}
				break;
			case 4: {
				tempList = FileBrowser.packedList;
			}
				break;
			case 5: {
				tempList = FileBrowser.unknownList;
			}
				break;
			}
			for (int j = 0; j < tempList.size(); j++)
				FileBrowser.folderList.add(tempList.get(j));
			tempList.clear();
		}
		FileBrowser.audioList.clear();
		FileBrowser.videoList.clear();
		FileBrowser.imageList.clear();
		FileBrowser.webTextList.clear();
		FileBrowser.packedList.clear();
		FileBrowser.unknownList.clear();
		gridView.invalidateViews();
	}

	public void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {

		final String tempPath = FileBrowser.currentPath;

		switch (this.displayMode) {
		case RELATIVE: {
			clickedFile = new File(currentDirectory.getAbsolutePath() + "\\"
					+ FileBrowser.folderList.get(position).getText());
		}
			break;
		case ABSOLUTE: {
			clickedFile = new File(FileBrowser.folderList.get(position)
					.getText());
		}
			break;
		}
		
		if(clickedFile.isHidden()){
			//TODO: some message
		} if (clickedFile.isDirectory())
			FileBrowser.currentPath = FileBrowser.currentPath
					+ FileBrowser.folderList.get(position).getText() + "/";
		else {
			final String fileName = clickedFile.getName();
			if (checkEndsWithInStringArray(fileName, getResources()
					.getStringArray(R.array.fileEndingImage))) {
				EditContactInfo.setProfilePhoto(this, clickedFile);
				finish();
			}
		}

		if (clickedFile != null)
			try {
				storeSubFolders(clickedFile.listFiles());
				this.browseTo(clickedFile);
			} catch (final NullPointerException e) {
				FileBrowser.currentPath = tempPath;
				Toast.makeText(
						this,
						FileBrowser.folderList.get(position).getText()
								+ " inaccessible", Toast.LENGTH_SHORT).show();
			}

	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			final String tempPath = FileBrowser.currentPath;
			if (tempPath.equals(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"))
				finish();
			else {
				String temp = "";
				for (int i = FileBrowser.currentPath.length() - 2; i >= 0; i--)
					if (FileBrowser.currentPath.charAt(i) == '/') {
						for (int j = 0; j < i; j++)
							temp = temp + FileBrowser.currentPath.charAt(j);
						temp = temp + '/';
						FileBrowser.currentPath = temp;
						i = 0;
					}
				if (currentDirectory.getParent() != null) {
					if (FileBrowser.currentPath.equals("/")) {
						FileBrowser.currentPath = tempPath;
						finish();
					}
					browseTo(new File(FileBrowser.currentPath));
				}
			}
		}
		return true;
	}

	private void sortList(final int numberofItems,
			final List<IconifiedText> list) {

		boolean exchangeMade = true;
		int pass = 0;
		IconifiedText itTemp;

		while ((pass < numberofItems - 1) && exchangeMade == true) {
			exchangeMade = false;
			pass++;

			for (int i = 0; i < (numberofItems - pass); i++)
				if (list.get(i).getText().compareTo(list.get(i + 1).getText()) > 0) {
					itTemp = list.get(i);
					list.set(i, list.get(i + 1));
					list.set(i + 1, itTemp);
					exchangeMade = true;
				}
		}
	}

	private boolean checkEndsWithInStringArray(final String checkItsEnd,
			final String[] fileEndings) {
		for (final String aEnd : fileEndings)
			if (checkItsEnd.endsWith(aEnd))
				return true;
		return false;
	}

	private static class EfficientAdapter extends BaseAdapter {

		private final LayoutInflater mInflater;
		private String textInfo = "";
		private Drawable mIcon;

		public EfficientAdapter(final Context context) {
			mInflater = LayoutInflater.from(context);
		}

		public int getCount() {
			return FileBrowser.folderList.size();
		}

		public Object getItem(final int position) {
			return position;
		}

		public long getItemId(final int position) {
			return position;
		}

		public View getView(final int position, View convertView,
				final ViewGroup parent) {
			ViewHolder holder = null;

			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.fb_grid, null);
				holder = new ViewHolder();
				holder.mainText = (TextView) convertView
						.findViewById(R.id.mainText);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				convertView.setTag(holder);

			} else
				holder = (ViewHolder) convertView.getTag();

			textInfo = FileBrowser.folderList.get(position).getText();
			mIcon = FileBrowser.folderList.get(position).getIcon();
			holder.mainText.setText(textInfo);

			if (mIcon != null)
				holder.icon.setBackgroundDrawable(mIcon);
			else
				holder.icon.setBackgroundResource(R.drawable.unknown);
			return convertView;

		}

		static class ViewHolder {
			TextView mainText;
			ImageView icon;
		}
	}
}
