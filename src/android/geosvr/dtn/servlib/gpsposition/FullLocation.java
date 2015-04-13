package android.geosvr.dtn.servlib.gpsposition;

import android.location.Location;

/**
 * ��������location�͵�ǰ��λ����ȫGPS�������GPS��λ
 * @author thedevilking
 *
 */
public class FullLocation 
{
	Location location=null;
	boolean gpsisvalue=true;
	
	//����ʹ��
//	public String s="";
	
	public FullLocation(Location location,boolean gpsisvalue)
	{
		this.location=location;
		this.gpsisvalue=gpsisvalue;
	}
	
	public Location getLocation()
	{
		return location;
	}
	
	public boolean isFullGps()
	{
		return gpsisvalue;
	}
}
