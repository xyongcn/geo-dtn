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
public class GeohistoryLog extends Thread
{
	private static String logfile="/sdcard/geoHistory_dtn/GeohistoryLog.txt";
	
	BlockingQueue<String> queue;//写入日志文件内容的队列
	BufferedOutputStream output=null;
	
	
	private static class SingleGeohistoryLog
	{
		static GeohistoryLog Instance=new GeohistoryLog();
	}
	
	private static GeohistoryLog getInstance()
	{
		return SingleGeohistoryLog.Instance;
	}
	
	public GeohistoryLog()
	{
		queue=new LinkedBlockingDeque<String>();
		output=getOutputStream();
	}
	
	//获取对日志文件读写的流
	private BufferedOutputStream getOutputStream()
	{
		BufferedOutputStream out=null;
		
		File file=new File(logfile);
		if(!file.exists())
		{
			try {
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
		while(true)
		{
			if(!DTNService.is_running())
			{
				break;
			}
			
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
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		output=null;
	}
	
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
