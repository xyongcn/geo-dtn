package android.geosvr.dtn.servlib.gpsposition;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;

/**
 * ��λ�Ĵ���
 * ʵ������п��Բ�ȡ�߳������ڷ��ʻ�ȡ����λ��
 * @author TheDevilKing
 *
 */
public class Gps 
{
	Context context=null;
	LocationManager locationmanager=null;
	Location nowlocation=null;
	
//	boolean gpsalive=false;
	
	public Gps(Context context)
	{
		this.context=context;
		locationmanager=(LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		
		LocationListener locationListener=new LocationListener() {
			
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				// TODO Auto-generated method stub
				/*switch(status)
				{
				case LocationProvider.AVAILABLE:
					gpsalive=true;
					break;
					
				case LocationProvider.OUT_OF_SERVICE:
					gpsalive=false;
					break;
					
				case LocationProvider.TEMPORARILY_UNAVAILABLE:
					gpsalive=false;
					break;
				}*/
			}
			
			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub
				nowlocation=locationmanager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				
			}
			
			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub
//				gpsalive=false;
			}
			
			@Override
			public void onLocationChanged(Location location) {
				// TODO Auto-generated method stub
				nowlocation=location;
				nowlocation.setTime(System.currentTimeMillis());
			}
		};
		locationmanager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		
	}
	
	//��ȡlocation��
	public Location getlocation()
	{
//		Location location= locationmanager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		return nowlocation;
	}
	
	/*public boolean getGpsAlive()
	{
		return gpsalive;
	}*/
	
	/**
	 * �⼸���ִ��������������ʹ�õģ���ȡ��location�����ͨ����������ַ�ʽ��ȡ�����ǵľ�γ���Լ���ʱ��ʱ��
	 */
	/*//��ȡ����
	public double getLongitude()
	{
		return location.getLongitude();
	}
	
	//��ȡγ��
	public double getLatitude()
	{
		return location.getLatitude();
	}
	
	//��ȡ��λʱ��
	public long getTime()
	{
		return location.getTime();
	}*/
}
