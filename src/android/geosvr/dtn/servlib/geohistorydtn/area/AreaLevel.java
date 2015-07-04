package android.geosvr.dtn.servlib.geohistorydtn.area;

public class AreaLevel {

	public final static int ZEROLEVEL=0;
	public final static int FIRSTLEVEL=1;
	public final static int SECONDLEVEL=2;
	public final static int THIRDLEVEL=3;
	
	//获取相匹配的level
	public static boolean isLevelValid(int l)
	{
		for(int i=MINLEVEL;i<=MAXLEVEL;i++)
		{
			if(i==l)
				return true;
		}
		return false;
	}
	
	public static int nextLevel(int l) throws Exception
	{
		for(int i=MINLEVEL;i<=MAXLEVEL;i++)
		{
			if(i==l)
			{
				if(i<MAXLEVEL)
				{
					return (i+1);
				}
				else
					break;
			}
		}
		throw new Exception("no matched level");
	}
	
	//最顶层
	public final static int MAXLEVEL=THIRDLEVEL;
	//最底层
	public final static int MINLEVEL=ZEROLEVEL;
}
