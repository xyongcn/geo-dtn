package android.geosvr.dtn.servlib.geohistorydtn.areaConnectiveSimulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.geosvr.dtn.DTNManager;
import android.geosvr.dtn.DTNService;
import android.geosvr.dtn.servlib.bundling.BundleDaemon;
import android.geosvr.dtn.servlib.geohistorydtn.area.AreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.routing.GeoHistoryRouter;
import android.geosvr.dtn.servlib.routing.BundleRouter;
import android.os.Bundle;
import android.os.Message;

public class CurrentLocationFromScript implements CurrentLocation{
	
	private static String TAG="CurrentLocationFromScript"; 
	private static CurrentLocationFromScript instance=null;
	
	//线程安全单例
	/*public static CurrentLocationFromScript getInstance()
	{
		if(instance==null)
		{
			synchronized (instance) {
				if(instance==null)
				{
					instance=new CurrentLocationFromScript();
				}
			}
		}
		return instance;
	}*/
	
	//用来获取全局的变量分配
	int[][] connectedMatrix;//各个区域连通性矩阵
	int[][] divisionMatrix;//各个区域的划分
	HashMap<Integer, AreaDivision> areadivisionMap=new HashMap<Integer, CurrentLocationFromScript.AreaDivision>();
	
	
	public CurrentLocationFromScript(Context context) throws NotFoundException, IOException
	{
		
		connectedMatrix=ReadGeoData.getConnectedMatrix(context);
		divisionMatrix=ReadGeoData.getDiversionMatrix(context);
		
		if(connectedMatrix==null || divisionMatrix==null)
			return;

		//建立区域划分关系
		for(int i=0;i<divisionMatrix.length;i++)
		{
			int thisAreaNum=i+1;
			AreaDivision area=areadivisionMap.get(thisAreaNum);
			if(area==null)
			{
				area=new AreaDivision(thisAreaNum);
				areadivisionMap.put(thisAreaNum, area);
			}
			
			area.setLayer(divisionMatrix[i]);
		}
		
		//建立区域间可达的关系表
		for(int i=0;i<connectedMatrix.length;i++)
		{
			int thisAreaNum=i+1;
			AreaDivision thisArea=areadivisionMap.get(thisAreaNum);
			if(thisArea==null)
			{
				thisArea=new AreaDivision(thisAreaNum);
				areadivisionMap.put(thisAreaNum, thisArea);
			}
			
			//添加可达区域
			for(int j=0;j<connectedMatrix[i].length;j++)
			{
				int neiborNum=j+1;
				if(connectedMatrix[i][j]==1 && thisArea.inquireArea(neiborNum)==null)
				{
					thisArea.addConnectedArea(neiborNum);
				}
			}
		}
		
		
		instance=this;
		initCurrent();
	}
	
	private void sendTextViewUpdate()
	{
		Message msg=new Message();
		msg.what=DTNManager.LOCATIONCHANGED;
		Bundle b=new Bundle();
		b.putString("location", currentArea.toString());
		msg.setData(b);
		DTNManager.getInstance().TextViewUpdate.sendMessage(msg);
	}
	
	//由其他类调用，改变区域位置
	public boolean changeArea(AreaDivision area)
	{
		AreaDivision next=areadivisionMap.get(area.areanum);
		if(next!=null)
		{
			currentArea=next;
			sendTextViewUpdate();
			
			//通知区域改编的函数，触发区域变化的事件
			AreaInfo areainfo=new AreaInfo(area.areaLayer);
			//判断DTN服务是否在运行
			if(DTNService.is_running())
			{
				BundleRouter router=BundleDaemon.getInstance().router();
				if(router instanceof GeoHistoryRouter)
				{
					((GeoHistoryRouter)router).movetoArea(areainfo);
				}
			}
			
			return true;
		}
		else
			return false;
	}
	
	public Object[] getNeighbourArea()
	{
		return currentArea.connectedMap.values().toArray();
	}
	
	//维护当前区域信息
	AreaDivision currentArea;
	
	
	//初始化最初的位置
	private void initCurrent()
	{
		currentArea=areadivisionMap.get(1);
		
		//更改textview的显示
		sendTextViewUpdate();
		
		//开始临时的随机移动线程
		/*(new Thread(new Runnable() {
			
			@Override
			public void run() {
				Random rand=new Random();
				// TODO Auto-generated method stub
				//最多移动次数
				for(int i=0;i<100;i++)
				{
					try {
						//沉睡30s
						Thread.sleep(10*1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					Object[] col=currentArea.getneighbours();
					int num=col.length;
					if(num>0)
					{
						int next=rand.nextInt(num);
						if(col[next] instanceof AreaDivision)
							currentArea=(AreaDivision)col[next];
					}
					
					//更改textview的显示
					sendTextViewUpdate();
					
					
				}
			}
		})).start();*/
	}
	
	/**
	 * 重写的公共方法作为借口
	 * @author wwtao
	 *
	 */
	/*@Override
	public int[] getAreaLayer() {
		return null;
	};*/
	
	@Override
	public int getAreaNum() {
		// TODO Auto-generated method stub
		if(currentArea!=null)
			return currentArea.areanum;
		return 0;
	}
	
	
	//收集分析各个区域连通性和划分的类
	public class AreaDivision
	{
		//所有编号表示为0的就是非法的意思
		int areanum=0;//区域的编号
		
		/*int zeroArea=0;
		int firstArea=0;
		int secondArea=0;
		int thirdArea=0;*/
		//区域分层的数组，从0位置往后依次是上层到下层
		int[] areaLayer=new int[4];
		
		private HashMap<Integer,AreaDivision> connectedMap=new HashMap<Integer, CurrentLocationFromScript.AreaDivision>();
		
		public AreaDivision(int areanum,int[] arealayer)
		{
			this.areanum=areanum;
			if(this.areaLayer.length==arealayer.length)
			{
				for(int i=0;i<areaLayer.length;i++)
				{
					areaLayer[i]=arealayer[i];
				}
			}
			
		}
		
		public AreaDivision(int areanum)
		{
			this.areanum=areanum;
		}
		
		
		//查询相邻区域
		public AreaDivision inquireArea(int areanum)
		{
			return connectedMap.get(areanum);
		}
		
		public AreaDivision inquireArea(AreaDivision area)
		{
			return connectedMap.get(area.areanum);
		}
		
		//获取相邻区域列表
		public Object[] getneighbours()
		{
			return connectedMap.values().toArray();
		}
		
		//添加新的连通区域
		public void addConnectedArea(int areanum)
		{
			AreaDivision neighbourArea=areadivisionMap.get(areanum);
			if(neighbourArea==null)
				connectedMap.put(areanum,new AreaDivision(areanum));
			else
				connectedMap.put(areanum,neighbourArea);
		}
		
		//设置区域所属的层次
		public boolean setLayer(int[] layer)
		{
			//只输入了1层到3层的划分
			if(layer.length==3)
			{
				areaLayer[0]=1;
				for(int i=0;i<layer.length;i++)
				{
					areaLayer[i+1]=layer[i];
				}
				return true;
			}
			//输入了0层到3层的划分
			else if(layer.length==4)
			{
				for(int i=0;i<layer.length;i++)
				{
					areaLayer[i]=layer[i];
				}
				return true;
			}
			else
				return false;
		}
		
		public boolean isCorrect()
		{
			if(areanum==0)
				return false;
			
			for(int i=0;i<areaLayer.length;i++)
			{
				if(areaLayer[i]==0)
					return false;
			}
			
			return true;
		}
		
		@Override
		public String toString() {
			String s="Layer:";
			for(int i=areaLayer.length-1;i>0;i--)
			{
				s+=areaLayer[i]+".";
			}
			s+=areaLayer[0]+"   areaNum=";
			s+=areanum;
			return s;
			/*if(isCorrect())
			{
				String s="Layer:";
				for(int i=areaLayer.length-1;i>0;i--)
				{
					s+=areaLayer[i]+".";
				}
				s+=areaLayer[0]+"   areaNum=";
				s+=areanum;
				return s;
			}
			else
				return "invalid area";*/
			
		}
	}

}
