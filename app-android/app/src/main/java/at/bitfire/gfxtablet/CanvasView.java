package at.bitfire.gfxtablet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import at.bitfire.gfxtablet.NetEvent.Type;

@SuppressLint("ViewConstructor")
public class CanvasView extends View implements SharedPreferences.OnSharedPreferenceChangeListener {
	private static final String TAG = "GfxTablet.CanvasView";
	private static final int SPEN_ACTION_DOWN = 211;
	private static final int SPEN_ACTION_UP = 212;
	private static final int SPEN_ACTION_MOVE = 213;
	private static final int SPEN_ACTION_CANCEL = 214;
	private static final int SPEN_BUTTON = MotionEvent.BUTTON_SECONDARY;
	final SharedPreferences settings;
	NetworkClient netClient;
	boolean acceptStylusOnly;
	int maxX, maxY;
	InRangeStatus inRangeStatus;
	private boolean spenButtonCurrentlyHeld = false;

	public CanvasView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);

		// view is disabled until a network client is set
		setEnabled(false);

		settings = PreferenceManager.getDefaultSharedPreferences(context);
		settings.registerOnSharedPreferenceChangeListener(this);
		setBackground();
		setInputMethods();
		inRangeStatus = InRangeStatus.OutOfRange;
	}


	// setup

	public void setNetworkClient(NetworkClient networkClient) {
		netClient = networkClient;
		setEnabled(true);
	}

	protected void setBackground() {
		if (settings.getBoolean(SettingsActivity.KEY_DARK_CANVAS, false))
			setBackgroundColor(Color.BLACK);
		else
			setBackgroundResource(R.drawable.bg_grid_pattern);
	}


	// settings

	protected void setInputMethods() {
		acceptStylusOnly = settings.getBoolean(SettingsActivity.KEY_PREF_STYLUS_ONLY, false);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		switch (key) {
			case SettingsActivity.KEY_PREF_STYLUS_ONLY:
				setInputMethods();
				break;
			case SettingsActivity.KEY_DARK_CANVAS:
				setBackground();
				break;
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldW, int oldH) {
		Log.i(TAG, "Canvas size changed: " + w + "x" + h + " (before: " + oldW + "x" + oldH + ")");
		maxX = w;
		maxY = h;
	}


	// drawing

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (isEnabled()) {
			for (int ptr = 0; ptr < event.getPointerCount(); ptr++)
				if (!acceptStylusOnly || (event.getToolType(ptr) == MotionEvent.TOOL_TYPE_STYLUS)) {
					short nx = normalizeX(event.getX(ptr)),
						ny = normalizeY(event.getY(ptr)),
						nPressure = normalizePressure(event.getPressure(ptr));
					boolean buttonState = (event.getButtonState() & SPEN_BUTTON) != 0;
					Log.v(TAG, String.format("Generic motion event logged: action %d with buttons %d @ %f|%f (pressure %f)", event.getActionMasked(), buttonState ? 1 : 0, event.getX(ptr), event.getY(ptr), event.getPressure(ptr)));
					switch (event.getActionMasked()) {
						case SPEN_ACTION_MOVE:
						case MotionEvent.ACTION_HOVER_MOVE:
							if (spenButtonCurrentlyHeld == buttonState) {
								netClient.getQueue().add(new NetEvent(Type.TYPE_MOTION, nx, ny, nPressure));
							} else {
								netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, nPressure, buttonState ? 1 : -1, true));
								spenButtonCurrentlyHeld = buttonState;
							}
							break;
						case MotionEvent.ACTION_HOVER_ENTER:
							inRangeStatus = InRangeStatus.InRange;
							netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, nPressure, buttonState ? 1 : -1, true));
							break;
						case MotionEvent.ACTION_HOVER_EXIT:
							inRangeStatus = InRangeStatus.OutOfRange;
							netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, nPressure, buttonState ? 1 : -1, false));
							break;
					}
				}
			return true;
		}
		return false;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		if (isEnabled()) {
			for (int ptr = 0; ptr < event.getPointerCount(); ptr++)
				if (!acceptStylusOnly || (event.getToolType(ptr) == MotionEvent.TOOL_TYPE_STYLUS)) {
					short nx = normalizeX(event.getX(ptr)),
						ny = normalizeY(event.getY(ptr)),
						nPressure = normalizePressure(event.getPressure(ptr));
					boolean buttonState = (event.getButtonState() & SPEN_BUTTON) != 0;
					Log.v(TAG, String.format("Touch event logged: action %d with buttons %d @ %f|%f (pressure %f)", event.getActionMasked(), buttonState ? 1 : 0, event.getX(ptr), event.getY(ptr), event.getPressure(ptr)));
					switch (event.getActionMasked()) {
						case SPEN_ACTION_MOVE:
						case MotionEvent.ACTION_MOVE:
							if (spenButtonCurrentlyHeld == buttonState) {
								netClient.getQueue().add(new NetEvent(Type.TYPE_MOTION, nx, ny, nPressure));
							} else {
								netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, nPressure, buttonState ? 1 : -1, true));
								spenButtonCurrentlyHeld = buttonState;
							}
							break;
						case SPEN_ACTION_DOWN:
						case MotionEvent.ACTION_DOWN:
							if (inRangeStatus == InRangeStatus.OutOfRange) {
								inRangeStatus = InRangeStatus.FakeInRange;
								netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, (short) 0, buttonState ? 1 : -1, true));
							}
							netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, nPressure, buttonState ? 1 : 0, true));
							break;
						case SPEN_ACTION_UP:
						case MotionEvent.ACTION_UP:
						case SPEN_ACTION_CANCEL:
						case MotionEvent.ACTION_CANCEL:
							netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, nPressure, buttonState ? 1 : 0, false));
							if (inRangeStatus == InRangeStatus.FakeInRange) {
								inRangeStatus = InRangeStatus.OutOfRange;
								netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, (short) 0, buttonState ? 1 : -1, false));
							}
							break;
					}

				}
			return true;
		}
		return false;
	}

	// these overflow and wrap around to negative short values, but thankfully Java will continue
	// on regardless, so we can just ignore Java’s interpretation of them and send them anyway.
	short normalizeX(float x) {
		return (short) (Math.min(Math.max(0, x), maxX) * 2 * Short.MAX_VALUE / maxX);
	}

	short normalizeY(float x) {
		return (short) (Math.min(Math.max(0, x), maxY) * 2 * Short.MAX_VALUE / maxY);
	}

	short normalizePressure(float x) {
		return (short) (Math.min(Math.max(0, x), 2.0) * Short.MAX_VALUE);
	}

	private enum InRangeStatus {
		OutOfRange,
		InRange,
		FakeInRange
	}

}
