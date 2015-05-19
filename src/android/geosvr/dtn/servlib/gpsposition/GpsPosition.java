package android.geosvr.dtn.servlib.gpsposition;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class GpsPosition extends Thread
{
	private final double meterPerAngle=111.31955*1000;
	
	private Context context=null;
	private Handler activityHandler=null;
	
	StepPosition stepposition=null;
	Gps gps=null;
	
	int interval=150;//获取地理位置的间隔（毫秒为单位，用来确定线程获取地理位置信息的频率）
	
	boolean alive=true;//程序是否结束标志，若为FALSE则结束程序运行
	
	boolean gpsisvalue=true;//检测当前GPS是否有效，TRUE表示GPS可正常使用;用来作为返回时标志是最新GPS定位还是相对GPS定位
	Position position=null;//通过计步程序获得的结果
	Location location=null;//通过GPS获取的位置信息
	Location lastLocation=null;//最后一个可以正常使用的GPS
	double lastLongtitude=0;
	double lastLatitude=0;
	long lastLocationTime=System.currentTimeMillis();//上次定位时间
	
	int gpsOutOfTime=3000;//gps三秒钟没有定位指示gps失去连接，进入计步定位模式
	
	/*//写入日志（测试使用）
	String log="";*/
	//测试GPS是否定位超时代码
//	String gpsout="";
	
	//默认位置信息，用来无法获取首次GPS信息也可以实现定位
	Location defaultLocation=null;
	
	Handler myhandler=new Handler()
	{

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			position=stepposition.getPosition();
			super.handleMessage(msg);
			
		}
		
	};
	
	//对当前的position（计步程序获取的位置进行初始化）
	public void resetStepPosition()
	{
		if(position!=null)
			position.reset();
	}
	
	//获取当前的定位的位置，activity调用
	public FullLocation getLocation()
	{
		if(lastLocation==null)
			return null;
		else
		{
			FullLocation flocation=new FullLocation(lastLocation, gpsisvalue);
//			flocation.s=gpsout;
			return flocation;
		}
	}
	
	
	/**
	 * 默认初始化
	 */
	public GpsPosition(Context context,Handler activityHandler)
	{
		this.context=context;
		gps=new Gps(context);
		this.activityHandler=activityHandler;
		
//		stepposition=new StepPosition(context, myhandler);
		
		//用来测试室内计步部分
//		lastLocation=new Location("默认");
//		stepposition=
	}
	
	/**
	 * 有初始位置的初始化
	 */
	public GpsPosition(Context context,Handler activityHandler,double longtitude,double latitude)
	{
		this.context=context;
		gps=new Gps(context);
		this.activityHandler=activityHandler;
		
		defaultLocation=new Location("默认");
		defaultLocation.setLongitude(longtitude);
		defaultLocation.setLatitude(latitude);
		defaultLocation.setTime(System.currentTimeMillis());
		
		lastLocation=new Location(defaultLocation);
		//用来测试室内计步部分
//		lastLocation=new Location("默认");
//		stepposition=
	}
	
	public void computeRelativePosition()
	{
		if(lastLocation!=null && position!=null)
		{
			//x是向北的偏移，也就是纬度的变化；y是向东的偏移也就是经度的变化
			//由于我们所处的位置是北半球，经度是东经，所以向东经度会变大，向北纬度会变大
			double addlongtitude=position.getY()/meterPerAngle;
			double angle=lastLocation.getLatitude()/((double)180)*Math.PI;
			double addlatitude=position.getX()/(meterPerAngle*Math.cos(angle));
			
//			Log.i("纬度与x偏移量","x="+position.getX()+"纬度:"+addlatitude+"分母："+Math.cos(angle)+"纬度："+lastLocation.getLatitude());
			
			lastLocation.setLongitude(lastLongtitude+addlongtitude);
			lastLocation.setLatitude(lastLatitude+addlatitude);
		}
	}
	
	@Override
	public void run() 
	{
		while(alive)
		{
			location=gps.getlocation();
			//location不为null说明GPS已经成功加载
			if(location!=null)
			{
				lastLocationTime=location.getTime();
				//gps定位超时
				if(Math.abs(System.currentTimeMillis()-lastLocationTime)>gpsOutOfTime)
				{
//					gpsout="计步定位中";
					
					if(gpsisvalue)
						gpsisvalue=false;
					
					if(stepposition==null)
					{
						stepposition=new StepPosition(context, myhandler);
						resetStepPosition();
					}
					
					computeRelativePosition();
				}
				//gps正常定位
				else
				{
//					gpsout="gps定位中";
					
					if(!gpsisvalue)
						gpsisvalue=true;
					
					lastLocation=location;
					lastLongtitude=lastLocation.getLongitude();
					lastLatitude=lastLocation.getLatitude();
					
					if(stepposition!=null)
					{
//						stepposition=null;
//						resetStepPosition();
						stepposition.clear();
					}
				}
			}
			//gps为null,有默认初始化的位置
			else if(defaultLocation!=null)
			{
				if(gpsisvalue)
					gpsisvalue=false;
				
				if(stepposition==null)
				{
					stepposition=new StepPosition(context, myhandler);
					resetStepPosition();
				}
				
				if(position!=null)
				{
					
					//x是向北的偏移，也就是纬度的变化；y是向东的偏移也就是经度的变化
					//由于我们所处的位置是北半球，经度是东经，所以向东经度会变大，向北纬度会变大
					double addlongtitude=position.getY()/meterPerAngle;
					double angle=defaultLocation.getLatitude()/((double)180)*Math.PI;
					double addlatitude=position.getX()/(meterPerAngle*Math.cos(angle));
					
//					Log.i("纬度与x偏移量","x="+position.getX()+"纬度:"+addlatitude+"分母："+Math.cos(angle)+"纬度："+lastLocation.getLatitude());
					
					lastLocation.setLongitude(defaultLocation.getLongitude()+addlongtitude);
					lastLocation.setLatitude(defaultLocation.getLatitude()+addlatitude);
				}
			}
			/*else
				gpsout="既没有gps定位也没有计步";*/
			
			/*if(location!=null)
			{
				System.out.println("gps正常使用");
				
				if(!gpsisvalue)
					gpsisvalue=true;
				
				lastLocation=location;
				lastLongtitude=lastLocation.getLongitude();
				lastLatitude=lastLocation.getLatitude();
								
				if(stepposition!=null)
				{
					stepposition=null;
					resetStepPosition();
				}
			}
			else//gps无法正常定位
			{
				System.out.println("gps无法使用");
				
				if(gpsisvalue)
					gpsisvalue=false;
				
				if(stepposition==null)
				{
					stepposition=new StepPosition(context, myhandler);
					resetStepPosition();
				}
				
				computeRelativePosition();
			}*/
			
			/*//写入日志,已经由外层类完成
			if(lastLocation!=null)
				log+=lastLocation.getLongitude()+","+lastLocation.getLatitude()+"\n";*/
			
			try 
			{
				sleep(interval);
				Message mesg=activityHandler.obtainMessage();
				activityHandler.sendMessage(mesg);
			} 
			catch (InterruptedException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/*public void writelog()
	{
		try 
		{
			SimpleDateFormat   formatter   =   new   SimpleDateFormat   ("yyyy年MM月dd日 HH,mm,ss");     
			Date   curDate   =   new   Date(System.currentTimeMillis());//获取当前时间     
			
			String parentFilename=Environment.getExternalStorageDirectory()+"/GpsPosition";
//			Log.i("fwne",parentFilename);
			File parentFile=new File(parentFilename);
			if(!parentFile.isDirectory())
			{
				parentFile.mkdirs();
			}
			String   filename   = parentFilename+"/"+  formatter.format(curDate)+".csv";
			File file=new File(filename);
			if(!file.exists())
			{
				file.createNewFile();
			}
			BufferedOutputStream write=new BufferedOutputStream(new FileOutputStream(file));
			write.write(log.getBytes());
			write.close();
			
		} catch (Exception e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	
	public void close()
	{
		alive=false;
	}
}
