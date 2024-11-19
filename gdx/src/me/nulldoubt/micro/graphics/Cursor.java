
package me.nulldoubt.micro.graphics;

import me.nulldoubt.micro.Graphics;
import me.nulldoubt.micro.utils.Disposable;

/**
 * <p>
 * Represents a mouse cursor. Create a cursor via {@link Graphics#newCursor(Pixmap, int, int)}. To set the cursor use
 * {@link Graphics#setCursor(Cursor)}. To use one of the system cursors, call Graphics#setSystemCursor
 * </p>
 **/
public interface Cursor extends Disposable {

	public static enum SystemCursor {
		Arrow, Ibeam, Crosshair, Hand, HorizontalResize, VerticalResize, NWSEResize, NESWResize, AllResize, NotAllowed, None
	}
}
