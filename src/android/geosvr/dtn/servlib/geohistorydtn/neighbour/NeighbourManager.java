package android.geosvr.dtn.servlib.geohistorydtn.neighbour;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.Collection;
import java.util.HashMap;

import android.geosvr.dtn.servlib.bundling.BundlePayload;
import android.geosvr.dtn.servlib.bundling.exception.BundleLockNotHeldByCurrentThread;
import android.geosvr.dtn.servlib.geohistorydtn.area.Area;
import android.geosvr.dtn.servlib.geohistorydtn.config.NeibhourConfig;
import android.geosvr.dtn.servlib.geohistorydtn.log.GeohistoryLog;
import android.geosvr.dtn.servlib.naming.EndpointID;
import android.util.Log;

/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-6-22 下午3:10:12 
 * 说明  ：作为邻居类的管理器
 * 
 * 尚未完成：没有将当前邻居的频率向量加入到计时器中
 */
public class NeighbourManager {
	
	private static String tag="NeighbourManager";
	
	/**
	 * 历史邻居的表
	 */
	HashMap<String, Neighbour> neighbourlist;
	
	/**
	 * 保存历史邻居对象的文件名称
	 * @author wwtao
	 */
	private static String historyNeighbourFileName="/sdcard/geoHistory_dtn/historyNeighbour";
	
	private static class SingleNeighbourManager
	{
		public static NeighbourManager instance=new NeighbourManager();
	}
	
	public static NeighbourManager getInstance()
	{
		return SingleNeighbourManager.instance;
	}
	
	private NeighbourManager()
	{
		neighbourlist=new HashMap<String, Neighbour>(NeibhourConfig.NEIGHOURNUM);
	}
	
	/**
	 * 从文件中获取历史的邻居的记录，包括历史邻居的
	 */
	public void init()
	{
		//从文件中读取邻居
		File file=new File(historyNeighbourFileName);
		try
		{
			if(file.exists())
			{
				Log.i(tag,String.format("从文件%s中读取历史的邻居信息",historyNeighbourFileName));
				ObjectInputStream in=new ObjectInputStream(new FileInputStream(file));
				Object obj=null;
				while((obj=in.readObject())!=null)
				{
					if(obj instanceof Neighbour)
					{
						//将读取到的邻居保存到内存中
						Neighbour neighbour=(Neighbour)obj;
						String s=neighbour.neighbourEid.toString();
						if(s==null || s.equals("")){
							continue;
						}
						neighbourlist.put(s, neighbour);
						
						//将读取到的历史邻居的频率频率向量加入到频率向量管理器中
//						addAreaFVector2Manager(area);
						neighbour.addAreaFVector2Manager();
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
			GeohistoryLog.i(tag,"读取本节点的历史邻居文件有错，IOExcetpion");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			GeohistoryLog.i(tag,"读取本节点的历史邻居文件有错，ClassNotFoundException");
			
			e.printStackTrace();
		}
	}
	
	/**
	 * 将历史的邻居记录保存到文件中，以便下一次访问
	 */
	private void saveHistoryNieghbour(){
		try
		{
			File file=new File(historyNeighbourFileName);
			if(!file.getParentFile().exists())
				file.getParentFile().mkdirs();
			if(!file.exists())
				file.createNewFile();
			
			ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(file, false));
			for(Neighbour neighbour:neighbourlist.values())
			{
				out.writeObject(neighbour);
			}
			out.writeObject(null);//加入一个写null对象，防止EOFexception异常
			out.flush();
			out.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			GeohistoryLog.i(tag,"将邻居的信息保存到文件出错，IOException");
		}
	}
	
	/**
	 * 关闭邻居管理器所要进行的必要步骤和操作，包括清空已有的邻居表，保存历史的邻居信息
	 */
	public void shutdown(){
		//退出前，打印本节点的邻居的相遇频率，邻居的区域频率向量
		GeohistoryLog.i(tag, "NeighbourManager退出前打印所有的邻居的相遇频率以及邻居的区域频率向量");
		for(Neighbour nei:neighbourlist.values()){
			//只是显示出来所有的neighbour的频率向量
			GeohistoryLog.d(tag,nei.toString());
			nei.printAreaInfo();
		}
		
		saveHistoryNieghbour();//保存所有的邻居对象到文件
		
		neighbourlist.clear();//清空邻居列表
	}
	
	/**
	 * 遇到新邻居后进行check，如果没有该邻居将其建立
	 */
	public Neighbour checkNeighbour(EndpointID eid)
	{
		if(eid==null)
			return null;
		
		Neighbour nei=neighbourlist.get(eid.toString());
		if(nei==null)
		{
			try {
				nei=new Neighbour(eid);
				GeohistoryLog.i(tag, String.format("添加新的邻居%s",eid.toString()));
				//测试信息，用来查看邻居的频率向量
				Log.v(tag,String.format("频率向量%s：",nei.toString()));
				Log.v(tag,String.format("邻居的移动规律："));
				nei.printAreaInfo();
			} catch (Exception e) {
				e.printStackTrace();
				return null;//由于eid是null值，不能生成所需要的Neighbour
			}
			neighbourlist.put(eid.toString(), nei);
			
		}
		//更改频率向量
		nei.checkVectorChange();
		return nei;
	}
	
	/**
	 * 找到该eid的Neichbour
	 */
	public Neighbour getNeighbour(EndpointID eid)
	{
		if(eid!=null)
			return neighbourlist.get(eid.toString());
		else
			return null;
	}
	
	public Collection<Neighbour> getAllNeighbour()
	{
		return neighbourlist.values();
	}
	
	public void printAllNeighbour(){
		for(Neighbour nei:neighbourlist.values()){
			//只是显示出来所有的neighbour的频率向量
			Log.v(tag,nei.toString());
		}
	}
	
	/**
	 * 将邻居发来的区域频率信息的bundle的payload内容保存下来
	 * @param payload：bundle的内容
	 * @param eid：发来bundle的邻居的eid
	 */
	public void saveNeighbourAreaPayload(BundlePayload payload,String eid){
		try{
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
//			return neighAreaFile;
		}
		catch (Exception e){
			e.printStackTrace();
//			return null;
		}
	}
	
	/**
	 * 需要对邻居进行排序，对部分邻居获取区域信息
	 */
}
