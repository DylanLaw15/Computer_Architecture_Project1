package computerArchProj;

import java.text.DecimalFormat;
import java.util.*; 

public class Float {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Random ran = new Random();
		
		//a+(b+c) == (a+b)+c
		//we are checking this 100 times
		
		float a = 0, b = 0, c = 0;
		int i = 0, t = 0, f = 0;
		boolean yesOrNo = false;
		
		while(i < 100) {
			
			a = ran.nextFloat();
			b = ran.nextFloat();
			c = ran.nextFloat();
			yesOrNo = check(a,b,c);
			System.out.println(yesOrNo + "\n");
			if(yesOrNo) {
				t++;
			}
			else if(!yesOrNo) {
				f++;
			}
			i++;
		}
	
		percentage(t,f);
	
	}
	
	
	
	//checking if the condition is true between the two math equations
	public static boolean check(float a, float b, float c) {
		
		
		float num1 = a + (b + c);
		float num2 = (a + b) + c;
		
		System.out.println("First Expression = " + num1 + "\nSecond Expression = " + num2);
	
		if(num1 == num2){
			return true;
		}
		else{
			return false;
		}
	}
	
	//finding the percentage of true conditions
	public static void percentage(int t, int f) {
		
	double num = 0;
	System.out.println("Number of true conditions = " + t + "\nNumber of false conditions = " + f);
	
	num = ((double) f / (double) t) * 100;
	//rounding
	DecimalFormat df = new DecimalFormat("#.##");      
	num = Double.valueOf(df.format(num));
	
	System.out.println("Condition is false about " + num + "% percent of the time.");
	}

}
