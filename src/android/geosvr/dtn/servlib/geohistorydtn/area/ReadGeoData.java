package android.geosvr.dtn.servlib.geohistorydtn.area;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

/**
 * 用来作为读取区域连通性矩阵以及脚本的类
 * @author wwtao
 *
 */
public class ReadGeoData 
{
	//读取的是一个方阵
	public static int[][] getConnectedMatrix(Context context) throws NotFoundException, IOException
	{
		InputStream in=context.getAssets().open("connectionmatrix.xml");
		int length=in.available();
		byte[] buffer=new byte[length];
		in.read(buffer);
		String content=new String(buffer);//文档内容
		
		
		String[] line=content.split("\n|\0");
		int[][] matrix=new int[line.length][line.length];
		
		for(int i=0;i<line.length;i++)
		{
			String[] s=line[i].split(",|\t");
			if(s.length!=line.length)
				return null;
			
			for(int j=0;j<s.length;j++)
			{
				matrix[i][j]=Integer.valueOf(s[j].trim());
			}
		}
		
		return matrix;
		
	}
	
	//读取一个矩阵
	public static int[][] getDiversionMatrix(Context context) throws IOException
	{
		InputStream in=context.getAssets().open("division.xml");
		int length=in.available();
		byte[] buffer=new byte[length];
		in.read(buffer);
		String content=new String(buffer);//文档内容
		
		
		String[] line=content.split("\n|\0");
		int[][] matrix=new int[line.length][];
		
		for(int i=0;i<line.length;i++)
		{
			String[] s=line[i].split(",|\t");
			/*if(s.length!=line.length)
				return null;*/
			matrix[i]=new int[s.length];//为每一行声明空间
			
			for(int j=0;j<s.length;j++)
			{
				matrix[i][j]=Integer.valueOf(s[j].trim());
			}
		}
		
		return matrix;
	}
}
