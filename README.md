# Computer Architecture Project1
## Stack Overflow in Recursive Functions (Fibonacci)
### So what is a Stack overflow?
[Techtarget](https://whatis.techtarget.com/definition/stack-overflow) defines a stack overflow as:
> A stack overflow is an undesirable condition in which a particular computer program tries to use more memory space than the call stack has available.

We can observe this through the C implementation of a Fibonacci calculator.

***All programming and debugging is done on a linux system, so the binaries will be compiled for a linux system (ELF)***

![file command](https://github.com/DylanLaw15/Computer_Architecture_Project1/blob/master/Pictures/file.png)

Here is the code (Fibonacci.c):
```c
#include <stdio.h>

int fibonacci(int n) {
   if(n == 0){
      return 0;
   } else if(n == 1) {
      return 1;
   } else {
      return (fibonacci(n-1) + fibonacci(n-2));
   }
}

int main() {
   int n;

   /*Getting user input*/
   printf("Enter a number to find to find Nth fibonacci term: ");
   scanf("%d", &n);
	
   /*Calculate fibonacci term of number given by user, then print it*/
   printf("Fibonacci of %d: " , n);
   printf("%d \n",fibonacci(n));

   return 0;
}
```
Compile the code with `gcc Fibonacci.c -o Fibonacci` then mark as executable with `chmod +x Fibonacci`

Run with small managble numbers:

![Regular Output](https://github.com/DylanLaw15/Computer_Architecture_Project1/blob/master/Pictures/regular_output.png)

On a Windows system the default stack size (depending on compiler) is usually 1MB, which is alot less than the relatively huge default stack size of 8MB on a linux system. Now for demonstrastration purposes I will decrease the stack size from 8MB to 128KB (So we can cause stack overflow easier)

![Decrease Size of Stack](https://github.com/DylanLaw15/Computer_Architecture_Project1/blob/master/Pictures/changing_stack_size.png)

Now run the binary with a large number:

![Seg Fault](https://github.com/DylanLaw15/Computer_Architecture_Project1/blob/master/Pictures/seg_fault.png)

As you can see we get a segmentation fault, meaning our process is trying to access memory it does not have permission to access.

### Now for the fun part, why and how did this happen?

We will be using gdb with the gdb-peda plugin (just becuase I am used to using it) to debug this program!

Lets load the binary into gdb and disassemble the fibonacci function:

![fibonacci_disass](https://github.com/DylanLaw15/Computer_Architecture_Project1/blob/master/Pictures/fibonacci_disassembly.png)

Lets break down what is happening in the fibonacci function:

1. Function Prologue
2. Passing in the argument to fibonacci function
3. Logic to return based on if n is 0 or 1
4. Calling fibonacci() on n-1 and n-2 (this is the recursion)
5. Function Epilogue


Now lets debug the Fibonacci binary and cause a seg fault (Finding 10000th term in fibonacci sequence). 

![vmmap](https://github.com/DylanLaw15/Computer_Architecture_Project1/blob/master/Pictures/vmmap.png)

![debug_seg_fault](https://github.com/DylanLaw15/Computer_Architecture_Project1/blob/master/Pictures/debug_seg_fault.png)

Lets break down what happend:

1. vmmap command shows the virtual memory segments. As you can see the stack is 128KB like we set. (0x00007ffffffff000 - 0x00007ffffffdf000 = 0x20000bytes == 131,072bytes == 128KB)
   - Quick note: The stack is the only memory segment that grows upward (from higher addresses to lower), this will be important in understanding why the program seg faulted
2. This is the last instruction executed before the seg fault, 0x18 was subtracted from the Stack Pointer (points to the top of the stack)
3. Now the stack pointer is 0x7ffffffdeff0. Lets take the starting address of the stack from picture 1 and subtract the rsp: 0x00007ffffffdf000 - 0x7ffffffdeff0 = 0x10 this means that the stack pointer is now pointing 2 bytes higher than the highest address of the stacks virtual memory segment (BAD!!!).
4. Now gdb-peda cannot display the stack because the top of the stack (RSP) is pointing out of range.
5. RBP is the base pointer, which stores the stack pointer from the previous stack frame, as seen in the 2nd instruction of the function prologue of fibonacci() `mov    rbp,rsp`. 
6. "RDI" is the register that the first argument to a function is passed to in the x86_64 calling convention. It contains 0x1cc0 (7360 in decimal), so we know the process segfaulted when calling fibonacci(7360). Which means that 2640 (10000 - 7360) calls to fibonacci() have active stack frames taking up space on the stack. We can verify this with backtrace or bt for short in gdb. The first 2634 entrys have been cropped out.

![backtrace](https://github.com/DylanLaw15/Computer_Architecture_Project1/blob/master/Pictures/backtrace.png)

We still havn't answered "how" this happend. Let's do that now.

Let's look at the top of the stack after it seg faulted.

![top of stack](https://github.com/DylanLaw15/Computer_Architecture_Project1/blob/master/Pictures/top_of_stack.png)

Analyzing the top of the stack:

1. The command `x/30x $sp+0x10` will show the top 30 quad words or QWORDS (64 bits/8 bytes) on the stack.
   - We are examining "$sp+0x10" because if you recall the stack pointer was pointing into invalid memory (stack -0x10) when the process seg faulted.
2. The top of the stacks virtual memory segment.
3. As you can see I highlighted each stack frame (except for the bottom partial stack frame, becuase I ran out of highlight colors and didn't want to confuse anyone)
4. This is a single stack frame.
   - The first QWORD is 0x00007ffffffdf100 which is the saved RBP. This is pushed onto the stack in the first instruction in the prologue of functions `push   rbp`. The "saved RBP" is the address of the beggining of the previous stack frame.
   - The fourth QWORD is 0x00001cc400000000, this isn't actually a QWORD, but instead two DWORDs (32 bits/4 bytes). That first DWORD 0x00001cc4 is actually the argument passed to the fibonacci function (0x00001cc4 == 7364 in decimal) through RDI. So in this stack frame we are on the 2636th call to fibonacci(), nearing the end of the stack, which we know happens at 2640 calls to fibonacci()
     - The argument pushed onto the stack through RDI (0x00001cc4 or 7364) is a DWORD becuase in our main function we declared "n" as an int. Which takes up 4 bytes (DWORD).
   - The sixth QWORD is 0x0000555555555178 which is the return address. This is pushed onto the stack during the `call   0x555555555145 <fibonacci>`. Since we are calling the fibonacci function again, a new stack frame is created.
     - Mabye you noticed, I said that the return address is push onto the stack during the `call   0x555555555145 <fibonacci>`, but that's not a push right? So how is something getting pushed onto the stack. Well on the [Intel x86 architecture](https://en.wikipedia.org/wiki/X86), the "call" instruction is actually 2 instructions `push $RIP;    jmp <address>`. The RIP is the pointer to the next instruction to be executed. 
   - Let's do some quick math: each stack frame is 0x30bytes in size, and there are 2640 of them, so 0x30 * 2640 = 126720bytes. 126720/1024 = 123KB. So there are 128KB-123KB (5KB) of stack space not allocated to the fibonacci() stack frames, where did they go? Well there are other things on the stack including main's stack frame, \__libc_start_main's stack frame, start's stack frame (which we can see in the backtrace), as well as arguments passed to the program, enviroment variables, ...
5. The command `x/2i 0x00005555555173` will output the instruction (plus the one before) of the return pointer that we see was pushed onto the end of each stack frame. So as you can see the return pointer is pushed onto the stack so that after the function returns, we can continue execution from where we left off.


### Sum it up

Since I implemented fibonacci recursivly instead of iterativly, each call to fibonacci() is calling fibonacci() again, so each function is creating a new stack frame, and since none of the functions return (because they are still calling themselves), none of the stack frames are deleted from the stack. So new frames are created until the stack is full.

We can see that the first call to fibonacci(), from main, never returns by adding a printf() to our main function after the call to fibonacci() and before return (Fibonacci_no_return.c)
```c
  
#include <stdio.h>

int fibonacci(int n) {
   if(n == 0){
      return 0;
   } else if(n == 1) {
      return 1;
   } else {
      return (fibonacci(n-1) + fibonacci(n-2));
   }
}

int main() {
   int n;

   /*Getting user input*/
   printf("Enter a number to find to find Nth fibonacci term: ");
   scanf("%d", &n);
	
   /*Calculate fibonacci term of number given by user, then print it*/
   printf("Fibonacci of %d: " , n);
   printf("%d \n",fibonacci(n));

   /*Print message if the recursive fibonacci has returned to main */
   printf("I have returned to main!\n");
   return 0;
}
```

Then compile and run it causing a seg fault:

![no return](https://github.com/DylanLaw15/Computer_Architecture_Project1/blob/master/Pictures/no_return.png)

As you can see, the program Seg faults before `printf("I have returned to main!\n");` has a chance to be called, becuase fibonacci() never returns to main.

##The Problem With Floating Point Numbers(part 2)

So now for the second part of the project, we're gonna show how computers have trouble with declaring floating point numbers.  When you're building your programs, some things can really slip your mind when working with all of these algorithms and numbers, and one of these things that you must keep in mind are floats (especially when comparing two float numbers).  

According to [bitbashing](https://bitbashing.io/comparing-floats.html#:~:text=Since%20the%20result%20of%20every,produce%20a%20different%20result%20than),
> Since the result of every floating-point operation must be rounded to the nearest possible value, math doesn’t behave like it does with real numbers. Depending on your        > hardware, compiler, and compiler flags, 0.1 \times 100.1×10 may produce a different result than \sum_{n=1}^{10} 0.1∑
> ​n=1
> ​10
> ​​ 0.1.1 Whenever we compare calculated values to each other, we should provide some leeway to account for this. Comparing their exact values with == won’t cut it.

Now let's demonstrate how computers have trouble with this concept.
This Java program has 100 different cases where equivalency is checked using floating point numbers.

'''java

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
'''

Step by step, what this program does is

1. Call Random java class to create random floating point numbers
2. Run the check() function, which will check the equivalency of two math equations: "a+(b+c)" and "(a+b)+c"
3. Rense and repeat 100 times, using different floating point numbers after each check
4. Finally, it will output the percentage of equivalency checks that failed.

One thing to note is that the higher number of checks we do, the percentage will usually creep toward about a little over 20% failure for the equivalency check.
The reason we get this equivalency check is because when we use these expressions to add and change up the order of operations, the computer tries to round these floating point values, and sometimes the number is not always the same after rounding and when you start putting that float into this arithmetic ((a+b)+c or a+(b+c)).

Here we can see that the float values have been declared a, b, c with random float values.  Note the number of digits that represent these values.  This is what can cause errors when doing arithmetic/rounding.

Now lets perform the arithmetic.

As you can see, these numbers, although equal in real life, are not equal at all to a computer!  The first equation has an extra digit!  And this case happens about 1/5 times when checking equivalency.



