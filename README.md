# Computer Architecture Project1
## Stack Overflow in Recursive Functions (Fibonacci)
### So what is a Stack overflow?
[Techtarget](https://whatis.techtarget.com/definition/stack-overflow) defines a stack overflow as:
> A stack overflow is an undesirable condition in which a particular computer program tries to use more memory space than the call stack has available.

We can observe this through the C implementation of a Fibonacci calculator.

***All programming and debugging is done on a linux system, so the binaries will be compiled for a linux system (ELF)***

Here is the code:
```
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

As you can see we get a segmentation fault, meaning our process is trying to access memory it does not have permission to.

### Now for the fun part, why and how did this happen?

We will be using gdb with the gdb-peda plugin (just becuase I am used to using it) to debug this program!

Lets load the binary into gdb and disassemble the fibonacci function:

![fibonacci_disass](https://github.com/DylanLaw15/Computer_Architecture_Project1/blob/master/Pictures/fibonacci_disassembly.png)

Lets break down what is happening in the fibonacci function:

1. Function Prologue
2. Passing in the argument to fibonacci function
3. Logic to return based on if n is 0 or 1
4. Calling fibonacci on n-1 and n-2
5. Function Epilogue

