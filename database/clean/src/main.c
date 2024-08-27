#include <stdio.h>
#include "arithmetic.h"
#include "string_manipulation.h"

int main() {
    int a = 10, b = 3;
    printf("Addition: %d + %d = %d\n", a, b, add(a, b));
    printf("Subtraction: %d - %d = %d\n", a, b, subtract(a, b));

    const char *str = "Hello World!";
    printf("String Length: %s = %d\n", str, string_length(str));
    printf("Vowel Count: %s = %d\n", str, count_vowels(str));

    return 0;
}
