CC=gcc
CFLAGS=-Wall -I./include
SRCS=src/main.c src/arithmetic.c src/string_manipulation.c
OBJS=$(SRCS:.c=.o)
TARGET=main
SHARED_LIB=libsimple_program.so

all: $(TARGET) $(SHARED_LIB)

$(TARGET): $(OBJS)
	$(CC) -o $@ $^

$(SHARED_LIB): $(OBJS)
	$(CC) -shared -o $@ $(OBJS)

%.o: %.c
	$(CC) $(CFLAGS) -fPIC -c $< -o $@

clean:
	rm -f $(OBJS) $(TARGET) $(SHARED_LIB)

run: all
	./$(TARGET)

test: $(SHARED_LIB)
	python3 -m unittest discover -s test

.PHONY: all clean run test
