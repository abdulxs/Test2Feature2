import unittest
import ctypes

# Load the shared library into ctypes
lib = ctypes.CDLL('./libsimple_program.so')

class TestArithmetic(unittest.TestCase):
    
    def test_add(self):
        result = lib.add(5, 3)
        self.assertEqual(result, 8)
    
    def test_subtract(self):
        result = lib.subtract(5, 3)
        self.assertEqual(result, 2)

if __name__ == '__main__':
    unittest.main()
