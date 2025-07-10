package at.bitfire.gfxtablet;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NetEvent {
	static final String signature = "GfxTablet";
	static final short protocol_version = 2;
	final Type type;
	short x, y, pressure;
	byte button, button_down;

	public NetEvent(Type type) {
		this.type = type;
	}


	public NetEvent(Type type, short x, short y, short pressure) {
		this.type = type;
		this.x = x;
		this.y = y;
		this.pressure = pressure;
	}

	public NetEvent(Type type, short x, short y, short pressure, int button, boolean button_down) {
		this(type, x, y, pressure);
		this.button = (byte) button;
		this.button_down = (byte) (button_down ? 1 : 0);
	}

	public byte[] toByteArray() {
		if (type == Type.TYPE_DISCONNECT)
			return null;

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

		try {
			dataOutputStream.writeBytes(signature);
			dataOutputStream.writeShort(protocol_version);

			switch (type) {
				case TYPE_MOTION:
					dataOutputStream.writeByte(0);
					break;
				case TYPE_BUTTON:
					dataOutputStream.writeByte(1);
					break;
				default:
			}

			dataOutputStream.writeShort(x);
			dataOutputStream.writeShort(y);
			dataOutputStream.writeShort(pressure);

			if (type == Type.TYPE_BUTTON) {
				dataOutputStream.writeByte(button);
				dataOutputStream.writeByte(button_down);
			}
		} catch (IOException e) {
			Log.wtf(signature, "Couldn't generate network packet");
		}

		return byteArrayOutputStream.toByteArray();
	}

	enum Type {
		TYPE_MOTION,
		TYPE_BUTTON,

		// not specified in protocol, only needed to shut network thread down.
		TYPE_DISCONNECT
	}
}
