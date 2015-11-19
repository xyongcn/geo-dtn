package android.geosvr.dtn.servlib.geohistorydtn.log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import android.geosvr.dtn.DTNService;
import android.util.Log;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-6-29 下午1:19:16 
 * 说明 
 */
public class GeohistoryLog implements Runnable
{
	public static String logfile="/sdcard/geoHistory_dtn/GeohistoryLog.txt";
	private static String tag="GeohistoryLog";
	
	BlockingQueue<String> queue;//写入日志文件内容的队列
	BufferedOutputStream output=null;
	
	boolean isrunning=false;//线程是否运行
	boolean shutdown=false;//是否需要关闭线程
	
	private static class SingleGeohistoryLog
	{
		static GeohistoryLog Instance=new GeohistoryLog();
	}
	
	public static GeohistoryLog getInstance()
	{
		GeohistoryLog ins= SingleGeohistoryLog.Instance;
		
		//确保写入日志的线程始终是正常运行的
		if(!ins.isrunning){
			(new Thread(ins)).start();
		}
		return ins;
	}
	
	private GeohistoryLog()
	{
		queue=new LinkedBlockingDeque<String>();
		if(output==null){
			output=getOutputStream();
		}
	}
	
	//获取对日志文件读写的流
	private BufferedOutputStream getOutputStream()
	{
		BufferedOutputStream out=null;
		
		File file=new File(logfile);
		if(!file.exists())
		{
			try {
				if(!file.getParentFile().isDirectory())
					file.getParentFile().mkdirs();
				
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		try {
			out=new BufferedOutputStream(new FileOutputStream(file, false));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return out;
	}
	
	@Override
	public void run() {
		isrunning=true;
		shutdown=false;//重置关闭的标志位
		while(true)
		{
			//根据DTN服务
			/*if(!DTNService.is_running())
			{
				break;
			}*/
			if(shutdown)
				break;
			
			try {
				String line=queue.take();
				if(!line.equals(""))
				{
					if(output!=null)
					{
						try {
							output.write(line.getBytes());
							output.flush();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			if(output!=null)
				output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		output=null;
		isrunning=false;
	}
	
	public void shutdown(){
		shutdown=true;
		i(tag,"GeohistoryLog thread shutdown");
	}
	
	//加到要写入的日志队列中
	private static void putSting2Queue(String tag,String s)
	{
		try {
			getInstance().queue.put(String.format("%s\t%s\n", tag,s));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//利用Log.i的日志输出
	public static void i(String tag,String s)
	{
		Log.i(tag,s);
		putSting2Queue(tag, s);
	}
	
	public static void e(String tag,String s)
	{
		Log.e(tag,s);
		putSting2Queue(tag, s);
	}
	
	public static void w(String tag,String s)
	{
		Log.w(tag,s);
		putSting2Queue(tag, s);
	}
	
	public static void d(String tag,String s)
	{
		Log.d(tag,s);
		putSting2Queue(tag, s);
	}
	
	public static void v(String tag,String s)
	{
		Log.v(tag,s);
		putSting2Queue(tag, s);
	}
}
