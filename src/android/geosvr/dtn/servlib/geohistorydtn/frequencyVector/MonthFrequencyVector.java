package android.geosvr.dtn.servlib.geohistorydtn.frequencyVector;

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
	}

	@Override
	public void changeVector() {
		// TODO Auto-generated method stub
		
	}

}
