package com.upic.coroutine.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import com.upic.coroutine.AbstractUpicContinuationContext;
import com.upic.coroutine.UpicContinuationContext;
import com.upic.coroutine.enums.StateEnum;
import com.upic.coroutine.pool.CommonPool;
import com.upic.coroutine.workthread.UploadTask;
import com.upic.workQueue.WorkQueue;

public class Scheduler {
	// 默认处理任务数为1
	private int dealTaskNum = 1;
	// 所有的任务
	private Map<Integer, RecursiveTask<List<String>>> map;
	// 并行处理的task
	private List<RecursiveTask<List<String>>> mTasks;
	private static Object obj = new Object();
	private static Scheduler scheduler;
	// 工作队列
	private WorkQueue<RecursiveTask<List<String>>> work;
	private boolean flag = true;

	private CommonPool commonPool;

	private Scheduler() {
		map = new HashMap<Integer, RecursiveTask<List<String>>>();
		work = new WorkQueue<>(10);
		mTasks = new ArrayList<>();
	}

	private Scheduler(int num, CommonPool commonPool) {
		this();
		this.commonPool = commonPool;
		dealTaskNum = num;
	}

	public static Scheduler getScheduler(int dealTakNum, CommonPool commonPool) {
		synchronized (obj) {
			if (scheduler == null) {
				scheduler = new Scheduler(dealTakNum, commonPool);
			}
		}
		return scheduler;
	}

	// 添加任务
	public int addTask(UpicContinuationContext context) {
		UploadTask uploadTask = new UploadTask(commonPool, context);
		map.put(context.getId(), uploadTask);

		if (((AbstractUpicContinuationContext) context).state().equals(StateEnum.RUN)) {
			work.put(uploadTask);
		}
		return context.getId();
	}

	// 添加任务队列
	public int setSchedule(UpicContinuationContext context) {
		UploadTask uploadTask;
		uploadTask =map.get(context.getId())==null? new UploadTask(commonPool, context):(UploadTask) map.get(context.getId());

		if (((AbstractUpicContinuationContext) context).state().equals(StateEnum.RUN)) {
			work.put(uploadTask);
		}
		return context.getId();
	}

	// 从map中移除
	public UploadTask exit(UploadTask uploadTask) {
		map.remove(uploadTask.getContinuationContext().getId());
		return uploadTask;
	}

	// 启动loop
	public void workingLoop() {
		Thread thread = new Thread(() -> {
			while (flag) {
				if (work.size() == 0) {
					continue;
				}
				mTasks.clear();
				for (int i = 0; i < dealTaskNum; i++) {
					CommonPool.JOINPOOL.execute(work.get());
				}
			}
		}, "loopThread");
		thread.setDaemon(true);
		CommonPool.JOINPOOL.submit(thread);
	}

	public boolean isFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

}
