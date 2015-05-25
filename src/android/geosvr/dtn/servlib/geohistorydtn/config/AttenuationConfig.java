package android.geosvr.dtn.servlib.geohistorydtn.config;
/** 
 * @author wwtao thedevilking@qq.com: 
 * @version 创建时间：2015-5-19 下午12:44:16 
 * 说明 :用来维护相应的衰减系数
 */
public class AttenuationConfig {
	
	
	/**
	 * 返回需要的衰减参数
	 * @param serviceType：向量服务于哪一种类别
	 * @param vectorLevel：向量属于哪一种时间尺度
	 * @return
	 */
	public static double getAttenuation(int vectorLever,int serviceType)
	{
		return 0.9d;
	}

}
