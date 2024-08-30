import argparse
import os

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("endpoint", default=None, type=str)
    args = parser.parse_args()
    print(args)

if __name__ == "__main__":
    main()
