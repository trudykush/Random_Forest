

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class RandomForestCateg
{
	private static final int NUM_THREADS=Runtime.getRuntime().availableProcessors();			// the number of threads to use when generating the forest 
	public static int C;	
	public static int M;
	public static int Ms;
	private ArrayList<Entropy> trees;		// the collection of the forest's decision trees 
	private ArrayList<BootStrap> trees2;		// the collection of the forest's decision trees 
	private long time_o;						// the starting time when timing random forest creation 
	private int numTrees;						// the number of trees in this random tree
	private double update;
	private double progress;
	private int[] importances;
	private HashMap<int[],int[]> estimateOOB;
	private double error;						/// the total forest-wide error 
	private ExecutorService treePool;
	private ArrayList<ArrayList<String>> data;
	private ArrayList<ArrayList<String>> testdata;
	private ArrayList<ArrayList<String>> Prediction;
	public ArrayList<Integer> TrainAttributes;
	public ArrayList<Integer> TestAttributes;

	public RandomForestCateg(int numTrees,int M,int Ms,int C, ArrayList<ArrayList<String>> train,ArrayList<ArrayList<String>> test) 
	{
		StartTimer();
		this.numTrees=numTrees;
		this.data=train;
		this.testdata=test;
		this.M=M;
		this.Ms=Ms;
		this.C=C;
		this.TrainAttributes=GetAttributes(train);
		this.TestAttributes=GetAttributes(test);
		trees = new ArrayList<Entropy>(numTrees);
		trees2 = new ArrayList<BootStrap>(numTrees);
		update=100/((double)numTrees);
		progress=0;
		System.out.println("creating "+numTrees+" trees in a random Forest. . . ");
		System.out.println("total data size is "+train.size());
		System.out.println("number of attributes "+M);
		System.out.println("number of selected attributes "+Ms);
		
		estimateOOB=new HashMap<int[],int[]>(data.size());
		Prediction = new ArrayList<ArrayList<String>>();
	}
	public void Start() 
	{
		System.out.println("Number of threads started : " + NUM_THREADS);
		System.out.println("Starting trees ");
		treePool=Executors.newFixedThreadPool(NUM_THREADS);
		for (int t=0;t<numTrees;t++)
		{
			treePool.execute(new CreateTree(data,this,t+1));
		}
	treePool.shutdown();
		try 
		{	         
			treePool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); 
	    }
		catch (InterruptedException ignored)
		{
	    	System.out.println("interrupted exception in Random Forests ");
	    }
		if(data.get(0).size()>testdata.get(0).size())
		{
			TestForestNoLabel(trees,data,testdata);
		}
		else if(data.get(0).size()==testdata.get(0).size())
		{
			TestForest2(trees2, data, testdata);
		}
		else
			System.out.println("Cannot test this data ");
		
		System.out.print("Done in "+TimeElapsed(time_o));
	}
	private void TestForestNoLabel(ArrayList<Entropy> trees2,ArrayList<ArrayList<String>> data2,ArrayList<ArrayList<String>> testdata2) 
	{
		ArrayList<String> TestResult = new ArrayList<String>();
		System.out.println("Predicting Labels now ");
		for(ArrayList<String> DP:testdata2)
		{
			ArrayList<String> Predict = new ArrayList<String>();
			for(Entropy DT:trees2)
			{
				Predict.add(DT.Evaluate(DP, testdata2));
			}
			TestResult.add(ModeofList(Predict));
		}
	}
	public void TestForest(ArrayList<Entropy> trees,ArrayList<ArrayList<String>> train,ArrayList<ArrayList<String>> test)
	{
		int correctness=0;ArrayList<String> ActualValues = new ArrayList<String>();
		
		for(ArrayList<String> s:test)
		{
			ActualValues.add(s.get(s.size()-1));
		}
		int treee=1;
		System.out.println("Testing forest now ");
		
		for(Entropy DTC : trees)
		{
			DTC.CalculateClasses(train, test, treee);
			treee++;
			if(DTC.predictions!=null)
			Prediction.add(DTC.predictions);
		}
		for(int i = 0;i<test.size();i++)
		{
			ArrayList<String> Val = new ArrayList<String>();
			for(int j=0;j<trees.size();j++)
			{
				Val.add(Prediction.get(j).get(i));
			}
			String pred = ModeofList(Val);
			if(pred.equalsIgnoreCase(ActualValues.get(i)))
			{
				correctness++;
			}
		}
		System.out.println("The Result of Predictions ");
		System.out.println("Total Cases : " + test.size());
		System.out.println("Total Correct Predicitions : " + correctness);
		System.out.println("Forest Accuracy :" + (correctness*100/test.size()) + "%");				
	}
	private void TestForestNoLabel2(ArrayList<BootStrap> trees22,ArrayList<ArrayList<String>> data2,ArrayList<ArrayList<String>> testdata2) 
	{
		ArrayList<String> TestResult = new ArrayList<String>();
		System.out.println("Predicting Labels now ");
		for(ArrayList<String> DP:testdata2){
			ArrayList<String> Predict = new ArrayList<String>();
			for(BootStrap DT:trees22)
			{
				Predict.add(DT.Evaluate(DP, testdata2));
			}
			TestResult.add(ModeofList(Predict));
		}
	}
		public void TestForest2(ArrayList<BootStrap> trees,ArrayList<ArrayList<String>> train,ArrayList<ArrayList<String>> test)
		{
		int correctness=0;ArrayList<String> ActualValues = new ArrayList<String>();
		for(ArrayList<String> s:test)
		{
			ActualValues.add(s.get(s.size()-1));
		}
		int treee=1;
		System.out.println("Testing forest now ");
		for(BootStrap DTC : trees)
		{
			DTC.CalculateClasses(train, test, treee);treee++;
			if(DTC.predictions!=null)
			Prediction.add(DTC.predictions);
		}
		for(int i = 0;i<test.size();i++)
		{
			ArrayList<String> Val = new ArrayList<String>();
			for(int j=0;j<trees.size();j++)
			{
				Val.add(Prediction.get(j).get(i));
			}
			String pred = ModeofList(Val);
			if(pred.equalsIgnoreCase(ActualValues.get(i)))
			{
				correctness = correctness +1;
			}
		}
		System.out.println("The Result of Predictions ");
		System.out.println("Total Cases : " + test.size());
		System.out.println("Total Correct Predictions : " + correctness);
		System.out.println("Forest Accuracy : " + (correctness*100/test.size())+"%");				
	}
	public String ModeofList(ArrayList<String> predictions) 
	{
		String MaxValue = null; int MaxCount = 0;
		for(int i=0;i<predictions.size();i++)
		{
			int count=0;
			for(int j=0;j<predictions.size();j++)
			{
				if(predictions.get(j).trim().equalsIgnoreCase(predictions.get(i).trim()))
					count++;
				if(count>MaxCount)
				{
					MaxValue=predictions.get(i);
					MaxCount=count;
				}
			}
		}
		return MaxValue;
	}
	private class CreateTree implements Runnable
	{
		private ArrayList<ArrayList<String>> data;
		private RandomForestCateg forest;
		private int treenum;
		public CreateTree(ArrayList<ArrayList<String>> data,RandomForestCateg forest,int num)
		{
			this.data=data;
			this.forest=forest;
			this.treenum=num;
		}
		public void run() 
		{
			trees2.add(new BootStrap(data, forest, treenum));
			progress+=update;
		}
	}
	private void StartTimer()  // to calculate the time taken by the feature for finding the accuracy.
	{
		time_o=System.currentTimeMillis();
	}
	private static String TimeElapsed(long timeinms)
	{
		double s=(double)(System.currentTimeMillis()-timeinms)/1000;
		int h=(int)Math.floor(s/((double)3600));
		s -= (h*3600);
		int m = (int)Math.floor(s/((double)60));
		s -= (m*60);
		return ""+h+"hr "+m+"m "+s+"sec";
	}
		private boolean isAlphaNumeric(String s)
		{
			char c[]=s.toCharArray();boolean hasalpha=false;
				for(int j=0;j<c.length;j++)
				{
					hasalpha = Character.isLetter(c[j]);
						if(hasalpha)
							break;
				}
				return hasalpha;
		}
		private ArrayList<Integer> GetAttributes(List<ArrayList<String>> data)
		{
			ArrayList<Integer> Attributes = new ArrayList<Integer>();
			int iter = 0;
			ArrayList<String> DataPoint = data.get(iter);
				if(DataPoint.contains("n/a") || DataPoint.contains("N/A"))
				{
					iter++;
					DataPoint = data.get(iter);
				}
			for(int i =0;i<DataPoint.size();i++)
			{
				if(isAlphaNumeric(DataPoint.get(i)))
				Attributes.add(1);
				else
				Attributes.add(0);
			}
			return Attributes;
		}
	}
