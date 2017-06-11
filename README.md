# 加入Scheduler

我们前面通过Kotlin的协程简单实现了java中的协程，今天根据其他语言或者框架的有点来完善自己的小框架。

- **Python中简单的协程实现原理**
- **Java(Kilim)框架**
- **Kotlin**
- **如何完善？**
- **还存在什么样的缺点**

-------------------

## Python中简单的协程实现原理
>Python中实现的协程（Python原生的generator，不是greenlet这些库）
 
    1、每个要运行的成为Task(类似于线程或进程，其实是运行在这里的主协程上)。
    
    2、需要一个scheduler用来调度任务执行。
    
    3、需要一个loop，不断去读取队列queue。
    
    
 每一个task都拥有一个唯一的tid。
 
为Task定义一个run()方法，用来向generator中传递值。

下面是一副工作原理图（用edraw画的，比较难看，有啥好的工具多多推介）：
![这里写图片描述](http://img.blog.csdn.net/20170611165506566?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvcXFfMzg3MjQyOTU=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

    1、任务调度器（Scheduler）。
    2、Task。
    3、Map存储Task。
    4、queue放入准备执行的任务。


## Java(Kilim)框架
>（Kilim）好几年也不更新了


他的实现的原理图:
![这里写图片描述](http://img.blog.csdn.net/20170611165823689?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvcXFfMzg3MjQyOTU=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

1. Task
       Task 就是一个执行的任务，使用方式和Java Thread 基本相同，只是继承的为Task，覆盖的为execute 方法，启动也是调用task 的start 方法。

2. Scheduler
      Scheduler负责分派Task给指定的工作者线程WorkerThread执行，类似于ExecutorService线程池管理Runnable和Callable任务的执行。
     
3. WorkerThread
WorkerThread是执行任务Task的具体线程，循环阻塞式的从任务队列中获取下一个任务执行。    
4. RingQueue
RingQueue是一个环形队列。

5. Kilim 中通过Mailbox 来接收消息.
### Kotlin

通过线程池去操作函数的状态，Kotlin 还增加了一个关键字：suspend，用作修饰会被暂停的函数，被标记为 suspend 的函数只能运行在协程或者其他 suspend 函数当中。前两者都是通过调度器去控制和操作线程、任务，这样对线程池也做了一个良好的封装，不需要暴露太多。


### 如何完善
#### 快速启动

```
mvn compile exec:java -Dexec.mainClass=com.upic.test.Test
```
#### 说明


    1、模拟启动四个任务上传到四个服务器上。
    2、其中一个任务先设定让步。
    3、启动主协程后再设定任务状态为可运行。

    根据以上总结的经验，对自己的代码也做出了修改，增加了Scheduler类：
    
```
public class Scheduler {
	
	private int dealTaskNum = 1;
	
	private Map<Integer, RecursiveTask<List<String>>> map;
	
	private List<RecursiveTask<List<String>>> mTasks;
	private static Object obj = new Object();
	private static Scheduler scheduler;
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

	public int addTask(UpicContinuationContext context) {
		UploadTask uploadTask = new UploadTask(commonPool, context);
		map.put(context.getId(), uploadTask);

		if (((AbstractUpicContinuationContext) context).state().equals(StateEnum.RUN)) {
			work.put(uploadTask);
		}
		return context.getId();
	}

	public int setSchedule(UpicContinuationContext context) {
		UploadTask uploadTask;
		uploadTask =map.get(context.getId())==null? new UploadTask(commonPool, context):(UploadTask) map.get(context.getId());

		if (((AbstractUpicContinuationContext) context).state().equals(StateEnum.RUN)) {
			work.put(uploadTask);
		}
		return context.getId();
	}

	public UploadTask exit(UploadTask uploadTask) {
		map.remove(uploadTask.getContinuationContext().getId());
		return uploadTask;
	}

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
```
Test里的相关代码
```
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
```

    通过这个类可以更加灵活的调度我们的Task
### 还存在什么样的缺点
>目前项目中存在的缺点：

>1、调度器中需要实现任务取消。

>2、任务执行一半时候挂起，然后过一段时间继续运行

>3、需要一个TimeUtile来操控任务的时间上的调度。


后续还会不断的更新完善，也需要大家的宝贵意见，慢慢的把她完善！
