package android.geosvr.dtn.systemlib.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.util.Log;

public class IpHelper {

    private static int byte2int(byte b) {
		if(b>=0){
			return b;
		}else{
			return b+256;
		}
	}
	/**
	 * ipbyte2ipstr
	 * byte[]转字符串ip
	 */
    public static String ipbyte2ipstr(byte[] ip) {
    	String ipStr="";
    	for(int i=0;i<4;i++){
    		ipStr+=Integer.toString(byte2int(ip[i]));
    		if(i < 3){
    			ipStr+=".";
    		}
    	}
    	return ipStr;
    }
    
    /**
     * 这种方法会把所有的IP地址查出来，再根据接口挑选。
     */
    public static InetAddress getLocalIpAddress() {
         try {
             for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                 NetworkInterface intf = en.nextElement();
                 for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                         enumIpAddr.nextElement();
                         if (intf.getName().equals("adhoc0"))
                         {
                                 //取第二个值
                                 InetAddress inetAddress = enumIpAddr.nextElement();
                                 if (!inetAddress.isLoopbackAddress()) {
                                 return inetAddress;
                             }
                         } 
                 }
             }
         } catch (SocketException ex) {
             Log.e("testAndroid1", ex.toString());
         }
         return null;
    } 
    
    /**
     * String IP 转 EndpointID String
     * dtn://192.168.1.3.wu.com
     */
    public static String ipstr2Idstr(String ipstr) {
    	String str = new String("dtn://");
    	str += ipstr;
    	str += ".wu.com";
    	return str;
    }

    /**
     * 将ip地址的字符串转换为Int来表示
     * @param ip
     * @return
     * @throws Exception
     */
    public static int ipstr2int(String ip) throws Exception{
		String[] iparr=ip.split("\\.");
		if(iparr.length!=4)
			throw new Exception(String.format("ip(%s) is not valid,length=%d",ip,iparr.length));
		int ipIntarr[]=new int[4];
		for(int i=0;i<iparr.length;i++){
			int temp=Integer.valueOf(iparr[i]);
			ipIntarr[i]=temp;
			if(temp<0 || temp >255){
				throw new Exception(String.format("ip(%s) is not valid",ip));
			}
		}
		
		int re=((ipIntarr[0] << 24) & 0xff000000 ) | ( (ipIntarr[1] << 16) & 0x00ff0000 )
				 | ( (ipIntarr[2] << 8) & 0x0000ff00 ) | ( ipIntarr[3] & 0x000000ff);
		return re;
	}
	
    /**
     * 将int类型的ip地址转换为字符串
     * @param ip
     * @return
     */
	public static String int2ipstr(int ip){
		int iparr[] = new int[4];
		iparr[0]=(ip >> 24) & 0x000000ff;
		iparr[1]=(ip >> 16) & 0x000000ff;
		iparr[2]=(ip >> 8) & 0x000000ff;
		iparr[3]=ip & 0x000000ff;
/*		System.out.println("ip="+ip);
		for(int i=0;i<iparr.length;i++)
			System.out.println(iparr[i]);*/
		return String.format("%s.%s.%s.%s", String.valueOf(iparr[0]), String.valueOf(iparr[1]),
				String.valueOf(iparr[2]), String.valueOf(iparr[3]));
	}
}
