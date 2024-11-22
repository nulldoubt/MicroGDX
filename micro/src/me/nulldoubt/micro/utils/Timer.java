package me.nulldoubt.micro.utils;

import me.nulldoubt.micro.Application;
import me.nulldoubt.micro.Files;
import me.nulldoubt.micro.LifecycleListener;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.utils.collections.Array;

public class Timer {
	
	static final Object threadLock = new Object();
	static TimerThread thread;
	
	public static Timer instance() {
		synchronized (threadLock) {
			TimerThread thread = thread();
			if (thread.instance == null)
				thread.instance = new Timer();
			return thread.instance;
		}
	}
	
	private static TimerThread thread() {
		synchronized (threadLock) {
			if (thread == null || thread.files != Micro.files) {
				if (thread != null)
					thread.dispose();
				thread = new TimerThread();
			}
			return thread;
		}
	}
	
	final Array<Task> tasks = new Array<>(false, 8);
	long stopTimeMillis;
	
	public Timer() {
		start();
	}
	
	public Task postTask(Task task) {
		return scheduleTask(task, 0, 0, 0);
	}
	
	public Task scheduleTask(Task task, float delaySeconds) {
		return scheduleTask(task, delaySeconds, 0, 0);
	}
	
	public Task scheduleTask(Task task, float delaySeconds, float intervalSeconds) {
		return scheduleTask(task, delaySeconds, intervalSeconds, -1);
	}
	
	public Task scheduleTask(Task task, float delaySeconds, float intervalSeconds, int repeatCount) {
		synchronized (threadLock) {
			synchronized (this) {
				synchronized (task) {
					if (task.timer != null)
						throw new IllegalArgumentException("The same task may not be scheduled twice.");
					task.timer = this;
					long timeMillis = System.nanoTime() / 1000000;
					long executeTimeMillis = timeMillis + (long) (delaySeconds * 1000);
					if (thread.pauseTimeMillis > 0)
						executeTimeMillis -= timeMillis - thread.pauseTimeMillis;
					task.executeTimeMillis = executeTimeMillis;
					task.intervalMillis = (long) (intervalSeconds * 1000);
					task.repeatCount = repeatCount;
					tasks.add(task);
				}
			}
			threadLock.notifyAll();
		}
		return task;
	}
	
	public void stop() {
		synchronized (threadLock) {
			if (thread().instances.removeValue(this, true))
				stopTimeMillis = System.nanoTime() / 1000000;
		}
	}
	
	public void start() {
		synchronized (threadLock) {
			TimerThread thread = thread();
			Array<Timer> instances = thread.instances;
			if (instances.contains(this, true))
				return;
			instances.add(this);
			if (stopTimeMillis > 0) {
				delay(System.nanoTime() / 1000000 - stopTimeMillis);
				stopTimeMillis = 0;
			}
			threadLock.notifyAll();
		}
	}
	
	public void clear() {
		synchronized (threadLock) {
			final TimerThread thread = thread();
			synchronized (this) {
				synchronized (thread.postedTasks) {
					for (int i = 0, n = tasks.size; i < n; i++) {
						Task task = tasks.get(i);
						thread.removePostedTask(task);
						task.reset();
					}
				}
				tasks.clear();
			}
		}
	}
	
	public synchronized boolean isEmpty() {
		return tasks.size == 0;
	}
	
	synchronized long update(final TimerThread thread, final long timeMillis, long waitMillis) {
		for (int i = 0, n = tasks.size; i < n; i++) {
			Task task = tasks.get(i);
			synchronized (task) {
				if (task.executeTimeMillis > timeMillis) {
					waitMillis = Math.min(waitMillis, task.executeTimeMillis - timeMillis);
					continue;
				}
				if (task.repeatCount == 0) {
					task.timer = null;
					tasks.removeIndex(i);
					i--;
					n--;
				} else {
					task.executeTimeMillis = timeMillis + task.intervalMillis;
					waitMillis = Math.min(waitMillis, task.intervalMillis);
					if (task.repeatCount > 0)
						task.repeatCount--;
				}
				thread.addPostedTask(task);
			}
		}
		return waitMillis;
	}
	
	public synchronized void delay(final long delayMillis) {
		for (int i = 0, n = tasks.size; i < n; i++) {
			Task task = tasks.get(i);
			synchronized (task) {
				task.executeTimeMillis += delayMillis;
			}
		}
	}
	
	public static Task post(final Task task) {
		return instance().postTask(task);
	}
	
	public static Task schedule(final Task task, final float delaySeconds) {
		return instance().scheduleTask(task, delaySeconds);
	}
	
	public static Task schedule(final Task task, final float delaySeconds, final float intervalSeconds) {
		return instance().scheduleTask(task, delaySeconds, intervalSeconds);
	}
	
	public static Task schedule(final Task task, final float delaySeconds, final float intervalSeconds, final int repeatCount) {
		return instance().scheduleTask(task, delaySeconds, intervalSeconds, repeatCount);
	}
	
	public static abstract class Task implements Runnable {
		
		private final Application application;
		private long executeTimeMillis, intervalMillis;
		private int repeatCount;
		private volatile Timer timer;
		
		public Task() {
			application = Micro.app; // Store which app to postRunnable (eg for multiple LwjglAWTCanvas).
			if (application == null)
				throw new IllegalStateException("Gdx.app not available.");
		}
		
		public abstract void run();
		
		public void cancel() {
			synchronized (threadLock) {
				thread().removePostedTask(this);
				Timer timer = this.timer;
				if (timer != null) {
					synchronized (timer) {
						timer.tasks.removeValue(this, true);
						reset();
					}
				} else
					reset();
			}
		}
		
		synchronized void reset() {
			executeTimeMillis = 0;
			this.timer = null;
		}
		
		public boolean isScheduled() {
			return timer != null;
		}
		
		public synchronized long getExecuteTimeMillis() {
			return executeTimeMillis;
		}
		
	}
	
	static class TimerThread implements Runnable, LifecycleListener {
		
		private final Files files;
		private final Application application;
		private final Array<Timer> instances = new Array<>(1);
		private Timer instance;
		private long pauseTimeMillis;
		
		private final Array<Task> postedTasks = new Array<>(2);
		private final Array<Task> runTasks = new Array<>(2);
		private final Runnable runPostedTasks = this::runPostedTasks;
		
		public TimerThread() {
			files = Micro.files;
			application = Micro.app;
			application.register(this);
			resume();
			
			final Thread thread = new Thread(this, "Timer");
			thread.setDaemon(true);
			thread.start();
		}
		
		public void run() {
			while (true) {
				synchronized (threadLock) {
					if (thread != this || files != Micro.files)
						break;
					
					long waitMillis = 5000;
					if (pauseTimeMillis == 0) {
						long timeMillis = System.nanoTime() / 1000000;
						for (int i = 0, n = instances.size; i < n; i++) {
							try {
								waitMillis = instances.get(i).update(this, timeMillis, waitMillis);
							} catch (Throwable ex) {
								throw new MicroRuntimeException("Task failed: " + instances.get(i).getClass().getName(), ex);
							}
						}
					}
					
					if (thread != this || files != Micro.files)
						break;
					
					try {
						if (waitMillis > 0)
							threadLock.wait(waitMillis);
					} catch (InterruptedException _) {}
				}
			}
			dispose();
		}
		
		void runPostedTasks() {
			synchronized (postedTasks) {
				runTasks.addAll(postedTasks);
				postedTasks.clear();
			}
			Object[] items = runTasks.items;
			for (int i = 0, n = runTasks.size; i < n; i++)
				((Task) items[i]).run();
			runTasks.clear();
		}
		
		void addPostedTask(Task task) {
			synchronized (postedTasks) {
				if (postedTasks.isEmpty())
					task.application.post(runPostedTasks);
				postedTasks.add(task);
			}
		}
		
		void removePostedTask(Task task) {
			synchronized (postedTasks) {
				Object[] items = postedTasks.items;
				for (int i = postedTasks.size - 1; i >= 0; i--)
					if (items[i] == task)
						postedTasks.removeIndex(i);
			}
		}
		
		public void resume() {
			synchronized (threadLock) {
				long delayMillis = System.nanoTime() / 1000000 - pauseTimeMillis;
				for (int i = 0, n = instances.size; i < n; i++)
					instances.get(i).delay(delayMillis);
				pauseTimeMillis = 0;
				threadLock.notifyAll();
			}
		}
		
		public void pause() {
			synchronized (threadLock) {
				pauseTimeMillis = System.nanoTime() / 1000000;
				threadLock.notifyAll();
			}
		}
		
		public void dispose() { // OK to call multiple times.
			synchronized (threadLock) {
				synchronized (postedTasks) {
					postedTasks.clear();
				}
				if (thread == this)
					thread = null;
				instances.clear();
				threadLock.notifyAll();
			}
			application.unregister(this);
		}
		
	}
	
}
