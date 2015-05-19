package android.geosvr.dtn.servlib.discovery;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.geosvr.dtn.DTNService;
import android.geosvr.dtn.R;
import android.geosvr.dtn.servlib.conv_layers.DTNLocationProvider;
import android.geosvr.dtn.servlib.gpsposition.FullLocation;
import android.geosvr.dtn.servlib.gpsposition.GpsPosition;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Location extends TimerTask{
	String TAG="Location";
	//gps位置获取的引用
	GpsPosition gpsposition=null;
	
	
	private File locationFile_;
	private int timeCounter_;
	private int delay_sec_ ;
	private double longitude_;
	private double latitude_;
	private boolean isManual;
	
	//对获取到的GPS数据进行处理
	Handler handler=new Handler()
	{

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
//			super.handleMessage(msg);
			if(gpsposition!=null)
			{
				FullLocation fullLocation=gpsposition.getLocation();
				if(fullLocation!=null)
				{
					android.location.Location location=fullLocation.getLocation();
					//获取经纬度
					longitude_=location.getLongitude();
					latitude_=location.getLatitude();
					//判断是GPS定位还是计步定位
					//boolean isGps=fullLocation.isFullGps();
				}
				else
					Log.e(TAG,"Gps无法定位或者没有默认的经纬度");
			}
			else
				Log.e(TAG,"GpsPosition没有初始化");
		}
		
	};
	
	public double getLongitude() {
		return longitude_;
	}
	public double getLatitude() {
		return latitude_;
	}

	private static Location instance_;
	private Location(){
		isManual = false;
		timeCounter_ = 0;
		delay_sec_ = 5;
	}
	public static Location getInstance(){
		if (instance_ == null){
			instance_ = new Location();
		}
		return instance_;
	}
	
	public void init(){
		try {
			if (DTNService.context().getResources().getString(
					R.string.SetLocationManually).equals("true")) {
				isManual = true;
				locationFile_ = new File(DTNService.context().getResources().getString(
						R.string.LocationFilePath));
				Timer timer = new Timer(); 
			    timer.schedule(this, delay_sec_ * 1000, delay_sec_ * 1000);

			} else {
				
				/*get GPS data*/
				
				//需要的初始化数据
				double defaultLongtitude=100.112233;//默认的经度
				double defaultLatitude= 200.556677;//默认的纬度
				Context context=null;//activity的上下文
				
				//初始化
				if(gpsposition==null)
				{
					gpsposition=new GpsPosition(context, handler, defaultLongtitude, defaultLatitude);
					gpsposition.start();
				}
				
			}
			//启动交互接口
			DTNLocationProvider loc = new DTNLocationProvider();
			loc.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//time(sec) longitude latitude
	public void run()
	{
		timeCounter_ += delay_sec_;
		BufferedReader reader = null;
		
        try {
            System.out.println("以行为单位读取文件内容，一次读一整行：");
            reader = new BufferedReader(new FileReader(locationFile_));
            String readStr = null;
            String tempStr[] = null;
            
            //每行格式:时间（sec） 经度  纬度
            // 读到对应的一行写入
            while ((readStr = reader.readLine()) != null) {
            	tempStr = readStr.split(" ");
            	if (Integer.parseInt(tempStr[0]) >= timeCounter_) {
            		longitude_ = Integer.parseInt(tempStr[1]);
            		latitude_ = Integer.parseInt(tempStr[2]);
            		break;
            	}
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        
//		longitude_ = 100.112233;
//		latitude_ = 200.556677;
	}
	

}
