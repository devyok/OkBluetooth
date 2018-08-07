package com.devyok.bluetooth.base;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
/**
 * @author wei.deng
 */
public final class TaskQueue extends HandlerThread{
    private Handler mHandler;
    
    final ConcurrentHashMap<Runnable, TaskWrapper> taskMapping = new ConcurrentHashMap<Runnable, TaskWrapper>();
    
    public TaskQueue(String name) {
        this(name,null);
    }
    
    public TaskQueue(String name,Callback callback) {
        super(name, android.os.Process.THREAD_PRIORITY_DEFAULT);
        ensureThreadLocked(callback);
    }

    void ensureThreadLocked(Callback callback) {
        start();
        mHandler = new Handler(getLooper(),callback);
    }
    
    void remove(TaskWrapper taskWrapper){
    	getHandler().removeCallbacks(taskWrapper);
    	if(taskWrapper.taskImpl!=null){
    		taskMapping.remove(taskWrapper.taskImpl);
        	taskWrapper.taskImpl = null;
    	}
    }

    private Handler getHandler() {
        return mHandler;
    }
    
    public boolean contains(Runnable task){
    	return taskMapping.contains(task);
    }
    
    public void removeTasks(Runnable ...tasks){
    	
    	for (int i = 0; i < tasks.length; i++) {
			Runnable task = tasks[i];
			if(task == null) continue;
			TaskWrapper taskWrapper = taskMapping.remove(task);
			if(taskWrapper!=null){
				taskWrapper.taskImpl = null;
				getHandler().removeCallbacks(taskWrapper);
			}
		}
    	
    }
    
    public void removeAllTasks(){
    	
    	for(Iterator<Map.Entry<Runnable, TaskWrapper>> iter = taskMapping.entrySet().iterator();iter.hasNext();){
    		
    		Map.Entry<Runnable, TaskWrapper> item = iter.next();
    		
    		TaskWrapper taskWrapper = item.getValue();
    		taskWrapper.taskImpl = null;
    		getHandler().removeCallbacks(taskWrapper);
    		
    	}
    	
    	taskMapping.clear();
    	
    }
    
    public void submitTask(Runnable task){
    	TaskWrapper taskWrapper = TaskWrapper.create(task, this);
    	taskMapping.put(task, taskWrapper);
    	getHandler().post(taskWrapper);
    }
    
    public void submitTask(Runnable task,long delayMillis){
    	TaskWrapper taskWrapper = TaskWrapper.create(task, this);
    	taskMapping.put(task, taskWrapper);
    	getHandler().postDelayed(taskWrapper, delayMillis);
    }
    
    final static class TaskWrapper implements Runnable {
    	
    	Runnable taskImpl;
    	TaskQueue taskQueue;
    	
    	private TaskWrapper(Runnable runnable,TaskQueue queue){
    		taskImpl = runnable;
    		this.taskQueue = queue;
    	}
    	
    	public static TaskWrapper create(Runnable runnable,TaskQueue queue){
    		TaskWrapper taskWrapper = new TaskWrapper(runnable,queue);
    		return taskWrapper;
    	}

		@Override
		public void run() {
			
			try {
				if(taskImpl!=null){
					taskImpl.run();
				}
			} finally {
				taskQueue.remove(this);
			}
			
		}
    }

	public int getTaskCount() {
		return taskMapping.size();
	}
    
}
