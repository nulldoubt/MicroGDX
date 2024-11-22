package me.nulldoubt.micro.backends.android;

import android.view.PointerIcon;
import android.view.View;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.Cursor;

public class AndroidCursor implements Cursor {
	
	static void setSystemCursor(View view, SystemCursor systemCursor) {
		int type = switch (systemCursor) {
			case Arrow -> PointerIcon.TYPE_DEFAULT;
			case Ibeam -> PointerIcon.TYPE_TEXT;
			case Crosshair -> PointerIcon.TYPE_CROSSHAIR;
			case Hand -> PointerIcon.TYPE_HAND;
			case HorizontalResize -> PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW;
			case VerticalResize -> PointerIcon.TYPE_VERTICAL_DOUBLE_ARROW;
			case NWSEResize -> PointerIcon.TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW;
			case NESWResize -> PointerIcon.TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW;
			case AllResize -> PointerIcon.TYPE_ALL_SCROLL;
			case NotAllowed -> PointerIcon.TYPE_NO_DROP;
			case None -> PointerIcon.TYPE_NULL;
			default -> throw new MicroRuntimeException("Unknown system cursor " + systemCursor);
		};
		view.setPointerIcon(PointerIcon.getSystemIcon(view.getContext(), type));
	}
	
	@Override
	public void dispose() {}
	
}
