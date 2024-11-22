package me.nulldoubt.micro.backends.android;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import me.nulldoubt.micro.Application;
import me.nulldoubt.micro.LifecycleListener;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.SnapshotArray;

public interface AndroidApplicationBase extends Application {
	
	int MINIMUM_SDK = 24;
	
	Context getContext();
	
	Array<Runnable> getRunnables();
	
	Array<Runnable> getExecutedRunnables();
	
	void runOnUiThread(Runnable runnable);
	
	void startActivity(Intent intent);
	
	@Override
	AndroidInput getInput();
	
	SnapshotArray<LifecycleListener> getLifecycleListeners();
	
	Window getApplicationWindow();
	
	WindowManager getWindowManager();
	
	void useImmersiveMode(boolean b);
	
	Handler getHandler();
	
	AndroidAudio createAudio(Context context, AndroidApplicationConfiguration config);
	
	AndroidInput createInput(Application activity, Context context, Object view, AndroidApplicationConfiguration config);
	
}
