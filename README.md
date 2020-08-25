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
4. Calling fibonacci on n-1 and n-2 (this is the recursion)
5. Function Epilogue


Now lets debug the Fibonacci binary and cause a seg fault (Finding 10000th term in fibonacci sequence). 

![vmmap](https://github.com/DylanLaw15/Computer_Architecture_Project1/blob/master/Pictures/vmmap.png)

![debug_seg_fault](https://github.com/DylanLaw15/Computer_Architecture_Project1/blob/master/Pictures/debug_seg_fault.png)

Lets break down what happend:

1. vmmap command shows the virtual memory segments
   - Quick note: The stack is the only memory segment that grows upward (from higher addresses to lower), this will be important in understanding why the program seg faulted
2. This is the last instruction executed before the seg fault, 0x18 was subtracted from the Stack Pointer (points to the top of the stack)
3. Now the stack pointer is 0x7ffffffdeff0. Lets take the starting address of the stack from picture 1 and subtract the rsp: 0x00007ffffffdf000 - 0x7ffffffdeff0 = 0x10 this means that the stack pointer is now pointing 2 bytes higher than the highest address of the stacks virtual memory segment (BAD!!!).
4. Now gdb-peda cannot display the stack bcause the top of the stack (RSP) is pointing out of range.
5. RBP is the base pointer, which stores the stack pointer from the previous stack frame, as seen in the 2nd instruction of the function prologue of fibonacci i.e.        `mov    rbp,rsp`. 
6. "RDI" is the register that the first argument to a function is passed to in the x86_64 calling convention. It contains 0x1cc0 (7360 in decimal), so we know the process segfaulted when calling fibonacci(7360). Which means that 2640 (10000 - 7360) calls to fibonacci have active stack frames taking up space on the stack. We can verify this with backtrace or bt for short in gdb. The first 2634 entrys have been cropped out.

![backtrace](https://github.com/DylanLaw15/Computer_Architecture_Project1/blob/master/Pictures/backtrace.png)
