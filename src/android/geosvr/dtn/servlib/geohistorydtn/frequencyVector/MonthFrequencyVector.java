package android.geosvr.dtn.servlib.geohistorydtn.frequencyVector;

import android.geosvr.dtn.servlib.geohistorydtn.area.AreaInfo;
import android.geosvr.dtn.servlib.geohistorydtn.timeManager.TimeManager;

public class MonthFrequencyVector extends FrequencyVector 
{

	
	public MonthFrequencyVector(int vectorLevel, int serviceType) {
		super(FrequencyVectorLevel.monthVector, serviceType);
		// TODO Auto-generated constructor stub
	}
	
	public void init()	
	{
		this.vector=new double[12];
		this.vectorChange=new boolean[12];
		this.vectorLength=12;
		
		//初始化向量
		for(int i=0;i<vectorLength;i++)
		{
			vector[i]=0;
			vectorChange[i]=false;
		}
	}

	@Override
	public void changeVector(AreaInfo info) {
		// TODO Auto-generated method stub
		int month=info.getMonth();
		
		if(month>=0 && month<12)
		{
			if(!vectorChange[month])
			{
				++vector[month];
			}
		}
	}

}
