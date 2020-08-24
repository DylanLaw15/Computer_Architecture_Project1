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