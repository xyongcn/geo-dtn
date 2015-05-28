package android.geosvr.dtn.servlib.geohistorydtn.frequencyVector;

import android.geosvr.dtn.servlib.geohistorydtn.area.AreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.timeManager.TimeManager;

public class HourFrequencyVector extends FrequencyVector 
{

	
	public HourFrequencyVector(int vectorLevel, int serviceType) {
		super(FrequencyVectorLevel.hourVector, serviceType);
	}
	
	public void init()	
	{
		this.vector=new double[24];
		this.vectorChange=new boolean[24];
		this.vectorLength=24;
		
		//初始化
		for(int i=0;i<vectorLength;i++)
		{
			vector[i]=0;
			vectorChange[i]=false;
		}
	}

	@Override
	public void changeVector(AreaInfo info) {
		// TODO Auto-generated method stub
		int hour=info.getHour();
		if(hour>=0 && hour<24)
		{
			if(!vectorChange[hour])
			{
				//对该向量加1
				++vector[hour];
			}
		}
	}

}
