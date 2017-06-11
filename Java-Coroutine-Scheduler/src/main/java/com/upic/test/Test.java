package com.upic.test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.upic.coroutine.enums.StateEnum;
import com.upic.coroutine.pool.CommonPool;
import com.upic.coroutine.scheduler.Scheduler;
import com.upic.uploadTest.UploadPath;
/**
 * 测试类
 * @author DTZ
 *
 */
public class Test {
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		//启动在做封装，异步启动
		System.out.println("开始上传");
		CommonPool commonPool = CommonPool.getCommonPool();
		UploadPath uploadPath = new UploadPath("www.upic123.com");
		//设置状态让步
		uploadPath.setState(StateEnum.YIELD);
		//启动协程 并且设置并行处理个数
		Scheduler scheduler = Scheduler.getScheduler(2, commonPool);
		scheduler.addTask(uploadPath);
		scheduler.addTask(new UploadPath("www.upic1234.com"));
		scheduler.addTask(new UploadPath("www.upic12345.com"));
		scheduler.addTask(new UploadPath("www.upic12346.com"));
		//启动协程
		scheduler.workingLoop();
		System.out.println("正在上传...");
		//改变状态
		uploadPath.setState(StateEnum.RUN);
		scheduler.setSchedule(uploadPath);
		CommonPool.JOINPOOL.awaitTermination(8000, TimeUnit.MILLISECONDS);
		CommonPool.JOINPOOL.shutdown();

	}
}
