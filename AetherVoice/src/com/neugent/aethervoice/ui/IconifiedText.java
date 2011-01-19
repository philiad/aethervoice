package com.neugent.aethervoice.ui;

import android.graphics.drawable.Drawable;

public class IconifiedText implements Comparable<IconifiedText> {

	private String mText = "";
	private Drawable mIcon;
	private boolean mSelectable = true;

	public IconifiedText(final String text, final Drawable bullet) {
		mIcon = bullet;
		mText = text;
	}

	public boolean isSelectable() {
		return mSelectable;
	}

	public void setSelectable(final boolean selectable) {
		mSelectable = selectable;
	}

	public String getText() {
		return mText;
	}

	public void setText(final String text) {
		mText = text;
	}

	public void setIcon(final Drawable icon) {
		mIcon = icon;
	}

	public Drawable getIcon() {
		return mIcon;
	}

	public int compareTo(final IconifiedText other) {
		if (this.mText != null)
			return this.mText.compareTo(other.getText());
		else
			throw new IllegalArgumentException();
	}
}
