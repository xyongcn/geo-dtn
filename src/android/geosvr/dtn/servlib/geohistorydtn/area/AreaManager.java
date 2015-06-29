package android.geosvr.dtn.servlib.geohistorydtn.area;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.util.Log;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-5-14 上午9:16:09 
 * 说明 ：对所有的Area进行管理，分配和超出容量删除机制
 */
public class AreaManager 
{
	private static String tag="AreaManager";
	
	private static class SingleAreaManager
	{
		static AreaManager instance=new AreaManager();
	}
	
	//线程安全单例模式
	public static AreaManager getInstance()
	{
		return SingleAreaManager.instance;
	}
	
	/**
	 * 默认构造函数
	 */
	public AreaManager()
	{
		init();
	}
	
	private void init()	
	{
		areaMap=new HashMap<String, Area>(500);
		
		//从文件中读取历史的区域信息
		try
		{
			File file=new File("/sdcard/geoHistory_dtn/historyarea");
			if(file.exists())
			{
				Log.i(tag,"从文件historyarea中读取历史的区域信息");
				ObjectInputStream in=new ObjectInputStream(new FileInputStream(file));
				Object obj=null;
				while((obj=in.readObject())!=null)
				{
					if(obj instanceof Area)
					{
						Area area=(Area)obj;
						String s=area.level+"#"+area.id;
						areaMap.put(s, area);
					}
				}
				in.close();
			}
			/*for(Area area:areaMap.values())
			{
				out.writeObject(area);
			}
			out.close();*/
		}
		catch(IOException e)
		{
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 用来存放所有的Area的hashmap
	 */
	HashMap<String,Area> areaMap;
	
	/**
	 * 检查这批新的区域并且修改相应的父类引用
	 * @param areainfo:所在各个层次信息
	 * @return :最底层的区域的引用，如果区域信息不合法会返回Null
	 */
	public Area checkAreaInfo(AreaInfo areainfo)
	{
		int[] areaid=areainfo.getAreaId();
		
		int start=areainfo.getBaseAreaNum();
		int end=areaid.length;
		List<Area> arealist=new ArrayList<Area>(end);
		
		boolean valid=true;
		int level=areainfo.getBaseAreaLevel();
		for(int i=start;i<end;i++)
		{
			int id=areaid[i];
			if(AreaLevel.isLevelValid(level))
			{
				//生成hashmap的标识字符串
				String s=String.valueOf(level)+"#"+String.valueOf(id);
				Area a=areaMap.get(s);
				if(a==null)
				{
					a=new Area(level, id);
					areaMap.put(s, a);
					
//					Log.i(tag,"add new area:"+a.toString());
				}
				
				arealist.add(a);
			}
			//如果存在不合格的level，那么久不需要
			else
			{
				valid=false;
				break;
			}
			
			//将区域层级+1
			++level;
		}
		
		//修改相关父类的引用
		if(valid)
		{
			for(int i=0;i<arealist.size()-1;i++)
			{
				arealist.get(i).setFatherArea(arealist.get(i+1));
			}
			return arealist.get(0);
		}
		else
			return null;
	}
	
	//将内存中的area相关信息写到sdcard中
//	FileOutputStream geohistoryarea=null;
	public void wrieteAreaInfoToFile()
	{
		(new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try
				{
					File file=new File("/sdcard/geoHistory_dtn/historyarea");
					file.getParentFile().mkdirs();
					if(!file.exists())
						file.createNewFile();
					
					ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(file, false));
					for(Area area:areaMap.values())
					{
						out.writeObject(area);
					}
					out.flush();
					out.close();
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}

			}
		})).start();
		
	}
	
	//将移动轨迹的area保存到log中
	static BufferedOutputStream areamovingLog=null;
	/**
	 * 将移动轨迹的area保存到log中
	 * @param reason 原因前缀
	 * @param area 新移动的区域
	 */
	public static void writeAreaLogToFile(String reason,Area area)
	{
		try
		{
			if(areamovingLog==null)
			{
				File file=new File("/sdcard/dtn_test_data/areamoving.log");
				file.getParentFile().mkdir();
				if(!file.exists())
					file.createNewFile();
				
				areamovingLog=new BufferedOutputStream(new FileOutputStream(file, false));
			}
			String temp;
			if(area!=null)
				temp=reason +area.toString();
			else
				temp=reason+"null";
			areamovingLog.write(temp.getBytes());
			areamovingLog.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	/**
	 * 计时器时间发生变化将写入日志中
	 * @param time
	 */
	public static void writeAreaTimeChange2Log(String time)
	{
		try
		{
			if(areamovingLog==null)
			{
				File file=new File("/sdcard/dtn_test_data/areamoving.log");
				file.getParentFile().mkdir();
				if(!file.exists())
					file.createNewFile();
				
				areamovingLog=new BufferedOutputStream(new FileOutputStream(file, false));
			}
			
			areamovingLog.write(time.getBytes());
			areamovingLog.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	//获取位置以及改变位置的流程
	//1.首先由专门获取位置的程序负责获取当前的位置，然后再通过相关程序得到相应的区域编号；一种模拟环境中是直接得到相应的区域编号。
	//2.调用GeoHistoryRouter里面的changeLocation,
	//3.由GeoHistroyRouter来判断是否进行改变区域的事件处理以及调用AreaManager里面对新区域的处理
	//
	//
}
