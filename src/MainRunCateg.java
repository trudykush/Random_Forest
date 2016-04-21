
import java.io.FileNotFoundException;

import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JOptionPane;

public class MainRunCateg
{
	//public String name2;
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException
	{
		System.out.println("Now Running Random-Forest");
		
		String name2;
		 name2 = JOptionPane.showInputDialog(null, "Enter the file name "); 	
		     @SuppressWarnings("unused")
	     // String   name1 = "tennis.csv";    
    	//  URL url = MainRunCateg.class.getResource(name1);
		// String name2; //= fileextract(name1);
		
			FileReader myFile = new FileReader(name2);
	
		int categ=0;
		String traindata,testdata;
		if(categ>0)
		{
			traindata = name2;
			testdata = name2;
		}
		else if(categ<0)
		{
			traindata = name2;
			testdata = name2;
		} 
		else
		{
			traindata = name2;
			testdata = name2;
		}
		
		DecisionTree dt = new DecisionTree(traindata);
		ArrayList<ArrayList<String>> Train = dt.CreateInput(traindata);
		ArrayList<ArrayList<String>> Test = dt.CreateInput(testdata);
		
		HashMap<String, Integer> Classes = new HashMap<String, Integer>();
		 for(ArrayList<String> dp : Train)
		 	{
			 	String clas = dp.get(dp.size()-1);
			 		if(Classes.containsKey(clas))
			 			Classes.put(clas, Classes.get(clas)+1);
			 		else
			 			Classes.put(clas, 1);				
		 	}
		
		int numTrees=10;
		int M=Train.get(0).size()-1;
		int Ms = (int)Math.round(Math.log(M)/Math.log(2)+1);
		int C = Classes.size();
		RandomForestCateg RFC = new RandomForestCateg(numTrees, M, Ms, C, Train, Test);
		RFC.Start();
		
	}
}

/*	public static String fileextract(String filename) throws UnsupportedEncodingException
	{
		URL url = MainRunCateg.class.getResource(filename);
		 return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}   */
