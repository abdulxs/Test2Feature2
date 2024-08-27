import unittest
import ctypes

# Load the shared library into ctypes
lib = ctypes.CDLL('./libsimple_program.so')

class TestStringManipulation(unittest.TestCase):
    
    def test_string_length(self):
        result = lib.string_length(b"Hello World!")
        self.assertEqual(result, 12)
    
    def test_count_vowels(self):
        result = lib.count_vowels(b"Hello World!")
        self.assertEqual(result, 3)

if __name__ == '__main__':
    unittest.main()
