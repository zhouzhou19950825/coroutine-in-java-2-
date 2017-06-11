package com.upic.coroutine.workthread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.RecursiveTask;

import com.upic.coroutine.UpicContinuation;
import com.upic.coroutine.UpicContinuationContext;
import com.upic.coroutine.enums.StateEnum;
import com.upic.coroutine.pool.CommonPool;
import com.upic.coroutine.start.StandaloneCoroutine;
import com.upic.uploadTest.UploadPath;
import com.upic.workQueue.Constant;

/**
 * Task运行环境
 * 
 * @author DTZ
 *
 */
public class UploadTask extends RecursiveTask<List<String>> {
	private List<String> result;
	private UpicContinuationContext continuationContext;
	private CommonPool commonPool;
//	public static UploadTask u;
//	public volatile boolean flag=true;
//	private List<RecursiveTask<List<String>>> mTasks;
	public UploadTask(CommonPool commonPool, UpicContinuationContext continuationContext) {
		this.commonPool = commonPool;
		this.continuationContext = continuationContext;
	}

	@Override
	protected List<String> compute() {
		result = new ArrayList<>();
		UpicContinuation interceptContinuation = null;
		if (continuationContext != null && this.continuationContext.state().equals(StateEnum.RUN)) {
			try {
				interceptContinuation = commonPool.interceptContinuation(new StandaloneCoroutine(continuationContext));
				String uploadFile = uploadFile(((UploadPath) interceptContinuation.getContext()).getPath());
				interceptContinuation.resume(uploadFile);
				System.out.println(Thread.currentThread().getName() + ":上传文件地址:"
						+ ((UploadPath) interceptContinuation.getContext()).getPath() + " :用时:" + uploadFile);
				result.add(uploadFile);
				// throw new Exception(); //测试异常时异常返回
			} catch (Exception e) {
				interceptContinuation.resumeWithException(e);
			}
		}
//		else{
//			while(flag){
//				
//			}
//			mTasks.add(u);
//			flag=true;
//			invokeAll(mTasks);
//		}
		return result;
	}

	// 模拟上传时间
	public static String uploadFile(String path) {
		System.out.println("upload to：" + path);
		// 暂时用这个模拟耗时
		long needTime = (long) ((Math.random() + 1) * 2.5 * 1000);
		try {
			Thread.sleep(needTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// 假设返回上传所需时间
		return needTime + "";
	}

	public UpicContinuationContext getContinuationContext() {
		return continuationContext;
	}

//	public void setmTasks(List<RecursiveTask<List<String>>> mTasks) {
//		this.mTasks = mTasks;
//	}

}