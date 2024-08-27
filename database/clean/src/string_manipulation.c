#include "string_manipulation.h"

int string_length(const char *str) {
    int length = 0;
    while (str[length] != '\0') {
        length++;
    }
    return length;
}

int count_vowels(const char *str) {
    int count = 0;
    for (int i = 0; str[i] != '\0'; i++) {
        if (str[i] == 'a' || str[i] == 'e' || str[i] == 'i' ||
            str[i] == 'o' || str[i] == 'u' || str[i] == 'A' ||
            str[i] == 'E' || str[i] == 'I' || str[i] == 'O' ||
            str[i] == 'U') {
            count++;
        }
    }
    return count;
}
