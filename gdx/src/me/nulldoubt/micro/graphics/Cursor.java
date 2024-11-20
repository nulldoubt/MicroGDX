package me.nulldoubt.micro.graphics;

import me.nulldoubt.micro.utils.Disposable;

public interface Cursor extends Disposable {
	
	enum SystemCursor {
		Arrow, Ibeam, Crosshair, Hand, HorizontalResize, VerticalResize, NWSEResize, NESWResize, AllResize, NotAllowed, None
	}
	
}
