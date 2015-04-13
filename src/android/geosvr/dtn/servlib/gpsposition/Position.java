package android.geosvr.dtn.servlib.gpsposition;

public class Position 
{
	private double x;
	private double y;
	private double orientation;
	private int stepnum;
	
	public Position(double x,double y,double orientation,int stepnum)
	{
		this.x=x;
		this.y=y;
		this.orientation=orientation;
		this.stepnum=stepnum;
	}
	
	public double getX()
	{
		return x;
	}
	
	public double getY()
	{
		return y;
	}
	
	public double getOrientation()
	{
		return orientation;
	}
	
	public int getStepnum()
	{
		return stepnum;
	}
	
	public void reset()
	{
		x=0;
		y=0;
		stepnum=0;
	}
}
