package android.geosvr.dtn.servlib.geohistorydtn.neighbour;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.geosvr.dtn.servlib.bundling.Bundle;
import android.geosvr.dtn.servlib.bundling.BundlePayload;
import android.geosvr.dtn.servlib.bundling.exception.BundleLockNotHeldByCurrentThread;
import android.geosvr.dtn.servlib.geohistorydtn.area.Area;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaLevel;
import android.geosvr.dtn.servlib.geohistorydtn.config.NeibhourConfig;
import android.geosvr.dtn.servlib.geohistorydtn.log.GeohistoryLog;
import android.geosvr.dtn.servlib.naming.EndpointID;
import android.util.Log;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-7-2 下午2:42:15 
 * 说明  : 记录一个邻居所到过的所有区域的信息，区域的频率信息
 */
public class NeighbourArea implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final static String tag="NeighbourArea";
	
	/**
	 * 该邻居的区域向量记录
	 */
	EndpointID neighbourEID=null;
	
	/**
	 * 用来存放所有的Area的hashmap
	 */
	HashMap<String,Area> areaMap;
	
/*	public NeighbourArea(EndpointID eid,BundlePayload payload)
	{
		neighbourEID=eid;
		areaMap=new HashMap<String, Area>(500);
		init(eid.toString(),payload);
	}*/
	
	public NeighbourArea(EndpointID eid)
	{
		neighbourEID=eid;
		areaMap=new HashMap<String, Area>(500);
		
		init();
	}
	
	/*public NeighbourAreaManager(Bundle bundle)
	{
		neighbourEID=bundle.sou
	}*/
	
	/*public NeighbourAreaManager(String eid)
	{
		neighbourEID=new EndpointID(eid);
		init(eid);
	}*/
	
	//利用已有的
	
	/**
	 * 利用本地文件来更新，读取eid所对应的区域文件，然后添加到对应的Area列表中
	 */
	private void init()
	{
		File neighbourAreaFile=new File(NeibhourConfig.NEIGHBOURAREAFILEDIR+neighbourEID.toString());
		if(!neighbourAreaFile.getParentFile().exists())
		{
			neighbourAreaFile.getParentFile().mkdirs();
			return ;
		}
		if(neighbourAreaFile.exists())
		{
			try
			{
				Log.i(tag,"从文件historyarea中读取历史的区域信息");
				updateArea(neighbourAreaFile);
				
				//从别的地方已经使用了
				/*ObjectInputStream in=new ObjectInputStream(new FileInputStream(neighbourAreaFile));
				Object obj=null;
				while((obj=in.readObject())!=null)
				{
					if(obj instanceof Area)
					{
						Area area=(Area)obj;
						String s=area.getAreaLevel()+"#"+area.getAreaId();
						areaMap.put(s, area);
					}
				}
				in.close();*/
			}
			catch(IOException e)
			{
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				GeohistoryLog.i(tag,"读取邻居的历史区域文件有错，ClassNotFoundException");
				
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 利用收到邻居的区域信息的bundle来初始化本地的邻居历史区域信息
	 * @param eid
	 * @param payload
	 */
	private void init(String eid,BundlePayload payload)	
	{
		if(areaMap==null)
		{
			areaMap=new HashMap<String, Area>(500);
		}
		else
		{
			areaMap.clear();
		}
		
		try {
			//利用将payload里面的对象直接读取出来的方式不可行，由于锁的原因，无法直接通过内存的读写，只好使用
//			byte[] buffer=new byte[payload.length()];
//			payload.read_data(0, buffer.length, buffer);
//			ByteArrayInputStream bin=new ByteArrayInputStream(payload.memory_buf());
//			ByteArrayInputStream bin=new ByteArrayInputStream(buffer);
			/*ObjectInputStream oin=new ObjectInputStream(bin);
			
			Object obj=null;
			while((obj=oin.readObject())!=null)
			{
				if(obj instanceof Area)
				{
					Area area=(Area)obj;
					String s=String.valueOf(area.getAreaLevel())+"#"+String.valueOf(area.getAreaId());
					areaMap.put(s, area);
				}
			}
			oin.close();
			bin.close();
			*/
			//将文件保存到本地
			File neighAreaFileDir=new File(NeibhourConfig.NEIGHBOURAREAFILEDIR);
			if(!neighAreaFileDir.exists())
			{
				neighAreaFileDir.mkdirs();
			}
			File neighAreaFile=new File(neighAreaFileDir, eid);
			//删除已有的文件
			if(neighAreaFile.exists())
			{
				neighAreaFile.delete();
			}
			neighAreaFile.createNewFile();
			
			//将邻居的区域记录保存到本地，利用payload里面原有的方法
			GeohistoryLog.i(tag, String.format("将邻居（ %s）发来的payload里面的区域移动规律存储到文件中", eid));
			payload.copy_to_file(neighAreaFile);
			
			/*Log.i(tag,"利用payload刚存入的邻居的区域信息来更新内存中邻居的区域移动规律");
			ObjectInputStream in=new ObjectInputStream(new FileInputStream(neighAreaFile));
			Object obj=null;
			while((obj=in.readObject())!=null)
			{
				if(obj instanceof Area)
				{
					Area area=(Area)obj;
					String s=area.getAreaLevel()+"#"+area.getAreaId();
					areaMap.put(s, area);
				}
			}
			in.close();*/
			updateArea(neighAreaFile);
			
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 由于不能解决利用payload里面的内存更新邻居的区域频率信息，暂时弃用这个函数
	 * @param payload
	 */
	/*public void updateArea(BundlePayload payload)
	{
		init(neighbourEID.toString(),payload);
	}*/
	
	/**
	 * 利用邻居区域移动频率的文件来更新频率文件
	 * @param payloadFile
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws StreamCorruptedException 
	 * @throws ClassNotFoundException 
	 */
	public void updateArea(File payloadFile) throws StreamCorruptedException, FileNotFoundException, IOException, ClassNotFoundException{
		Log.i(tag,"利用payloadFile来更新内存中邻居的区域移动规律");
		ObjectInputStream in=new ObjectInputStream(new FileInputStream(payloadFile));
		Object obj=null;
		while((obj=in.readObject())!=null)
		{
			if(obj instanceof Area)
			{
				Area area=(Area)obj;
				String s=area.getAreaLevel()+"#"+area.getAreaId();
				areaMap.put(s, area);
			}
		}
		in.close();
	}
	
	/*public static boolean payload_to_neighbourAreaFile(EndpointID eid,File file)
	{
		if(file==null)
		{
			return false;
		}
		
		
		
		return true;
	}*/
	
	/**
	 * 检查该邻居的历史区域向量中有没有该区域
	 * @param areainfo:所在各个层次信息
	 * @return :最底层的区域的引用，如果邻居历史区域信息中没有则返回Null
	 */
	/*public Area checkAreaInfo(AreaInfo areainfo)
	{
		int[] areaid=areainfo.getAreaId();
		
		int start=areainfo.getBaseAreaNum();
		int end=areaid.length;
		List<Area> arealist=new ArrayList<Area>(end);
		
		boolean valid=true;
		int level=areainfo.getBaseAreaLevel();
		
		int id=areaid[start];
		if(AreaLevel.isLevelValid(level))
		{
			//生成hashmap的标识字符串
			String s=String.valueOf(level)+"#"+String.valueOf(id);
			Area a=areaMap.get(s);
			
			return a;
		}
		//如果存在不合格的level，那么久不需要
		else
		{
			return null;
		}
	
	}*/
	
	/**
	 * 作用 ：根据bundle的目的节点，查询当前邻居节点中离目的地最近的区域（主要是尽可能低的区域层次）
	 * @param bundle :需要对比目的节点的bundle
	 * @return :如果找到了同一层次的区域则返回该层次区域；如果该邻居区域信息为空，则为null；一般情况下只要有区域信息就能返回区域信息的。
	 */
	public Area checkBundleDestArea(Bundle bundle)
	{
		if(areaMap.size()==0)
			return null;
		
		String areastr;
		Area result=null;
		
		areastr=String.valueOf(AreaLevel.FIRSTLEVEL)+"#"+String.valueOf(bundle.firstArea());
		if((result=areaMap.get(areastr))!=null)
		{
			return result;
		}
		
		areastr=String.valueOf(AreaLevel.SECONDLEVEL)+"#"+String.valueOf(bundle.secondArea());
		if((result=areaMap.get(areastr))!=null)
		{
			return result;
		}
		
		areastr=String.valueOf(AreaLevel.THIRDLEVEL)+"#"+String.valueOf(bundle.thirdArea());
		result=areaMap.get(areastr);
		return result;
	}
	
	/**
	 * 根据area查询当前
	 * @param area
	 * @return
	 */
	public Area checkArea(Area area)
	{
		String areastr=String.valueOf(area.getAreaLevel())+"#"+String.valueOf(area.getAreaId());
		
		return areaMap.get(areastr);
	}
}
